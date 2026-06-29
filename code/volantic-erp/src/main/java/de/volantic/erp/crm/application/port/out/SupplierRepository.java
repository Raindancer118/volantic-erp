package de.volantic.erp.crm.application.port.out;

import de.volantic.erp.crm.domain.model.Supplier;
import de.volantic.erp.crm.domain.model.SupplierId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Outbound port for supplier persistence. */
public interface SupplierRepository {

    Supplier save(Supplier supplier);

    Optional<Supplier> findById(SupplierId id);

    boolean existsBySupplierNumber(String supplierNumber);

    Page<Supplier> findAll(Pageable pageable);

    /** Returns all suppliers whose {@code orgUnitId} is in the given set (ADR-0007 list-filtering). */
    Page<Supplier> findAllInOrgUnits(Set<UUID> orgUnitIds, Pageable pageable);

    /** Ids of suppliers matching the optional equality filter (null fields ignored, ANDed). */
    List<SupplierId> findIds(String name, String email);

    /** Deletes the supplier; returns {@code false} if it did not exist. */
    boolean deleteById(SupplierId id);
}
