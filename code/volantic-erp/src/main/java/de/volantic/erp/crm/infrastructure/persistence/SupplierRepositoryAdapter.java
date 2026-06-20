package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.crm.application.port.out.SupplierRepository;
import de.volantic.erp.crm.domain.model.Supplier;
import de.volantic.erp.crm.domain.model.SupplierId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Outbound adapter for {@link SupplierRepository}: maps between domain {@link Supplier} and JPA. */
@Component
class SupplierRepositoryAdapter implements SupplierRepository {

    private final SupplierJpaRepository jpa;

    SupplierRepositoryAdapter(SupplierJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Supplier save(Supplier supplier) {
        // Versioned aggregate → version-checked merge (optimistic locking); new → insert. No re-fetch.
        SupplierEntity entity = supplier.version() == null
                ? new SupplierEntity(supplier.id().value(), supplier.supplierNumber(), supplier.name(), supplier.email())
                : SupplierEntity.forUpdate(supplier.id().value(), supplier.supplierNumber(),
                        supplier.name(), supplier.email(), supplier.version());
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Supplier> findById(SupplierId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public boolean existsBySupplierNumber(String supplierNumber) {
        return jpa.existsBySupplierNumber(supplierNumber);
    }

    @Override
    public Page<Supplier> findAll(Pageable pageable) {
        return jpa.findAll(pageable).map(this::toDomain);
    }

    @Override
    public List<SupplierId> findIds(String name, String email) {
        return jpa.findIdsByFilter(name, email).stream().map(SupplierId::new).toList();
    }

    private Supplier toDomain(SupplierEntity entity) {
        return Supplier.reconstitute(new SupplierId(entity.getId()), entity.getVersion(),
                entity.supplierNumber(), entity.name(), entity.email());
    }
}
