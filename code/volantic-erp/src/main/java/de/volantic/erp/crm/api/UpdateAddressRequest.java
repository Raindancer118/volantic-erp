package de.volantic.erp.crm.api;

import de.volantic.erp.crm.domain.model.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body for updating an address (owner is immutable). */
public record UpdateAddressRequest(
        @NotNull AddressType type,
        @NotBlank @Size(max = 200) String street,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(min = 2, max = 2) String countryCode) {
}
