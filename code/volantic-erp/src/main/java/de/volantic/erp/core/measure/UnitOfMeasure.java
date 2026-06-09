package de.volantic.erp.core.measure;

/**
 * Unit of measure for a {@link Quantity}, identified by a short code (e.g. {@code PCS}, {@code KG},
 * {@code H}). Kept as a free-form validated code rather than a fixed enum so installations can define
 * their own units; reference data (the {@code uom} table) is layered on top later.
 */
public record UnitOfMeasure(String code) {

    public static final UnitOfMeasure PIECE = new UnitOfMeasure("PCS");
    public static final UnitOfMeasure KILOGRAM = new UnitOfMeasure("KG");
    public static final UnitOfMeasure METRE = new UnitOfMeasure("M");
    public static final UnitOfMeasure HOUR = new UnitOfMeasure("H");

    public UnitOfMeasure {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("unit code must not be blank");
        }
        code = code.strip().toUpperCase();
    }
}
