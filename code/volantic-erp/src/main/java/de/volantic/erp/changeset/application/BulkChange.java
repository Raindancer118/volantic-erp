package de.volantic.erp.changeset.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A mass-edit request: apply the same {@code fieldChanges} to every selected resource of one
 * {@code resourceType}. The field keys must be a subset of the resource's editable fields.
 *
 * @param resourceType module-qualified type, e.g. {@code "crm.customer"}
 * @param ids          the resources to change
 * @param fieldChanges field name → new value
 */
public record BulkChange(String resourceType, List<UUID> ids, Map<String, String> fieldChanges) {

    public BulkChange {
        if (resourceType == null || resourceType.isBlank()) {
            throw new IllegalArgumentException("resourceType must not be blank");
        }
        ids = ids == null ? List.of() : List.copyOf(ids);
        fieldChanges = fieldChanges == null ? Map.of() : Map.copyOf(fieldChanges);
    }
}
