package de.volantic.erp.security.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for renaming an org unit (REST v1). The {@code code} business key is immutable. */
public record RenameOrgUnitRequest(@NotBlank @Size(max = 200) String name) {
}
