package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.crm.application.port.out.SupplierRepository;
import de.volantic.erp.crm.domain.model.Supplier;
import de.volantic.erp.crm.domain.model.SupplierId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

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
        SupplierEntity entity = jpa.findById(supplier.id().value())
                .orElseGet(() -> new SupplierEntity(
                        supplier.id().value(), supplier.supplierNumber(), supplier.name(), supplier.email()));
        entity.apply(supplier.name(), supplier.email());
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

    private Supplier toDomain(SupplierEntity entity) {
        return Supplier.reconstitute(
                new SupplierId(entity.getId()), entity.supplierNumber(), entity.name(), entity.email());
    }
}
