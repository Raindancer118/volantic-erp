package de.volantic.erp.sales.domain.model;

import de.volantic.erp.core.measure.Money;

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * A sales invoice — the {@code sales} aggregate root and the canonical "Beleg" (DB architecture §5.1,
 * ADR-0006 §2). Pure domain: owns its identity, lines and the GoBD lifecycle invariants, free of
 * persistence and framework concerns.
 *
 * <p>GoBD rules enforced here:
 * <ul>
 *   <li>A {@code DRAFT} has no number and is editable. {@link #post(String)} finalizes it: it assigns the
 *       gap-free document number (drawn by the application from {@code core.numberrange}) and freezes it.</li>
 *   <li>A {@code POSTED} invoice is immutable — it can neither be edited nor deleted. The only correction
 *       is {@link #cancel(InvoiceId) cancellation} by a separate <strong>storno</strong> document; the
 *       original is never altered (forward-only).</li>
 *   <li>A storno is itself an invoice created via {@link #storno(Invoice)}: a negated copy of the
 *       cancelled invoice that posts into the <em>same</em> number range.</li>
 * </ul>
 *
 * <p>Org-unit scoping (ADR-0007): each invoice is owned by exactly one org unit ({@link #orgUnitId()}),
 * supplied at draft creation and immutable thereafter. A storno inherits the org unit of the original.
 */
public final class Invoice {

    private final InvoiceId id;
    private final Long version;
    private final UUID customerId;
    private final UUID orgUnitId;
    private final Currency currency;
    private final List<InvoiceLine> lines;
    /** For a storno document, the invoice it cancels; otherwise {@code null}. */
    private final InvoiceId stornoOf;
    private InvoiceStatus status;
    private String documentNumber;
    private LocalDate issueDate;
    /** For a cancelled invoice, the storno document that cancelled it; otherwise {@code null}. */
    private InvoiceId cancelledBy;

    private Invoice(InvoiceId id, Long version, UUID customerId, UUID orgUnitId, Currency currency,
                    List<InvoiceLine> lines, InvoiceId stornoOf, InvoiceStatus status,
                    String documentNumber, LocalDate issueDate, InvoiceId cancelledBy) {
        this.id = requireNonNull(id, "id");
        this.version = version;
        this.customerId = requireNonNull(customerId, "customerId");
        this.orgUnitId = requireNonNull(orgUnitId, "orgUnitId");
        this.currency = requireNonNull(currency, "currency");
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("an invoice must have at least one line");
        }
        if (lines.stream().anyMatch(line -> !line.unitPrice().currency().equals(currency))) {
            throw new IllegalArgumentException("all line prices must be in the invoice currency " + currency.getCurrencyCode());
        }
        this.lines = List.copyOf(lines);
        this.stornoOf = stornoOf;
        this.status = requireNonNull(status, "status");
        this.documentNumber = documentNumber;
        this.issueDate = issueDate;
        this.cancelledBy = cancelledBy;
    }

    /**
     * Creates a new editable draft invoice for a customer, issued by the given org unit (ADR-0007).
     * {@code orgUnitId} is immutable and must not be {@code null}.
     */
    public static Invoice createDraft(UUID orgUnitId, UUID customerId, Currency currency, List<InvoiceLine> lines) {
        return new Invoice(InvoiceId.newId(), null, customerId, orgUnitId, currency, lines, null,
                InvoiceStatus.DRAFT, null, null, null);
    }

    /** Re-creates an invoice from persisted state (used by the persistence adapter). */
    public static Invoice reconstitute(InvoiceId id, long version, UUID customerId, UUID orgUnitId,
                                       Currency currency, List<InvoiceLine> lines, InvoiceId stornoOf,
                                       InvoiceStatus status, String documentNumber, LocalDate issueDate,
                                       InvoiceId cancelledBy) {
        return new Invoice(id, version, customerId, orgUnitId, currency, lines, stornoOf, status,
                documentNumber, issueDate, cancelledBy);
    }

    /**
     * Finalizes a draft into a posted legal document: assigns the gap-free {@code documentNumber} (drawn
     * by the application within the same transaction) and the issue date. Only valid for a {@code DRAFT}.
     */
    public void post(String documentNumber) {
        if (status != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("only a DRAFT invoice can be posted, not " + status);
        }
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new IllegalArgumentException("documentNumber must not be blank");
        }
        this.documentNumber = documentNumber;
        this.issueDate = LocalDate.now();
        this.status = InvoiceStatus.POSTED;
    }

    /**
     * Builds the storno (cancellation) document for a posted invoice: a draft copy of its lines, marked as
     * a storno of the original. It is posted like any invoice (drawing the next number in the same range),
     * which keeps the GoBD number/audit chain intact instead of altering the original. The credit sign is
     * carried at the document level — {@link #total()} of a storno is the negation — so line invariants
     * (positive quantity, non-negative unit price) stay intact.
     */
    public static Invoice storno(Invoice original) {
        if (original.status != InvoiceStatus.POSTED) {
            throw new IllegalStateException("only a POSTED invoice can be cancelled, not " + original.status);
        }
        List<InvoiceLine> stornoLines = original.lines.stream()
                .map(line -> new InvoiceLine("Storno: " + line.description(), line.quantity(), line.unitPrice()))
                .toList();
        // orgUnitId is inherited from the original — a storno belongs to the same unit (ADR-0007).
        return new Invoice(InvoiceId.newId(), null, original.customerId, original.orgUnitId, original.currency,
                stornoLines, original.id, InvoiceStatus.DRAFT, null, null, null);
    }

    /** Marks this posted invoice cancelled by the given storno document. Only valid for a {@code POSTED}. */
    public void cancel(InvoiceId stornoDocument) {
        if (status != InvoiceStatus.POSTED) {
            throw new IllegalStateException("only a POSTED invoice can be cancelled, not " + status);
        }
        this.status = InvoiceStatus.CANCELLED;
        this.cancelledBy = requireNonNull(stornoDocument, "stornoDocument");
    }

    /**
     * The invoice total: the sum of its line totals — <em>negated</em> for a storno document, which is a
     * credit (the lines themselves stay positive; the credit sign lives at the document level).
     */
    public Money total() {
        Money sum = Money.zero(currency);
        for (InvoiceLine line : lines) {
            sum = sum.plus(line.lineTotal());
        }
        return isStorno() ? negate(sum) : sum;
    }

    public boolean isStorno() {
        return stornoOf != null;
    }

    public InvoiceId id() {
        return id;
    }

    public Long version() {
        return version;
    }

    public UUID customerId() {
        return customerId;
    }

    /** The org unit that issued this invoice; immutable and never {@code null} (ADR-0007). */
    public UUID orgUnitId() {
        return orgUnitId;
    }

    public Currency currency() {
        return currency;
    }

    public List<InvoiceLine> lines() {
        return lines;
    }

    public InvoiceStatus status() {
        return status;
    }

    public String documentNumber() {
        return documentNumber;
    }

    public LocalDate issueDate() {
        return issueDate;
    }

    public InvoiceId stornoOf() {
        return stornoOf;
    }

    public InvoiceId cancelledBy() {
        return cancelledBy;
    }

    /** Identity equality: two invoices are the same iff they share an id, regardless of mutable state. */
    @Override
    public boolean equals(Object other) {
        return other instanceof Invoice that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    private static Money negate(Money money) {
        return money.multiply(java.math.BigDecimal.valueOf(-1));
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
