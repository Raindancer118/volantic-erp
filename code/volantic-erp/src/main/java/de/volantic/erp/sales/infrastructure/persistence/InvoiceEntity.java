package de.volantic.erp.sales.infrastructure.persistence;

import de.volantic.erp.core.AbstractEntity;
import de.volantic.erp.core.measure.Money;
import de.volantic.erp.sales.domain.model.Invoice;
import de.volantic.erp.sales.domain.model.InvoiceId;
import de.volantic.erp.sales.domain.model.InvoiceLine;
import de.volantic.erp.sales.domain.model.InvoiceStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/** JPA representation of a sales invoice with its lines (collection table). Table {@code sales.invoice}. */
@Entity
@Table(schema = "sales", name = "invoice")
class InvoiceEntity extends AbstractEntity {

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "org_unit_id", nullable = false, updatable = false)
    private UUID orgUnitId;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "storno_of")
    private UUID stornoOf;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "sales", name = "invoice_line", joinColumns = @JoinColumn(name = "invoice_id"))
    private List<InvoiceLineRow> lines = new ArrayList<>();

    protected InvoiceEntity() {
    }

    private InvoiceEntity(Invoice invoice) {
        super(invoice.id().value());
        this.customerId = invoice.customerId();
        this.orgUnitId = invoice.orgUnitId();
        this.currency = invoice.currency().getCurrencyCode();
        this.stornoOf = invoice.stornoOf() == null ? null : invoice.stornoOf().value();
        for (InvoiceLine line : invoice.lines()) {
            this.lines.add(new InvoiceLineRow(line.description(), line.quantity(), line.unitPrice().amount()));
        }
        applyMutableState(invoice);
    }

    static InvoiceEntity forNew(Invoice invoice) {
        return new InvoiceEntity(invoice);
    }

    /** Detached entity carrying the expected version for a version-checked merge (optimistic locking). */
    static InvoiceEntity forUpdate(Invoice invoice, long version) {
        InvoiceEntity entity = new InvoiceEntity(invoice);
        entity.markPersisted(version);
        return entity;
    }

    /** Applies the mutable lifecycle fields (status/number/dates) from the domain aggregate. */
    void applyMutableState(Invoice invoice) {
        this.status = invoice.status();
        this.documentNumber = invoice.documentNumber();
        this.issueDate = invoice.issueDate();
        this.cancelledBy = invoice.cancelledBy() == null ? null : invoice.cancelledBy().value();
    }

    Invoice toDomain() {
        Currency cur = Currency.getInstance(currency);
        List<InvoiceLine> domainLines = lines.stream()
                .map(row -> new InvoiceLine(row.description(), row.quantity(), Money.of(row.unitPriceAmount(), cur)))
                .toList();
        return Invoice.reconstitute(new InvoiceId(getId()), getVersion(), customerId, orgUnitId, cur, domainLines,
                stornoOf == null ? null : new InvoiceId(stornoOf), status, documentNumber, issueDate,
                cancelledBy == null ? null : new InvoiceId(cancelledBy));
    }
}
