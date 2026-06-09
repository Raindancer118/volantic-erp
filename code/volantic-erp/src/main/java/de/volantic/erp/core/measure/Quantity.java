package de.volantic.erp.core.measure;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * An amount of a {@link UnitOfMeasure} — the quantity side of the core money/quantity model. Immutable
 * value object; arithmetic between quantities requires matching units. Used by catalog (BOM lines) and
 * later by inventory/sales.
 */
public record Quantity(BigDecimal amount, UnitOfMeasure unit) {

    public Quantity {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
    }

    public static Quantity of(BigDecimal amount, UnitOfMeasure unit) {
        return new Quantity(amount, unit);
    }

    public static Quantity of(String amount, UnitOfMeasure unit) {
        return new Quantity(new BigDecimal(amount), unit);
    }

    public Quantity plus(Quantity other) {
        requireSameUnit(other);
        return new Quantity(amount.add(other.amount), unit);
    }

    public Quantity minus(Quantity other) {
        requireSameUnit(other);
        return new Quantity(amount.subtract(other.amount), unit);
    }

    /** Multiplies by a dimensionless factor (e.g. a BOM multiplier). */
    public Quantity multiply(BigDecimal factor) {
        return new Quantity(amount.multiply(factor), unit);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    private void requireSameUnit(Quantity other) {
        if (!unit.equals(other.unit)) {
            throw new IllegalArgumentException("unit mismatch: " + unit.code() + " vs " + other.unit.code());
        }
    }
}
