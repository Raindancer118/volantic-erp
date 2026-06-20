package de.volantic.erp.catalog.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Value equality of the {@link BomLineRow} embeddable. Hibernate needs this on an {@code @ElementCollection}
 * to diff the collection instead of deleting and re-inserting every row on each update.
 */
class BomLineRowTest {

    @Test
    void equalRowsAreEqualAndShareHashCodeRegardlessOfAmountScale() {
        UUID component = UUID.randomUUID();
        BomLineRow a = new BomLineRow(component, new BigDecimal("2"), "PIECE");
        BomLineRow b = new BomLineRow(component, new BigDecimal("2.00"), "PIECE");

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void differsByComponentAmountOrUnit() {
        UUID component = UUID.randomUUID();
        BomLineRow base = new BomLineRow(component, new BigDecimal("2"), "PIECE");

        assertThat(base).isNotEqualTo(new BomLineRow(UUID.randomUUID(), new BigDecimal("2"), "PIECE"));
        assertThat(base).isNotEqualTo(new BomLineRow(component, new BigDecimal("3"), "PIECE"));
        assertThat(base).isNotEqualTo(new BomLineRow(component, new BigDecimal("2"), "KILOGRAM"));
    }
}
