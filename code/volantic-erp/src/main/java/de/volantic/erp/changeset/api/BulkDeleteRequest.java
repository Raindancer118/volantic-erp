package de.volantic.erp.changeset.api;

import de.volantic.erp.changeset.application.BulkDelete;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request body for a mass delete (REST v1): delete the selected resources of {@code resourceType}, chosen
 * by explicit {@code ids} or an equality {@code filter} (exactly one). Runs in a LIVE session and is
 * reversible via the Rollback Engine (re-create from snapshot).
 */
public record BulkDeleteRequest(
        @NotBlank String resourceType,
        List<UUID> ids,
        Map<String, String> filter) {

    public BulkDelete toCommand() {
        return new BulkDelete(resourceType, ids, filter);
    }
}
