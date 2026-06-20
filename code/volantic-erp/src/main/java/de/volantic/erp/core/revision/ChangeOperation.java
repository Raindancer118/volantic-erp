package de.volantic.erp.core.revision;

/**
 * The kind of mutation a reversible operation represents. Used both when recording what happened in a
 * {@code ChangeSet} and when compensating it (ADR-0006): compensation is always forward-only — it never
 * deletes audit history, it applies the inverse as a new, audited operation.
 */
public enum ChangeOperation {

    /** A resource was created — its compensation removes/deactivates it (there is no before-state). */
    CREATE,

    /** A resource's fields changed — its compensation restores the captured before-state. */
    UPDATE,

    /** A resource was deleted — its compensation re-creates it from the captured before-state. */
    DELETE,

    /**
     * A document (Beleg) was posted — its compensation is a forward-only storno (a cancellation document
     * in the same number range), never a delete or restore, so the GoBD number/audit chain stays intact.
     */
    POST
}
