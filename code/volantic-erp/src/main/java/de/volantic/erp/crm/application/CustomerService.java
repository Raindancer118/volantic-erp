package de.volantic.erp.crm.application;

import de.volantic.erp.core.OptimisticLock;
import de.volantic.erp.crm.application.port.out.CustomerRepository;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import de.volantic.erp.security.ScopeEnforcer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Customer use cases. Enforcement happens here at the service boundary (ADR-0004) with org-unit scoping
 * (ADR-0007): create is checked declaratively against the requested unit; read/update/delete load the
 * customer first and then check against its own org unit via {@link ScopeEnforcer}; listing is filtered
 * to the units the caller may see. A global grant still covers every unit. The REST layer is only the
 * entry point. The bulk/change-set path calls these same methods, so it inherits the scope checks.
 */
@Service
public class CustomerService {

    private static final String PERM_READ   = "crm.customer:read";
    private static final String PERM_UPDATE = "crm.customer:update";
    private static final String PERM_DELETE = "crm.customer:delete";

    private final CustomerRepository customers;
    private final ScopeEnforcer scopeEnforcer;

    CustomerService(CustomerRepository customers, ScopeEnforcer scopeEnforcer) {
        this.customers = customers;
        this.scopeEnforcer = scopeEnforcer;
    }

    /**
     * Creates a customer in the given org unit. The caller must hold {@code crm.customer:create} on that
     * specific unit (declarative, evaluated by Spring Security before the method body runs).
     */
    @Transactional
    @PreAuthorize("hasPermission(#orgUnitId, 'ORG_UNIT', 'crm.customer:create')")
    public Customer createCustomer(UUID orgUnitId, String customerNumber, String name, String email) {
        if (customers.existsByCustomerNumber(customerNumber)) {
            throw new CustomerNumberAlreadyExistsException(customerNumber);
        }
        return customers.save(Customer.create(orgUnitId, customerNumber, name, email));
    }

    // noRollbackFor: a not-found lookup must not mark a surrounding transaction rollback-only — the
    // change-set handlers call this inside a wider tx to probe existence (capture() returns null when
    // absent), and a missing resource there is an expected, non-fatal outcome, not a write failure.
    @Transactional(readOnly = true, noRollbackFor = CustomerNotFoundException.class)
    public Customer getCustomer(CustomerId id) {
        Customer customer = customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
        scopeEnforcer.require(PERM_READ, customer.orgUnitId());
        return customer;
    }

    @Transactional(readOnly = true)
    public Page<Customer> listCustomers(Pageable pageable) {
        scopeEnforcer.requireAnywhere(PERM_READ);
        return scopeEnforcer.permittedOrgUnits(PERM_READ)
                .map(ids -> customers.findAllInOrgUnits(ids, pageable))
                .orElseGet(() -> customers.findAll(pageable));
    }

    /**
     * Resolves a selection filter (name and/or email, exact match) to the matching customer ids — the
     * bulk-edit selection path. The caller must hold the read permission somewhere; per-instance scope is
     * still enforced when each selected customer is subsequently updated (a denied one fails the bulk run).
     */
    @Transactional(readOnly = true)
    public List<CustomerId> findCustomerIds(String name, String email) {
        scopeEnforcer.requireAnywhere(PERM_READ);
        return customers.findIds(name, email);
    }

    /**
     * Updates with an explicit optimistic-lock check (REST CRUD via ETag/If-Match): rejects the write if
     * the resource changed since the caller read {@code expectedVersion}.
     */
    @Transactional
    public Customer updateCustomer(CustomerId id, long expectedVersion, String name, String email) {
        Customer customer = customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
        scopeEnforcer.require(PERM_UPDATE, customer.orgUnitId());
        OptimisticLock.check(customer.version(), expectedVersion, id);
        customer.rename(name);
        customer.changeEmail(email);
        return customers.save(customer);
    }

    /**
     * Updates without an explicit expected version — for internal/bulk callers (the change-set engine)
     * that have no client ETag. Still version-safe within the transaction: the loaded aggregate carries
     * its version and is saved via a version-checked merge, so a concurrent change is detected.
     */
    @Transactional
    public Customer updateCustomer(CustomerId id, String name, String email) {
        Customer customer = customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
        scopeEnforcer.require(PERM_UPDATE, customer.orgUnitId());
        customer.rename(name);
        customer.changeEmail(email);
        return customers.save(customer);
    }

    /** Deletes a customer (used directly and as a change-set bulk delete) after a scope check. */
    @Transactional
    public void deleteCustomer(CustomerId id) {
        Customer customer = customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
        scopeEnforcer.require(PERM_DELETE, customer.orgUnitId());
        customers.deleteById(id);
    }

    /**
     * Re-creates a previously deleted customer with its original id, business key and org unit (Rollback
     * Engine compensation of a DELETE). Bypasses the duplicate-number check on purpose — it restores
     * exactly what was removed. The caller must hold {@code crm.customer:create} on the restored unit.
     */
    @Transactional
    @PreAuthorize("hasPermission(#orgUnitId, 'ORG_UNIT', 'crm.customer:create')")
    public Customer recreateCustomer(CustomerId id, UUID orgUnitId, String customerNumber, String name, String email) {
        return customers.save(Customer.reconstitute(id, orgUnitId, customerNumber, name, email));
    }
}
