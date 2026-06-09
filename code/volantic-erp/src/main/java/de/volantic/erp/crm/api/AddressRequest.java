package de.volantic.erp.crm.api;

import de.volantic.erp.crm.domain.model.AddressType;
import de.volantic.erp.crm.domain.model.PartnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request body for creating an address. The owner ({@code ownerType}/{@code ownerId}) is immutable, so
 * updates use {@link UpdateAddressRequest} without it.
 */
public record AddressRequest(
        @NotNull PartnerType ownerType,
        @NotNull UUID ownerId,
        @NotNull AddressType type,
        @NotBlank @Size(max = 200) String street,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(min = 2, max = 2) String countryCode) {
}
