package de.volantic.erp.changeset.api;

import de.volantic.erp.changeset.application.BulkChange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request body for a mass edit (REST v1): apply the same {@code fieldChanges} to every selected resource
 * of one {@code resourceType}. Used both for the dry-run preview and for applying within a session.
 *
 * @param resourceType module-qualified type, e.g. {@code "crm.customer"}
 * @param ids          the resources to change
 * @param fieldChanges field name → new value
 */
public record BulkChangeRequest(
        @NotBlank String resourceType,
        @NotNull List<UUID> ids,
        @NotNull Map<String, String> fieldChanges) {

    /** Maps this request to the application-layer command. */
    public BulkChange toCommand() {
        return new BulkChange(resourceType, ids, fieldChanges);
    }
}
