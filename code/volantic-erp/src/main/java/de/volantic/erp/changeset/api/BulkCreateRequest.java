package de.volantic.erp.changeset.api;

import de.volantic.erp.changeset.application.BulkCreate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

/**
 * Request body for a mass create (REST v1): create one resource of {@code resourceType} per field map in
 * {@code records}. Runs in a LIVE session and is reversible via the Rollback Engine.
 */
public record BulkCreateRequest(
        @NotBlank String resourceType,
        @NotEmpty List<Map<String, String>> records) {

    public BulkCreate toCommand() {
        return new BulkCreate(resourceType, records);
    }
}
