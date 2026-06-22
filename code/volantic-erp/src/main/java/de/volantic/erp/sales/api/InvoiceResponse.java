package de.volantic.erp.sales.api;

import de.volantic.erp.sales.domain.model.Invoice;
import de.volantic.erp.sales.domain.model.InvoiceLine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Response body representing a sales invoice (REST v1). No JPA entity ever leaves the api layer. */
public record InvoiceResponse(
        String id,
        long version,
        String orgUnitId,
        String customerId,
        String currency,
        String status,
        String documentNumber,
        LocalDate issueDate,
        String stornoOf,
        String cancelledBy,
        BigDecimal total,
        List<Line> lines) {

    public record Line(String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    static InvoiceResponse from(Invoice invoice) {
        List<Line> lines = invoice.lines().stream()
                .map(InvoiceResponse::lineOf)
                .toList();
        return new InvoiceResponse(
                invoice.id().value().toString(),
                invoice.version() == null ? 0L : invoice.version(),
                invoice.orgUnitId().toString(),
                invoice.customerId().toString(),
                invoice.currency().getCurrencyCode(),
                invoice.status().name(),
                invoice.documentNumber(),
                invoice.issueDate(),
                invoice.stornoOf() == null ? null : invoice.stornoOf().value().toString(),
                invoice.cancelledBy() == null ? null : invoice.cancelledBy().value().toString(),
                invoice.total().amount(),
                lines);
    }

    private static Line lineOf(InvoiceLine line) {
        return new Line(line.description(), line.quantity(), line.unitPrice().amount(), line.lineTotal().amount());
    }
}
