package de.volantic.erp.core.measure;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure value-object tests for {@link Quantity} and {@link UnitOfMeasure}. */
class QuantityTest {

    @Test
    void addsSameUnit() {
        Quantity sum = Quantity.of("2", UnitOfMeasure.PIECE).plus(Quantity.of("3", UnitOfMeasure.PIECE));
        assertThat(sum.amount()).isEqualByComparingTo("5");
        assertThat(sum.unit()).isEqualTo(UnitOfMeasure.PIECE);
    }

    @Test
    void rejectsUnitMismatch() {
        assertThatThrownBy(() -> Quantity.of("1", UnitOfMeasure.PIECE).plus(Quantity.of("1", UnitOfMeasure.KILOGRAM)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multipliesByFactor() {
        assertThat(Quantity.of("2.5", UnitOfMeasure.KILOGRAM).multiply(new BigDecimal("4")).amount())
                .isEqualByComparingTo("10.0");
    }

    @Test
    void unitCodeIsNormalisedAndValidated() {
        assertThat(new UnitOfMeasure(" pcs ").code()).isEqualTo("PCS");
        assertThatThrownBy(() -> new UnitOfMeasure("  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
