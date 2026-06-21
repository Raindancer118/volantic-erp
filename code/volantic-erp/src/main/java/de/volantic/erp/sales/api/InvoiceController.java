package de.volantic.erp.sales.api;

import de.volantic.erp.core.measure.Money;
import de.volantic.erp.core.web.PageResponse;
import de.volantic.erp.sales.application.InvoiceService;
import de.volantic.erp.sales.domain.model.Invoice;
import de.volantic.erp.sales.domain.model.InvoiceId;
import de.volantic.erp.sales.domain.model.InvoiceLine;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * REST v1 endpoints for sales invoices (Belege). Thin adapter: it maps DTOs and delegates to
 * {@link InvoiceService}, which enforces authorization. Versioned under {@code /v1} (additive-only).
 * A draft is created and edited, then posted (gap-free number, immutable); a posted invoice is corrected
 * only by {@code POST .../cancel}, which posts a storno document and returns it.
 */
@RestController
@RequestMapping("/v1/sales/invoices")
class InvoiceController {

    private final InvoiceService invoices;

    InvoiceController(InvoiceService invoices) {
        this.invoices = invoices;
    }

    @PostMapping
    ResponseEntity<InvoiceResponse> create(@Valid @RequestBody CreateInvoiceRequest request) {
        Currency currency = Currency.getInstance(request.currency());
        List<InvoiceLine> lines = request.lines().stream()
                .map(line -> new InvoiceLine(line.description(), line.quantity(), Money.of(line.unitPrice(), currency)))
                .toList();
        Invoice created = invoices.createDraft(request.customerId(), request.currency(), lines);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id().value()).toUri();
        return ResponseEntity.created(location).body(InvoiceResponse.from(created));
    }

    @GetMapping("/{id}")
    InvoiceResponse getById(@PathVariable UUID id) {
        return InvoiceResponse.from(invoices.getInvoice(new InvoiceId(id)));
    }

    @GetMapping
    PageResponse<InvoiceResponse> list(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(invoices.listInvoices(pageable), InvoiceResponse::from);
    }

    @PostMapping("/{id}/post")
    InvoiceResponse post(@PathVariable UUID id) {
        return InvoiceResponse.from(invoices.post(new InvoiceId(id)));
    }

    @PostMapping("/{id}/cancel")
    ResponseEntity<InvoiceResponse> cancel(@PathVariable UUID id) {
        Invoice storno = invoices.cancel(new InvoiceId(id));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/v1/sales/invoices/{id}").buildAndExpand(storno.id().value()).toUri();
        return ResponseEntity.created(location).body(InvoiceResponse.from(storno));
    }
}
