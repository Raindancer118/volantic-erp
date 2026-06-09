package de.volantic.erp.crm.application.port.out;

import de.volantic.erp.crm.domain.model.Supplier;
import de.volantic.erp.crm.domain.model.SupplierId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/** Outbound port for supplier persistence. */
public interface SupplierRepository {

    Supplier save(Supplier supplier);

    Optional<Supplier> findById(SupplierId id);

    boolean existsBySupplierNumber(String supplierNumber);

    Page<Supplier> findAll(Pageable pageable);
}
