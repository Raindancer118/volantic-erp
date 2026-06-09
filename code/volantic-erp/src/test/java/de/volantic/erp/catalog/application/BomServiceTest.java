package de.volantic.erp.catalog.application;

import de.volantic.erp.catalog.application.port.out.BomRepository;
import de.volantic.erp.catalog.application.port.out.ProductRepository;
import de.volantic.erp.catalog.domain.model.Bom;
import de.volantic.erp.catalog.domain.model.BomLine;
import de.volantic.erp.catalog.domain.model.ProductId;
import de.volantic.erp.core.UuidV7;
import de.volantic.erp.core.measure.Quantity;
import de.volantic.erp.core.measure.UnitOfMeasure;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Use-case logic of {@link BomService} over mocked repositories (no Spring/DB). */
class BomServiceTest {

    private final BomRepository boms = mock(BomRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final BomService service = new BomService(boms, products);

    private static ProductId id() {
        return new ProductId(UuidV7.randomUuid());
    }

    private static BomLine line(ProductId componentId) {
        return new BomLine(componentId, Quantity.of("2", UnitOfMeasure.PIECE));
    }

    @Test
    void createValidatesProductComponentsAndVersion() {
        ProductId productId = id();
        ProductId componentId = id();
        when(products.existsById(productId)).thenReturn(true);
        when(products.existsById(componentId)).thenReturn(true);
        when(boms.existsByProductIdAndVersion(productId, 1)).thenReturn(false);
        when(boms.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Bom created = service.createBom(productId, 1, null, null, List.of(line(componentId)));

        assertThat(created.productId()).isEqualTo(productId);
        verify(boms).save(any(Bom.class));
    }

    @Test
    void createRejectsWhenProductMissing() {
        ProductId productId = id();
        when(products.existsById(productId)).thenReturn(false);

        assertThatThrownBy(() -> service.createBom(productId, 1, null, null, List.of(line(id()))))
                .isInstanceOf(CatalogExceptions.ProductNotFound.class);
        verify(boms, never()).save(any());
    }

    @Test
    void createRejectsWhenComponentMissing() {
        ProductId productId = id();
        ProductId componentId = id();
        when(products.existsById(productId)).thenReturn(true);
        when(products.existsById(componentId)).thenReturn(false);

        assertThatThrownBy(() -> service.createBom(productId, 1, null, null, List.of(line(componentId))))
                .isInstanceOf(CatalogExceptions.ProductNotFound.class);
        verify(boms, never()).save(any());
    }

    @Test
    void createRejectsDuplicateVersion() {
        ProductId productId = id();
        ProductId componentId = id();
        when(products.existsById(productId)).thenReturn(true);
        when(products.existsById(componentId)).thenReturn(true);
        when(boms.existsByProductIdAndVersion(productId, 1)).thenReturn(true);

        assertThatThrownBy(() -> service.createBom(productId, 1, null, null, List.of(line(componentId))))
                .isInstanceOf(CatalogExceptions.BomVersionAlreadyExists.class);
        verify(boms, never()).save(any());
    }
}
