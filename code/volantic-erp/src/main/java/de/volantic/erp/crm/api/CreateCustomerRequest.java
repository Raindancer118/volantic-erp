package de.volantic.erp.crm.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request body for creating a customer (REST v1). {@code orgUnitId} is the organizational unit the
 * customer belongs to (data scope for authorization); optional — defaults to the HQ org unit when
 * omitted, so existing clients keep working.
 */
public record CreateCustomerRequest(
        @NotBlank @Size(max = 50) String customerNumber,
        @NotBlank @Size(max = 200) String name,
        @Email @Size(max = 320) String email,
        UUID orgUnitId) {
}
