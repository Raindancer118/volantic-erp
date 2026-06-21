package de.volantic.erp.core.revision;

import java.util.UUID;

/**
 * SPI a business module provides so its aggregates can take part in the <strong>Rollback Engine</strong>
 * (forward-only compensation of a whole edit session, ADR-0006). The {@code changeset} module collects
 * one handler per resource type as a Spring bean and never knows module-specific concepts — it only
 * deals with opaque serialized state and {@link ChangeOperation}s.
 *
 * <p>Compensation is always forward-only: it never deletes audit history, it applies the inverse as a
 * new, audited operation through the module's own domain service.
 */
public interface ReversibleResourceHandler {

    /** The module-qualified resource type this handler serves, e.g. {@code "crm.customer"}. */
    String resourceType();

    /**
     * Serializes the current state of the resource, or {@code null} if it does not currently exist.
     * Captured before a {@code LIVE} change so the change can later be compensated.
     */
    String capture(UUID id);

    /**
     * Compensates one recorded operation: for {@code UPDATE}/{@code DELETE} it restores
     * {@code beforeState}, for {@code CREATE} it removes/deactivates the resource (in which case
     * {@code beforeState} is {@code null}). Runs through the domain service — never a direct DB write.
     */
    void compensate(ChangeOperation operation, UUID id, String beforeState);
}
