package de.volantic.erp.sales.infrastructure.revision;

import de.volantic.erp.core.revision.DocumentPostingHandler;
import de.volantic.erp.sales.application.InvoiceService;
import de.volantic.erp.sales.domain.model.InvoiceId;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * {@code core.revision} document-posting handler for the sales {@link de.volantic.erp.sales.domain.model.Invoice}
 * (resource type {@code sales.invoice}). Lets a change-set bulk-post draft invoices and take the batch back
 * via storno (ADR-0006 §2). Both operations run through {@link InvoiceService} (authorization + audit + the
 * gap-free number range), never a direct DB write.
 */
@Component
class InvoicePostingHandler implements DocumentPostingHandler {

    private final InvoiceService invoices;

    InvoicePostingHandler(InvoiceService invoices) {
        this.invoices = invoices;
    }

    @Override
    public String resourceType() {
        return "sales.invoice";
    }

    @Override
    public void post(UUID id) {
        invoices.post(new InvoiceId(id));
    }

    @Override
    public void storno(UUID id) {
        invoices.cancel(new InvoiceId(id));
    }
}
