package de.volantic.erp.security.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Request body for creating an org unit (REST v1). {@code parentId} null = create a root unit. */
public record CreateOrgUnitRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 200) String name,
        UUID parentId) {
}
