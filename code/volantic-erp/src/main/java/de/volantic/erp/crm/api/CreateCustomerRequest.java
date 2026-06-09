package de.volantic.erp.crm.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for creating a customer (REST v1). */
public record CreateCustomerRequest(
        @NotBlank @Size(max = 50) String customerNumber,
        @NotBlank @Size(max = 200) String name,
        @Email @Size(max = 320) String email) {
}
