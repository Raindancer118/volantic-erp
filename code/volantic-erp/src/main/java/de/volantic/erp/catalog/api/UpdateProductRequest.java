package de.volantic.erp.catalog.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Request body for updating a product's name and list price (REST v1). The SKU is immutable. */
public record UpdateProductRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull @DecimalMin("0.0") BigDecimal priceAmount,
        @NotBlank @Size(min = 3, max = 3) String priceCurrency) {
}
