package de.volantic.erp.sales.domain.model;

/**
 * Lifecycle of a sales {@link Invoice} (GoBD, DB architecture §5.1). A {@code DRAFT} is freely editable;
 * once {@code POSTED} it is a legal document — immutable, gap-free numbered, and correctable only by a
 * {@code CANCELLED} via a separate storno document (forward-only, never deleted).
 */
public enum InvoiceStatus {

    /** Work in progress: editable, no document number yet. */
    DRAFT,

    /** Posted: a finalized legal document with a gap-free number; immutable. */
    POSTED,

    /** Cancelled by a storno document (the posted invoice itself is never altered or deleted). */
    CANCELLED
}
