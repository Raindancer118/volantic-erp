package de.volantic.erp.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Embeddable BOM line stored in the {@code catalog.bom_line} collection table (owned by {@link BomEntity}). */
@Embeddable
class BomLineRow {

    @Column(name = "component_id", nullable = false)
    private UUID componentId;

    @Column(name = "qty_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyAmount;

    @Column(name = "qty_unit", nullable = false, length = 20)
    private String qtyUnit;

    protected BomLineRow() {
    }

    BomLineRow(UUID componentId, BigDecimal qtyAmount, String qtyUnit) {
        this.componentId = componentId;
        this.qtyAmount = qtyAmount;
        this.qtyUnit = qtyUnit;
    }

    UUID componentId() {
        return componentId;
    }

    BigDecimal qtyAmount() {
        return qtyAmount;
    }

    String qtyUnit() {
        return qtyUnit;
    }

    // equals/hashCode are required on an @ElementCollection embeddable: without them Hibernate cannot
    // tell which rows changed and instead deletes and re-inserts the whole collection on every update.
    // qtyAmount is compared by value (scale-insensitive) so 2 and 2.0 are the same line.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof BomLineRow that
                && Objects.equals(componentId, that.componentId)
                && Objects.equals(qtyUnit, that.qtyUnit)
                && (qtyAmount == null ? that.qtyAmount == null
                        : that.qtyAmount != null && qtyAmount.compareTo(that.qtyAmount) == 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentId, qtyAmount == null ? null : qtyAmount.stripTrailingZeros(), qtyUnit);
    }
}
