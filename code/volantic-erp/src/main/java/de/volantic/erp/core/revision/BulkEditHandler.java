package de.volantic.erp.core.revision;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SPI a business module provides so its aggregates can be edited in bulk (ADR-0006). The
 * {@code changeset} module collects one handler per resource type as a Spring bean; it stays generic by
 * only ever passing field-name → new-value maps and never touching module-specific types.
 *
 * <p>Used both for direct mass changes and for applying buffered {@code Probemodus} operations on
 * "Übertragen". Applying a change always goes through the module's domain service — never a direct DB
 * write — so all validation, hooks and the audit trail run.
 */
public interface BulkEditHandler {

    /** The module-qualified resource type this handler serves, e.g. {@code "crm.customer"}. */
    String resourceType();

    /** The field names that may be changed in bulk for this resource. */
    Set<String> editableFields();

    /**
     * Applies the given field changes to one resource through the domain service. The keys are a subset
     * of {@link #editableFields()}; validation of the values is the handler's responsibility.
     */
    void applyChange(UUID id, Map<String, String> fieldChanges);
}
