package de.volantic.erp.changeset.api;

import de.volantic.erp.changeset.application.BulkPreview;

import java.util.List;
import java.util.UUID;

/**
 * Response body for a mass-edit dry-run (REST v1). Mirrors {@link BulkPreview} so the JSON contract stays
 * in the api layer and is decoupled from the application type. {@code hasProblems} lets a client gate the
 * apply behind a confirmation when any row is not applicable.
 */
public record BulkPreviewResponse(String resourceType, boolean hasProblems, List<Row> rows) {

    public static BulkPreviewResponse from(BulkPreview preview) {
        List<Row> rows = preview.rows().stream()
                .map(row -> new Row(row.id(), row.applicable(), row.beforeState(), row.problem()))
                .toList();
        return new BulkPreviewResponse(preview.resourceType(), preview.hasProblems(), rows);
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
