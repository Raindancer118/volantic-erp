package de.volantic.erp.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

/**
 * Request body for creating a versioned BOM (REST v1). The owning product id is taken from the path.
 * {@code validFrom}/{@code validTo} are optional; lines must be non-empty.
 */
public record CreateBomRequest(
        @Min(1) int version,
        LocalDate validFrom,
        LocalDate validTo,
        @NotEmpty @Valid List<BomLineDto> lines) {
}
