package de.volantic.erp.crm.application.port.out;

import de.volantic.erp.crm.domain.model.Supplier;
import de.volantic.erp.crm.domain.model.SupplierId;

import java.util.List;
import java.util.Optional;

/** Outbound port for supplier persistence. */
public interface SupplierRepository {

    Supplier save(Supplier supplier);

    Optional<Supplier> findById(SupplierId id);

    boolean existsBySupplierNumber(String supplierNumber);

    List<Supplier> findAll();
}
