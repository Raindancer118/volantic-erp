package de.volantic.erp.catalog.infrastructure.revision;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.catalog.application.CatalogExceptions;
import de.volantic.erp.catalog.application.ProductService;
import de.volantic.erp.catalog.domain.model.Product;
import de.volantic.erp.catalog.domain.model.ProductId;
import de.volantic.erp.core.measure.Money;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ProductLifecycleHandler}: the {@link Money} list price round-trips through the
 * snapshot (amount + ISO currency) and recreate restores the original sku/price. Service mocked.
 */
class ProductLifecycleHandlerTest {

    private final ProductService products = mock(ProductService.class);
    private final ProductLifecycleHandler handler = new ProductLifecycleHandler(products, new ObjectMapper());

    @Test
    void resourceType() {
        assertThat(handler.resourceType()).isEqualTo("catalog.product");
    }

    @Test
    void snapshotThenRecreateRestoresSkuAndExactPrice() {
        UUID id = UUID.randomUUID();
        when(products.getProduct(new ProductId(id)))
                .thenReturn(Product.reconstitute(new ProductId(id), "SKU-1", "Widget", Money.of("19.99", "EUR")));

        String snapshot = handler.snapshot(id);
        assertThat(snapshot).contains("\"sku\":\"SKU-1\"");

        handler.recreate(id, snapshot);

        ArgumentCaptor<Money> price = ArgumentCaptor.forClass(Money.class);
        verify(products).recreateProduct(eq(new ProductId(id)), eq("SKU-1"), eq("Widget"), price.capture());
        assertThat(price.getValue()).isEqualTo(Money.of("19.99", "EUR"));
    }

    @Test
    void createDelegatesWithSkuNamePrice() {
        UUID id = UUID.randomUUID();
        when(products.createProduct(eq("SKU-2"), eq("Gadget"), eq(Money.of("5.00", "EUR"))))
                .thenReturn(Product.reconstitute(new ProductId(id), "SKU-2", "Gadget", Money.of("5.00", "EUR")));

        UUID created = handler.create(Map.of(
                "sku", "SKU-2", "name", "Gadget", "listPriceAmount", "5.00", "listPriceCurrency", "EUR"));

        assertThat(created).isEqualTo(id);
    }

    @Test
    void snapshotReturnsNullWhenMissing() {
        UUID id = UUID.randomUUID();
        when(products.getProduct(new ProductId(id))).thenThrow(new CatalogExceptions.ProductNotFound(new ProductId(id)));

        assertThat(handler.snapshot(id)).isNull();
    }

    @Test
    void deleteDelegates() {
        UUID id = UUID.randomUUID();
        handler.delete(id);
        verify(products).deleteProduct(new ProductId(id));
    }
}
