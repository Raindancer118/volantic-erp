package de.volantic.erp.changeset.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A mass-delete request: delete the selected resources of {@code resourceType} (ADR-0006 §2). The
 * selection is given either as explicit {@code ids} or as an equality {@code filter} (exactly one), like
 * {@link BulkChange}. Runs only in a LIVE session; the Rollback Engine takes each deletion back by
 * re-creating the resource from the snapshot captured before it was deleted (forward-only, GoBD-safe).
 *
 * @param resourceType module-qualified type, e.g. {@code "crm.contact"}
 * @param ids          the resources to delete (empty when a filter is used)
 * @param filter       equality selection filter (empty when explicit ids are used)
 */
public record BulkDelete(String resourceType, List<UUID> ids, Map<String, String> filter) {

    public BulkDelete {
        if (resourceType == null || resourceType.isBlank()) {
            throw new IllegalArgumentException("resourceType must not be blank");
        }
        ids = ids == null ? List.of() : List.copyOf(ids);
        filter = filter == null ? Map.of() : Map.copyOf(filter);
        if (!ids.isEmpty() && !filter.isEmpty()) {
            throw new IllegalArgumentException("provide either ids or a filter, not both");
        }
    }

    public static BulkDelete byIds(String resourceType, List<UUID> ids) {
        return new BulkDelete(resourceType, ids, Map.of());
    }

    public static BulkDelete byFilter(String resourceType, Map<String, String> filter) {
        return new BulkDelete(resourceType, List.of(), filter);
    }

    public boolean isFiltered() {
        return !filter.isEmpty();
    }
}
