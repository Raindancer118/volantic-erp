package de.volantic.erp.crm.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for updating a customer's mutable fields (REST v1). */
public record UpdateCustomerRequest(
        @NotBlank @Size(max = 200) String name,
        @Email @Size(max = 320) String email) {
}
