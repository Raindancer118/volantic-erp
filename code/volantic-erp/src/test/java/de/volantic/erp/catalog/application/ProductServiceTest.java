package de.volantic.erp.catalog.application;

import de.volantic.erp.catalog.application.port.out.ProductRepository;
import de.volantic.erp.catalog.domain.model.Product;
import de.volantic.erp.catalog.domain.model.ProductId;
import de.volantic.erp.core.UuidV7;
import de.volantic.erp.core.measure.Money;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Use-case logic of {@link ProductService} over a mocked {@link ProductRepository} (no Spring/DB). */
class ProductServiceTest {

    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductService service = new ProductService(repository);

    @Test
    void createPersistsWhenSkuIsFree() {
        when(repository.existsBySku("SKU-1")).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Product created = service.createProduct("SKU-1", "Widget", Money.of("9.99", "EUR"));

        assertThat(created.sku()).isEqualTo("SKU-1");
        verify(repository).save(any(Product.class));
    }

    @Test
    void createRejectsDuplicateSku() {
        when(repository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> service.createProduct("SKU-1", "Widget", Money.of("9.99", "EUR")))
                .isInstanceOf(CatalogExceptions.SkuAlreadyExists.class);
        verify(repository, never()).save(any());
    }

    @Test
    void getThrowsWhenMissing() {
        ProductId id = new ProductId(UuidV7.randomUuid());
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProduct(id))
                .isInstanceOf(CatalogExceptions.ProductNotFound.class);
    }

    @Test
    void updateMutatesAndSaves() {
        Product existing = Product.create("SKU-1", "Widget", Money.of("9.99", "EUR"));
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = service.updateProduct(existing.id(), "Gadget", Money.of("12.00", "EUR"));

        assertThat(updated.name()).isEqualTo("Gadget");
        assertThat(updated.listPrice()).isEqualTo(Money.of("12.00", "EUR"));
        verify(repository).save(existing);
    }
}
