package de.volantic.erp.crm.api;

import de.volantic.erp.crm.domain.model.PartnerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Request body for creating a contact (owner is immutable, so updates use {@link UpdateContactRequest}). */
public record ContactRequest(
        @NotNull PartnerType ownerType,
        @NotNull UUID ownerId,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Email @Size(max = 320) String email,
        @Size(max = 50) String phone) {
}
