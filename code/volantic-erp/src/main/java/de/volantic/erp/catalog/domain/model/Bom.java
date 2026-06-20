package de.volantic.erp.catalog.domain.model;

import de.volantic.erp.core.UuidV7;

import java.time.LocalDate;
import java.util.List;

/**
 * A versioned bill of materials for a product — the BOM aggregate root. BOMs are immutable once
 * created; a change means a new {@code version}. Lines reference component products (multi-level).
 * Pure domain.
 */
public final class Bom {

    private final BomId id;
    private final ProductId productId;
    private final int version;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final List<BomLine> lines;

    private Bom(BomId id, ProductId productId, int version, LocalDate validFrom, LocalDate validTo, List<BomLine> lines) {
        this.id = id;
        this.productId = requireNonNull(productId, "productId");
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("a BOM must have at least one line");
        }
        // A component must appear at most once: duplicates would double-count in MRP explosions and can
        // hide cycles from the circular-reference check. Merge quantities upstream instead.
        if (lines.stream().map(BomLine::componentId).distinct().count() != lines.size()) {
            throw new IllegalArgumentException("a BOM must not list the same component more than once");
        }
        if (validFrom != null && validTo != null && validTo.isBefore(validFrom)) {
            throw new IllegalArgumentException("validTo must not be before validFrom");
        }
        this.version = version;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.lines = List.copyOf(lines);
    }

    public static Bom create(ProductId productId, int version, LocalDate validFrom, LocalDate validTo, List<BomLine> lines) {
        return new Bom(new BomId(UuidV7.randomUuid()), productId, version, validFrom, validTo, lines);
    }

    public static Bom reconstitute(BomId id, ProductId productId, int version,
                                   LocalDate validFrom, LocalDate validTo, List<BomLine> lines) {
        return new Bom(id, productId, version, validFrom, validTo, lines);
    }

    public BomId id() {
        return id;
    }

    public ProductId productId() {
        return productId;
    }

    public int version() {
        return version;
    }

    public LocalDate validFrom() {
        return validFrom;
    }

    public LocalDate validTo() {
        return validTo;
    }

    public List<BomLine> lines() {
        return lines;
    }

    /** Identity equality: two BOMs are the same iff they share an id (a BOM is immutable once created). */
    @Override
    public boolean equals(Object other) {
        return other instanceof Bom that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
