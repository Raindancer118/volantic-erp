package de.volantic.erp.core.numberrange;

/**
 * Public API for GoBD-compliant, gap-free document number ranges (DB architecture §5.1). Other modules
 * define a range once (e.g. invoices, orders) and then draw consecutive numbers. Each {@link #next(String)}
 * call must run inside the caller's transaction so the allocated number and the document it belongs to
 * commit (or roll back) together — guaranteeing no gaps.
 */
public interface NumberRanges {

    /** Defines a range if it does not exist yet (idempotent). {@code startValue} is the first number drawn. */
    void defineRange(String key, String prefix, int padding, long startValue);

    /** Allocates and returns the next formatted number for the range. Throws if the range is undefined. */
    String next(String key);
}
