package de.volantic.erp.sales.application.port.out;

import de.volantic.erp.sales.domain.model.Invoice;
import de.volantic.erp.sales.domain.model.InvoiceId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/** Outbound port for invoice persistence. The implementation maps between {@link Invoice} and JPA. */
public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(InvoiceId id);

    Page<Invoice> findAll(Pageable pageable);
}
