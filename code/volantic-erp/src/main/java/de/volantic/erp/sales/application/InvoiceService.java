package de.volantic.erp.sales.application;

import de.volantic.erp.audit.AuditTrail;
import de.volantic.erp.core.numberrange.NumberRanges;
import de.volantic.erp.sales.SalesExceptions;
import de.volantic.erp.sales.application.port.out.InvoiceRepository;
import de.volantic.erp.sales.domain.model.Invoice;
import de.volantic.erp.sales.domain.model.InvoiceId;
import de.volantic.erp.sales.domain.model.InvoiceLine;
import de.volantic.erp.security.ScopeEnforcer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * Sales invoice use cases (DB architecture §5.1, ADR-0006 §2). Authorization is enforced here at the
 * service boundary (ADR-0004) via {@code sales.invoice:*} permissions, with org-unit scoping (ADR-0007).
 *
 * <p>Posting draws a gap-free number from {@link NumberRanges} <em>inside the same transaction</em> as the
 * status change, so the number and the posted document commit (or roll back) together — no GoBD gaps.
 * A posted invoice is never edited or deleted: correcting it means {@link #cancel(InvoiceId) cancelling}
 * it, which posts a separate storno document into the same range (forward-only, audit chain intact).
 */
@Service
public class InvoiceService {

    /** Number-range key for sales invoices (and their storno documents — same range, DB architecture §5.1). */
    public static final String INVOICE_NUMBER_RANGE = "sales.invoice";

    private static final String PERM_READ  = "sales.invoice:read";
    private static final String PERM_WRITE = "sales.invoice:write";
    private static final String PERM_POST  = "sales.invoice:post";

    private final InvoiceRepository invoices;
    private final NumberRanges numberRanges;
    private final AuditTrail audit;
    private final ScopeEnforcer scopeEnforcer;

    InvoiceService(InvoiceRepository invoices, NumberRanges numberRanges, AuditTrail audit,
                   ScopeEnforcer scopeEnforcer) {
        this.invoices = invoices;
        this.numberRanges = numberRanges;
        this.audit = audit;
        this.scopeEnforcer = scopeEnforcer;
        // Defined idempotently up front so the first post() does not race the range's creation.
        numberRanges.defineRange(INVOICE_NUMBER_RANGE, "RE-", 6, 1);
    }

    /**
     * Creates a draft invoice for the given org unit. The caller must hold {@code sales.invoice:write}
     * on that specific org unit (declarative, evaluated by Spring Security's AOP proxy before the method
     * body runs).
     */
    @Transactional
    @PreAuthorize("hasPermission(#orgUnitId, 'ORG_UNIT', 'sales.invoice:write')")
    public Invoice createDraft(UUID orgUnitId, UUID customerId, String currencyCode, List<InvoiceLine> lines) {
        return invoices.save(Invoice.createDraft(orgUnitId, customerId, Currency.getInstance(currencyCode), lines));
    }

    @Transactional(readOnly = true, noRollbackFor = SalesExceptions.InvoiceNotFound.class)
    public Invoice getInvoice(InvoiceId id) {
        Invoice invoice = invoices.findById(id).orElseThrow(() -> new SalesExceptions.InvoiceNotFound(id));
        scopeEnforcer.require(PERM_READ, invoice.orgUnitId());
        return invoice;
    }

    @Transactional(readOnly = true)
    public Page<Invoice> listInvoices(Pageable pageable) {
        scopeEnforcer.requireAnywhere(PERM_READ);
        return scopeEnforcer.permittedOrgUnits(PERM_READ)
                .map(ids -> invoices.findAllInOrgUnits(ids, pageable))
                .orElseGet(() -> invoices.findAll(pageable));
    }

    /** Posts a draft: assigns the next gap-free invoice number and freezes the document. */
    @Transactional
    public Invoice post(InvoiceId id) {
        Invoice invoice = load(id);
        scopeEnforcer.require(PERM_POST, invoice.orgUnitId());
        invoice.post(numberRanges.next(INVOICE_NUMBER_RANGE));
        Invoice saved = invoices.save(invoice);
        audit.record("sales.invoice-posted", "sales.invoice", id.value(), saved.documentNumber());
        return saved;
    }

    /**
     * Cancels a posted invoice via a storno (the GoBD-safe correction): creates and posts a negated storno
     * document into the same number range, then marks the original cancelled. Returns the storno document.
     * The original invoice is never altered beyond recording which storno cancelled it.
     */
    @Transactional
    public Invoice cancel(InvoiceId id) {
        Invoice original = load(id);
        scopeEnforcer.require(PERM_POST, original.orgUnitId());
        if (original.status() != de.volantic.erp.sales.domain.model.InvoiceStatus.POSTED) {
            throw new SalesExceptions.InvalidInvoiceState(
                    "only a POSTED invoice can be cancelled, not " + original.status());
        }
        // storno inherits original.orgUnitId (ADR-0007)
        Invoice storno = Invoice.storno(original);
        storno.post(numberRanges.next(INVOICE_NUMBER_RANGE));
        Invoice savedStorno = invoices.save(storno);

        original.cancel(savedStorno.id());
        invoices.save(original);

        audit.record("sales.invoice-cancelled", "sales.invoice", id.value(), savedStorno.documentNumber());
        return savedStorno;
    }

    private Invoice load(InvoiceId id) {
        return invoices.findById(id).orElseThrow(() -> new SalesExceptions.InvoiceNotFound(id));
    }
}
