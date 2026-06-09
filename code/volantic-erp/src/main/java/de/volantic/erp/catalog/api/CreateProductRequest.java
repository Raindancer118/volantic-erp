package de.volantic.erp.catalog.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Request body for creating a product (REST v1). Price is amount + ISO-4217 currency code. */
public record CreateProductRequest(
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 200) String name,
        @NotNull @DecimalMin("0.0") BigDecimal priceAmount,
        @NotBlank @Size(min = 3, max = 3) String priceCurrency) {
}
