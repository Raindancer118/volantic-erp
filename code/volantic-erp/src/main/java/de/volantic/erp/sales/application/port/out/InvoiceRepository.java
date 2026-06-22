package de.volantic.erp.sales.application.port.out;

import de.volantic.erp.sales.domain.model.Invoice;
import de.volantic.erp.sales.domain.model.InvoiceId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Outbound port for invoice persistence. The implementation maps between {@link Invoice} and JPA. */
public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(InvoiceId id);

    Page<Invoice> findAll(Pageable pageable);

    /** Returns all invoices whose {@code orgUnitId} is in the given set (ADR-0007 list-filtering). */
    Page<Invoice> findAllInOrgUnits(Set<UUID> orgUnitIds, Pageable pageable);
}
