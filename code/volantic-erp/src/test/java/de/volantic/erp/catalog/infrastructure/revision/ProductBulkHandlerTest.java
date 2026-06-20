package de.volantic.erp.catalog.infrastructure.revision;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.catalog.application.CatalogExceptions;
import de.volantic.erp.catalog.application.ProductService;
import de.volantic.erp.catalog.domain.model.Product;
import de.volantic.erp.catalog.domain.model.ProductId;
import de.volantic.erp.core.measure.Money;
import de.volantic.erp.core.revision.ChangeOperation;
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
 * Unit test that the {@link ProductBulkHandler} round-trips the {@link Money} list price through the
 * generic field model (amount + ISO currency) without precision loss, and applies a partial change via
 * {@link ProductService}.
 */
class ProductBulkHandlerTest {

    private final ProductService products = mock(ProductService.class);
    private final ProductBulkHandler handler = new ProductBulkHandler(products, new ObjectMapper());

    @Test
    void resourceTypeAndEditableFields() {
        assertThat(handler.resourceType()).isEqualTo("catalog.product");
        assertThat(handler.editableFields())
                .containsExactlyInAnyOrder("name", "listPriceAmount", "listPriceCurrency");
    }

    @Test
    void compensateRestoresNameAndExactPrice() {
        UUID id = UUID.randomUUID();
        when(products.getProduct(new ProductId(id)))
                .thenReturn(Product.reconstitute(new ProductId(id), "SKU-1", "Widget", Money.of("19.99", "EUR")));

        String before = handler.capture(id);
        handler.compensate(ChangeOperation.UPDATE, id, before);

        ArgumentCaptor<Money> price = ArgumentCaptor.forClass(Money.class);
        verify(products).updateProduct(eq(new ProductId(id)), eq("Widget"), price.capture());
        assertThat(price.getValue()).isEqualTo(Money.of("19.99", "EUR"));
    }

    @Test
    void applyChangeRepricesKeepingName() {
        UUID id = UUID.randomUUID();
        when(products.getProduct(new ProductId(id)))
                .thenReturn(Product.reconstitute(new ProductId(id), "SKU-1", "Widget", Money.of("19.99", "EUR")));

        handler.applyChange(id, Map.of("listPriceAmount", "24.50"));

        verify(products).updateProduct(eq(new ProductId(id)), eq("Widget"), eq(Money.of("24.50", "EUR")));
    }

    @Test
    void captureReturnsNullWhenProductMissing() {
        UUID id = UUID.randomUUID();
        when(products.getProduct(new ProductId(id))).thenThrow(new CatalogExceptions.ProductNotFound(new ProductId(id)));

        assertThat(handler.capture(id)).isNull();
    }
}
