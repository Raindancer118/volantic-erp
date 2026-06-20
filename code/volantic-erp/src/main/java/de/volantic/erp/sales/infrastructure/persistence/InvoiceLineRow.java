package de.volantic.erp.sales.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.Objects;

/** Embeddable invoice line stored in the {@code sales.invoice_line} collection table (owned by InvoiceEntity). */
@Embeddable
class InvoiceLineRow {

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPriceAmount;

    protected InvoiceLineRow() {
    }

    InvoiceLineRow(String description, BigDecimal quantity, BigDecimal unitPriceAmount) {
        this.description = description;
        this.quantity = quantity;
        this.unitPriceAmount = unitPriceAmount;
    }

    String description() {
        return description;
    }

    BigDecimal quantity() {
        return quantity;
    }

    BigDecimal unitPriceAmount() {
        return unitPriceAmount;
    }

    // equals/hashCode are required on an @ElementCollection embeddable so Hibernate diffs lines instead of
    // delete-all + re-insert on every change. Amounts compared by value (scale-insensitive).
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof InvoiceLineRow that
                && Objects.equals(description, that.description)
                && compare(quantity, that.quantity)
                && compare(unitPriceAmount, that.unitPriceAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description,
                quantity == null ? null : quantity.stripTrailingZeros(),
                unitPriceAmount == null ? null : unitPriceAmount.stripTrailingZeros());
    }

    private static boolean compare(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }
}
