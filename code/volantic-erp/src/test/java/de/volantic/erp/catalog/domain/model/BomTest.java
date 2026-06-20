package de.volantic.erp.catalog.domain.model;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.core.measure.Quantity;
import de.volantic.erp.core.measure.UnitOfMeasure;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Invariants of the versioned {@link Bom} aggregate root (pure domain, no Spring). */
class BomTest {

    private static ProductId productId() {
        return new ProductId(UuidV7.randomUuid());
    }

    private static BomLine line() {
        return new BomLine(productId(), Quantity.of("2", UnitOfMeasure.PIECE));
    }

    @Test
    void createKeepsImmutableCopyOfLines() {
        Bom bom = Bom.create(productId(), 1, LocalDate.of(2026, 1, 1), null, List.of(line(), line()));

        assertThat(bom.id()).isNotNull();
        assertThat(bom.version()).isEqualTo(1);
        assertThat(bom.lines()).hasSize(2);
        assertThatThrownBy(() -> bom.lines().add(line()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsVersionBelowOne() {
        assertThatThrownBy(() -> Bom.create(productId(), 0, null, null, List.of(line())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyLines() {
        assertThatThrownBy(() -> Bom.create(productId(), 1, null, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsValidToBeforeValidFrom() {
        assertThatThrownBy(() -> Bom.create(
                productId(), 1, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), List.of(line())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsZeroQuantityLine() {
        // A zero-quantity component would divide-by-zero / no-op in MRP explosion downstream.
        assertThatThrownBy(() -> new BomLine(productId(), Quantity.of("0", UnitOfMeasure.PIECE)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTheSameComponentTwice() {
        ProductId component = productId();
        assertThatThrownBy(() -> Bom.create(productId(), 1, null, null, List.of(
                new BomLine(component, Quantity.of("1", UnitOfMeasure.PIECE)),
                new BomLine(component, Quantity.of("2", UnitOfMeasure.PIECE)))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
