package de.volantic.erp.catalog.infrastructure.persistence;

import de.volantic.erp.catalog.application.port.out.BomRepository;
import de.volantic.erp.catalog.application.port.out.ProductRepository;
import de.volantic.erp.catalog.domain.model.Bom;
import de.volantic.erp.catalog.domain.model.BomLine;
import de.volantic.erp.catalog.domain.model.Product;
import de.volantic.erp.catalog.domain.model.ProductId;
import de.volantic.erp.core.measure.Money;
import de.volantic.erp.core.measure.Quantity;
import de.volantic.erp.core.measure.UnitOfMeasure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence integration test for the catalog adapters against a real PostgreSQL (Testcontainers):
 * product roundtrip plus a versioned BOM with its element-collection lines, validating the JPA mapping
 * against the Flyway schema. Skipped without Docker; runs in CI.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ProductRepositoryAdapter.class, BomRepositoryAdapter.class})
@Testcontainers(disabledWithoutDocker = true)
class CatalogRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ProductRepository products;

    @Autowired
    private BomRepository boms;

    @Test
    void savesLoadsAndUpdatesProduct() {
        Product product = Product.create("SKU-1001", "Widget", Money.of("19.99", "EUR"));
        products.save(product);

        assertThat(products.existsBySku("SKU-1001")).isTrue();
        assertThat(products.existsBySku("SKU-9999")).isFalse();

        Product loaded = products.findById(product.id()).orElseThrow();
        assertThat(loaded.name()).isEqualTo("Widget");
        assertThat(loaded.listPrice()).isEqualTo(Money.of("19.99", "EUR"));

        loaded.rename("Gadget");
        loaded.reprice(Money.of("24.50", "EUR"));
        products.save(loaded);

        Product reloaded = products.findById(product.id()).orElseThrow();
        assertThat(reloaded.name()).isEqualTo("Gadget");
        assertThat(reloaded.listPrice()).isEqualTo(Money.of("24.50", "EUR"));
        assertThat(reloaded.sku()).isEqualTo("SKU-1001");
    }

    @Test
    void savesAndLoadsVersionedBomWithLines() {
        Product parent = products.save(Product.create("SKU-P", "Parent", Money.of("100.00", "EUR")));
        Product componentA = products.save(Product.create("SKU-A", "Comp A", Money.of("1.00", "EUR")));
        Product componentB = products.save(Product.create("SKU-B", "Comp B", Money.of("2.00", "EUR")));

        List<BomLine> lines = List.of(
                new BomLine(componentA.id(), Quantity.of("3", UnitOfMeasure.PIECE)),
                new BomLine(componentB.id(), Quantity.of("1.5", UnitOfMeasure.KILOGRAM)));
        Bom bom = Bom.create(parent.id(), 1, LocalDate.of(2026, 1, 1), null, lines);
        boms.save(bom);

        assertThat(boms.existsByProductIdAndVersion(parent.id(), 1)).isTrue();
        assertThat(boms.existsByProductIdAndVersion(parent.id(), 2)).isFalse();

        Bom loaded = boms.findById(bom.id()).orElseThrow();
        assertThat(loaded.productId()).isEqualTo(parent.id());
        assertThat(loaded.version()).isEqualTo(1);
        assertThat(loaded.validFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(loaded.lines()).hasSize(2);
        assertThat(loaded.lines())
                .extracting(line -> line.quantity().unit().code())
                .containsExactlyInAnyOrder("PCS", "KG");
    }

    @Test
    void listsBomsOfProduct() {
        Product parent = products.save(Product.create("SKU-MULTI", "Multi", Money.of("10.00", "EUR")));
        Product component = products.save(Product.create("SKU-C", "Comp", Money.of("1.00", "EUR")));
        BomLine line = new BomLine(component.id(), Quantity.of("1", UnitOfMeasure.PIECE));

        boms.save(Bom.create(parent.id(), 1, null, null, List.of(line)));
        boms.save(Bom.create(parent.id(), 2, null, null, List.of(line)));

        List<Bom> found = boms.findByProductId(parent.id());
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Bom::version).containsExactly(1, 2);
    }

    @Test
    void findByProductIdReturnsEmptyForUnknownProduct() {
        ProductId unknown = Product.create("SKU-X", "X", Money.of("1.00", "EUR")).id();
        assertThat(boms.findByProductId(unknown)).isEmpty();
    }
}
