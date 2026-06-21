package de.volantic.erp.catalog.domain.model;

import de.volantic.erp.core.measure.Quantity;

/**
 * One position of a bill of materials: a {@code component} product in a given {@link Quantity}. Because
 * the component is itself a {@link ProductId} that may have its own {@link Bom}, structures are
 * multi-level.
 */
public record BomLine(ProductId componentId, Quantity quantity) {

    public BomLine {
        if (componentId == null) {
            throw new IllegalArgumentException("componentId must not be null");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("quantity must not be null");
        }
        if (quantity.isNegative() || quantity.isZero()) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
