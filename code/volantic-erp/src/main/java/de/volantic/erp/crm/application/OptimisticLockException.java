package de.volantic.erp.crm.application;

/**
 * Raised when an update carries an expected version that no longer matches the current persisted state —
 * i.e. the resource was changed by someone else since the client last read it. Mapped to HTTP 412
 * (Precondition Failed) by the api layer, so the client must re-read and retry rather than silently
 * overwrite the other change (lost-update prevention).
 */
public class OptimisticLockException extends RuntimeException {

    public OptimisticLockException(String resource, long expectedVersion, Long actualVersion) {
        super("stale update of " + resource + ": expected version " + expectedVersion
                + " but current is " + actualVersion);
    }
}
