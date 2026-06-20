package de.volantic.erp.changeset.api;

import de.volantic.erp.changeset.application.BulkPostDocuments;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Request body for mass-posting draft documents within a session (REST v1). Runs in a LIVE session and is
 * reversible via the Rollback Engine (storno).
 */
public record BulkPostDocumentsRequest(
        @NotBlank String resourceType,
        @NotEmpty List<UUID> ids) {

    public BulkPostDocuments toCommand() {
        return new BulkPostDocuments(resourceType, ids);
    }
}
