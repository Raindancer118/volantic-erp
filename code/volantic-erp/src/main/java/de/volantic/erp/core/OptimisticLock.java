package de.volantic.erp.core;

import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Small shared guard for optimistic concurrency control (ETag / If-Match). An update use case passes the
 * version the caller expected to be current (from the resource's ETag) together with the version actually
 * loaded; if they differ, the resource was modified concurrently and the update is rejected rather than
 * silently overwriting the other change (lost update). Kept in the core (OPEN) module so every module's
 * application layer applies the exact same rule.
 */
public final class OptimisticLock {

    private OptimisticLock() {
    }

    /**
     * Fails with {@link OptimisticLockingFailureException} (mapped to 412 by the api layer) if the loaded
     * {@code actualVersion} does not match the caller's {@code expectedVersion}.
     */
    public static void check(Long actualVersion, long expectedVersion, Object resource) {
        if (actualVersion == null || actualVersion != expectedVersion) {
            throw new OptimisticLockingFailureException("stale version for " + resource
                    + ": expected " + expectedVersion + " but current is " + actualVersion);
        }
    }
}
