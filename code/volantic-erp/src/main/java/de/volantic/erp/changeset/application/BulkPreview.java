package de.volantic.erp.changeset.application;

import java.util.List;
import java.util.UUID;

/**
 * Dry-run result of a {@link BulkChange}: one row per selected resource, with its current state and any
 * problem that would stop the change being applied. Computing a preview never mutates anything.
 *
 * @param resourceType the type previewed
 * @param rows         one row per requested id
 */
public record BulkPreview(String resourceType, List<Row> rows) {

    public BulkPreview {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    /** True if at least one row could not be applied (e.g. missing resource, uneditable field). */
    public boolean hasProblems() {
        return rows.stream().anyMatch(row -> !row.applicable());
    }

    /**
     * @param id          the previewed resource
     * @param applicable  whether the change could be applied to it
     * @param beforeState serialized current state, or {@code null} if the resource does not exist
     * @param problem     human-readable reason it is not applicable, or {@code null}
     */
    public record Row(UUID id, boolean applicable, String beforeState, String problem) {
    }
}
