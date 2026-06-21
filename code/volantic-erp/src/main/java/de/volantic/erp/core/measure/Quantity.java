package de.volantic.erp.core.measure;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * An amount of a {@link UnitOfMeasure} — the quantity side of the core money/quantity model. Immutable
 * value object; arithmetic between quantities requires matching units. Used by catalog (BOM lines) and
 * later by inventory/sales.
 */
public record Quantity(BigDecimal amount, UnitOfMeasure unit) {

    /**
     * Maximum number of fractional digits, mirroring the {@code NUMERIC(19,4)} storage of quantity
     * amounts (e.g. {@code catalog.bom_line.qty_amount}). A finer value would be <em>silently truncated</em>
     * by the database on persist, so it is rejected here at the domain boundary (fail-fast, no silent
     * data loss — GoBD).
     */
    public static final int MAX_SCALE = 4;

    public Quantity {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
        if (amount.scale() > MAX_SCALE) {
            throw new IllegalArgumentException("amount has more than " + MAX_SCALE
                    + " decimal places (would be truncated on persist): " + amount.toPlainString());
        }
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

    public boolean isZero() {
        return amount.signum() == 0;
    }

    private void requireSameUnit(Quantity other) {
        if (!unit.equals(other.unit)) {
            throw new IllegalArgumentException("unit mismatch: " + unit.code() + " vs " + other.unit.code());
        }
    }

    /**
     * Value equality that is scale-insensitive on the amount: {@code Quantity.of("2")} equals
     * {@code Quantity.of("2.0")}. The record's generated {@code equals} would delegate to
     * {@link BigDecimal#equals(Object)}, which compares value <em>and</em> scale and would report those
     * two as different — silently breaking comparisons, set membership and BOM logic. We compare the
     * amount via {@link BigDecimal#compareTo} instead.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Quantity that
                && unit.equals(that.unit)
                && amount.compareTo(that.amount) == 0;
    }

    @Override
    public int hashCode() {
        // Must agree with the scale-insensitive equals: strip the scale before hashing so equal values
        // (different scale) land in the same bucket.
        return Objects.hash(amount.stripTrailingZeros(), unit);
    }
}
