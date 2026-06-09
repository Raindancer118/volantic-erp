package de.volantic.erp.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.UUID;

/** Embeddable BOM line stored in the {@code catalog.bom_line} collection table (owned by {@link BomEntity}). */
@Embeddable
class BomLineRow {

    @Column(name = "component_id", nullable = false)
    private UUID componentId;

    @Column(name = "qty_amount", nullable = false)
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
}
