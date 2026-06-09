package de.volantic.erp.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** One BOM line in a request/response: a component product id with a quantity and unit (REST v1). */
public record BomLineDto(
        @NotNull UUID componentId,
        @NotNull @Positive BigDecimal quantity,
        @NotBlank @Size(max = 20) String unit) {
}
