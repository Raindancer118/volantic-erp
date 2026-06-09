package de.volantic.erp.core.numberrange.domain.model;

/**
 * A gap-free document number range (GoBD): a named, monotonically increasing counter rendered as
 * {@code prefix + zero-padded(value)} (e.g. {@code RE-000001}). Pure domain; the gap-free guarantee
 * comes from the infrastructure holding a pessimistic lock on the row while {@link #allocate()} runs
 * inside the caller's transaction — a rollback rolls the counter back too, so no number is skipped.
 */
public final class NumberRange {

    private final String key;
    private final String prefix;
    private final int padding;
    private long nextValue;

    public NumberRange(String key, String prefix, int padding, long nextValue) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("range key must not be blank");
        }
        if (padding < 0) {
            throw new IllegalArgumentException("padding must not be negative");
        }
        if (nextValue < 0) {
            throw new IllegalArgumentException("nextValue must not be negative");
        }
        this.key = key;
        this.prefix = prefix == null ? "" : prefix;
        this.padding = padding;
        this.nextValue = nextValue;
    }

    /** Returns the next formatted number and advances the counter by one. */
    public String allocate() {
        String formatted = prefix + pad(nextValue, padding);
        nextValue++;
        return formatted;
    }

    public String key() {
        return key;
    }

    public String prefix() {
        return prefix;
    }

    public int padding() {
        return padding;
    }

    public long nextValue() {
        return nextValue;
    }

    private static String pad(long value, int width) {
        String digits = Long.toString(value);
        return digits.length() >= width ? digits : "0".repeat(width - digits.length()) + digits;
    }
}
