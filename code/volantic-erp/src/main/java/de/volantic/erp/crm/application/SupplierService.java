package de.volantic.erp.crm.application;

import de.volantic.erp.core.OptimisticLock;
import de.volantic.erp.crm.application.port.out.SupplierRepository;
import de.volantic.erp.crm.domain.model.Supplier;
import de.volantic.erp.crm.domain.model.SupplierId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Supplier use cases. Authorization is enforced here at the service boundary (ADR-0004). */
@Service
public class SupplierService {

    private final SupplierRepository suppliers;

    SupplierService(SupplierRepository suppliers) {
        this.suppliers = suppliers;
    }

    @Transactional
    @PreAuthorize("hasPermission(null, 'crm.supplier:create')")
    public Supplier createSupplier(String supplierNumber, String name, String email) {
        if (suppliers.existsBySupplierNumber(supplierNumber)) {
            throw new SupplierNumberAlreadyExistsException(supplierNumber);
        }
        return suppliers.save(Supplier.create(supplierNumber, name, email));
    }

    // noRollbackFor: a not-found read must not poison a surrounding transaction (see CustomerService).
    @Transactional(readOnly = true, noRollbackFor = SupplierNotFoundException.class)
    @PreAuthorize("hasPermission(null, 'crm.supplier:read')")
    public Supplier getSupplier(SupplierId id) {
        return suppliers.findById(id).orElseThrow(() -> new SupplierNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'crm.supplier:read')")
    public Page<Supplier> listSuppliers(Pageable pageable) {
        return suppliers.findAll(pageable);
    }

    /** Resolves a selection filter (name and/or email, exact match) to the matching supplier ids. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'crm.supplier:read')")
    public List<SupplierId> findSupplierIds(String name, String email) {
        return suppliers.findIds(name, email);
    }

    /** Update with an explicit optimistic-lock check (REST CRUD via ETag/If-Match). */
    @Transactional
    @PreAuthorize("hasPermission(null, 'crm.supplier:update')")
    public Supplier updateSupplier(SupplierId id, long expectedVersion, String name, String email) {
        Supplier supplier = suppliers.findById(id).orElseThrow(() -> new SupplierNotFoundException(id));
        OptimisticLock.check(supplier.version(), expectedVersion, id);
        supplier.rename(name);
        supplier.changeEmail(email);
        return suppliers.save(supplier);
    }

    /** Update without an explicit version — internal/bulk callers; still version-safe within the tx. */
    @Transactional
    @PreAuthorize("hasPermission(null, 'crm.supplier:update')")
    public Supplier updateSupplier(SupplierId id, String name, String email) {
        Supplier supplier = suppliers.findById(id).orElseThrow(() -> new SupplierNotFoundException(id));
        supplier.rename(name);
        supplier.changeEmail(email);
        return suppliers.save(supplier);
    }
}
