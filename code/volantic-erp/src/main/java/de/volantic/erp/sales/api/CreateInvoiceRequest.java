package de.volantic.erp.sales.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Request body for creating a draft invoice (REST v1). */
public record CreateInvoiceRequest(
        @NotNull UUID orgUnitId,
        @NotNull UUID customerId,
        @NotNull @Size(min = 3, max = 3) String currency,
        @NotEmpty @Valid List<Line> lines) {

    /** One requested invoice line. */
    public record Line(
            @jakarta.validation.constraints.NotBlank String description,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal unitPrice) {
    }
}
