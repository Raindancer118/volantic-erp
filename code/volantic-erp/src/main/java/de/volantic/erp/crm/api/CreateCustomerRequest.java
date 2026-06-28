package de.volantic.erp.crm.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Request body for creating a customer (REST v1). */
public record CreateCustomerRequest(
        @NotNull UUID orgUnitId,
        @NotBlank @Size(max = 50) String customerNumber,
        @NotBlank @Size(max = 200) String name,
        @Email @Size(max = 320) String email) {
}
