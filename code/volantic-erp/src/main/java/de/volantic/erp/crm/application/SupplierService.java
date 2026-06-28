package de.volantic.erp.crm.application;

import de.volantic.erp.core.OptimisticLock;
import de.volantic.erp.crm.application.port.out.SupplierRepository;
import de.volantic.erp.crm.domain.model.Supplier;
import de.volantic.erp.crm.domain.model.SupplierId;
import de.volantic.erp.security.ScopeEnforcer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Supplier use cases. Authorization is enforced here at the service boundary (ADR-0004) with org-unit
 * scoping (ADR-0007), mirroring {@code CustomerService}: create is checked declaratively against the
 * requested unit; read/update/delete load the supplier and check against its own unit via
 * {@link ScopeEnforcer}; listing is filtered to the units the caller may see. A global grant still
 * covers every unit. The bulk/change-set path calls these same methods, so it inherits the checks.
 */
@Service
public class SupplierService {

    private static final String PERM_READ   = "crm.supplier:read";
    private static final String PERM_UPDATE = "crm.supplier:update";
    private static final String PERM_DELETE = "crm.supplier:delete";

    private final SupplierRepository suppliers;
    private final ScopeEnforcer scopeEnforcer;

    SupplierService(SupplierRepository suppliers, ScopeEnforcer scopeEnforcer) {
        this.suppliers = suppliers;
        this.scopeEnforcer = scopeEnforcer;
    }

    @Transactional
    @PreAuthorize("hasPermission(#orgUnitId, 'ORG_UNIT', 'crm.supplier:create')")
    public Supplier createSupplier(UUID orgUnitId, String supplierNumber, String name, String email) {
        if (suppliers.existsBySupplierNumber(supplierNumber)) {
            throw new SupplierNumberAlreadyExistsException(supplierNumber);
        }
        return suppliers.save(Supplier.create(orgUnitId, supplierNumber, name, email));
    }

    // noRollbackFor: a not-found read must not poison a surrounding transaction (see CustomerService).
    @Transactional(readOnly = true, noRollbackFor = SupplierNotFoundException.class)
    public Supplier getSupplier(SupplierId id) {
        Supplier supplier = suppliers.findById(id).orElseThrow(() -> new SupplierNotFoundException(id));
        scopeEnforcer.require(PERM_READ, supplier.orgUnitId());
        return supplier;
    }

    @Transactional(readOnly = true)
    public Page<Supplier> listSuppliers(Pageable pageable) {
        scopeEnforcer.requireAnywhere(PERM_READ);
        return scopeEnforcer.permittedOrgUnits(PERM_READ)
                .map(ids -> suppliers.findAllInOrgUnits(ids, pageable))
                .orElseGet(() -> suppliers.findAll(pageable));
    }

    /**
     * Resolves a selection filter (name and/or email, exact match) to the matching supplier ids — the
     * bulk-edit selection path. Per-instance scope is still enforced when each selected supplier is
     * subsequently updated (a denied one fails the bulk run).
     */
    @Transactional(readOnly = true)
    public List<SupplierId> findSupplierIds(String name, String email) {
        scopeEnforcer.requireAnywhere(PERM_READ);
        return suppliers.findIds(name, email);
    }

    /** Update with an explicit optimistic-lock check (REST CRUD via ETag/If-Match). */
    @Transactional
    public Supplier updateSupplier(SupplierId id, long expectedVersion, String name, String email) {
        Supplier supplier = suppliers.findById(id).orElseThrow(() -> new SupplierNotFoundException(id));
        scopeEnforcer.require(PERM_UPDATE, supplier.orgUnitId());
        OptimisticLock.check(supplier.version(), expectedVersion, id);
        supplier.rename(name);
        supplier.changeEmail(email);
        return suppliers.save(supplier);
    }

    /** Update without an explicit version — internal/bulk callers; still version-safe within the tx. */
    @Transactional
    public Supplier updateSupplier(SupplierId id, String name, String email) {
        Supplier supplier = suppliers.findById(id).orElseThrow(() -> new SupplierNotFoundException(id));
        scopeEnforcer.require(PERM_UPDATE, supplier.orgUnitId());
        supplier.rename(name);
        supplier.changeEmail(email);
        return suppliers.save(supplier);
    }

    /** Deletes a supplier (used directly and as a change-set bulk delete) after a scope check. */
    @Transactional
    public void deleteSupplier(SupplierId id) {
        Supplier supplier = suppliers.findById(id).orElseThrow(() -> new SupplierNotFoundException(id));
        scopeEnforcer.require(PERM_DELETE, supplier.orgUnitId());
        suppliers.deleteById(id);
    }

    /**
     * Re-creates a previously deleted supplier with its original id, business key and org unit (Rollback
     * Engine compensation of a DELETE). Bypasses the duplicate-number check — it restores exactly what
     * was removed. The caller must hold {@code crm.supplier:create} on the restored unit.
     */
    @Transactional
    @PreAuthorize("hasPermission(#orgUnitId, 'ORG_UNIT', 'crm.supplier:create')")
    public Supplier recreateSupplier(SupplierId id, UUID orgUnitId, String supplierNumber, String name, String email) {
        return suppliers.save(Supplier.reconstitute(id, orgUnitId, supplierNumber, name, email));
    }
}
