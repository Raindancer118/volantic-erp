package de.volantic.erp.sales.infrastructure.persistence;

import de.volantic.erp.sales.application.port.out.InvoiceRepository;
import de.volantic.erp.sales.domain.model.Invoice;
import de.volantic.erp.sales.domain.model.InvoiceId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Outbound adapter for {@link InvoiceRepository}: maps between the domain {@link Invoice} and the JPA
 * {@link InvoiceEntity}. A versioned invoice is saved as a detached, version-checked merge (optimistic
 * locking) — never re-fetched first, which would discard the expected version.
 */
@Component
class InvoiceRepositoryAdapter implements InvoiceRepository {

    private final InvoiceJpaRepository jpa;

    InvoiceRepositoryAdapter(InvoiceJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceEntity entity = invoice.version() == null
                ? InvoiceEntity.forNew(invoice)
                : InvoiceEntity.forUpdate(invoice, invoice.version());
        return jpa.save(entity).toDomain();
    }

    @Override
    public Optional<Invoice> findById(InvoiceId id) {
        return jpa.findById(id.value()).map(InvoiceEntity::toDomain);
    }

    @Override
    public Page<Invoice> findAll(Pageable pageable) {
        return jpa.findAll(pageable).map(InvoiceEntity::toDomain);
    }

    @Override
    public Page<Invoice> findAllInOrgUnits(Set<UUID> orgUnitIds, Pageable pageable) {
        return jpa.findAllByOrgUnitIdIn(orgUnitIds, pageable).map(InvoiceEntity::toDomain);
    }
}
