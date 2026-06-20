package de.volantic.erp.sales.domain.model;

import de.volantic.erp.core.measure.Money;

import java.math.BigDecimal;

/**
 * One line of an {@link Invoice}: a description, a quantity and a unit price. The line total is derived
 * ({@code unitPrice × quantity}). Pure value object.
 *
 * @param description free-text line description
 * @param quantity    number of units (must be positive)
 * @param unitPrice   price per unit
 */
public record InvoiceLine(String description, BigDecimal quantity, Money unitPrice) {

    public InvoiceLine {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("line description must not be blank");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("line quantity must be positive");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("line unit price must not be null");
        }
        if (unitPrice.isNegative()) {
            throw new IllegalArgumentException("line unit price must not be negative");
        }
        description = description.strip();
    }

    /** The line total: unit price multiplied by the quantity. */
    public Money lineTotal() {
        return unitPrice.multiply(quantity);
    }
}
