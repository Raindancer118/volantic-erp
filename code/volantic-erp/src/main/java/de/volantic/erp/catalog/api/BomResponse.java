package de.volantic.erp.catalog.api;

import de.volantic.erp.catalog.domain.model.Bom;

import java.time.LocalDate;
import java.util.List;

/** Response body representing a versioned BOM with its lines (REST v1). */
public record BomResponse(
        String id,
        String productId,
        int version,
        LocalDate validFrom,
        LocalDate validTo,
        List<BomLineDto> lines) {

    static BomResponse from(Bom bom) {
        List<BomLineDto> lines = bom.lines().stream()
                .map(line -> new BomLineDto(
                        line.componentId().value(),
                        line.quantity().amount(),
                        line.quantity().unit().code()))
                .toList();
        return new BomResponse(
                bom.id().value().toString(),
                bom.productId().value().toString(),
                bom.version(),
                bom.validFrom(),
                bom.validTo(),
                lines);
    }
}
