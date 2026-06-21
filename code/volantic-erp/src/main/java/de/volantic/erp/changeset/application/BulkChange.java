package de.volantic.erp.changeset.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A mass-edit request: apply the same {@code fieldChanges} to every selected resource of one
 * {@code resourceType}. The field keys must be a subset of the resource's editable fields.
 *
 * <p>The selection is given <em>either</em> as an explicit {@code ids} list <em>or</em> as an equality
 * {@code filter} (field → value, ANDed) that the resource's handler resolves to ids (ADR-0006 §5,
 * "Selektion … oder Filter"). Exactly one of the two must be provided.
 *
 * @param resourceType module-qualified type, e.g. {@code "crm.customer"}
 * @param ids          the resources to change (empty when a filter is used)
 * @param filter       equality selection filter (empty when explicit ids are used)
 * @param fieldChanges field name → new value
 */
public record BulkChange(String resourceType, List<UUID> ids, Map<String, String> filter,
                         Map<String, String> fieldChanges) {

    public BulkChange {
        if (resourceType == null || resourceType.isBlank()) {
            throw new IllegalArgumentException("resourceType must not be blank");
        }
        ids = ids == null ? List.of() : List.copyOf(ids);
        filter = filter == null ? Map.of() : Map.copyOf(filter);
        fieldChanges = fieldChanges == null ? Map.of() : Map.copyOf(fieldChanges);
        if (!ids.isEmpty() && !filter.isEmpty()) {
            throw new IllegalArgumentException("provide either ids or a filter, not both");
        }
    }

    /** Convenience constructor for an explicit-id selection (no filter). */
    public BulkChange(String resourceType, List<UUID> ids, Map<String, String> fieldChanges) {
        this(resourceType, ids, Map.of(), fieldChanges);
    }

    /** A mass edit selecting resources by an equality filter instead of explicit ids. */
    public static BulkChange byFilter(String resourceType, Map<String, String> filter,
                                      Map<String, String> fieldChanges) {
        return new BulkChange(resourceType, List.of(), filter, fieldChanges);
    }

    /** True if this request selects its targets by filter rather than by an explicit id list. */
    public boolean isFiltered() {
        return !filter.isEmpty();
    }
}
