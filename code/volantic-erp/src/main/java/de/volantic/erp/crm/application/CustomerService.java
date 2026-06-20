package de.volantic.erp.crm.application;

import de.volantic.erp.core.OptimisticLock;
import de.volantic.erp.crm.application.port.out.CustomerRepository;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Customer use cases. Enforcement happens here at the service boundary (ADR-0004) via
 * {@code @PreAuthorize("hasPermission(...)")}, which routes through the central
 * {@code AuthorizationService}; the REST layer is only the entry point.
 */
@Service
public class CustomerService {

    private final CustomerRepository customers;

    CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Transactional
    @PreAuthorize("hasPermission(null, 'crm.customer:create')")
    public Customer createCustomer(String customerNumber, String name, String email) {
        if (customers.existsByCustomerNumber(customerNumber)) {
            throw new CustomerNumberAlreadyExistsException(customerNumber);
        }
        return customers.save(Customer.create(customerNumber, name, email));
    }

    // noRollbackFor: a not-found lookup must not mark a surrounding transaction rollback-only — the
    // change-set handlers call this inside a wider tx to probe existence (capture() returns null when
    // absent), and a missing resource there is an expected, non-fatal outcome, not a write failure.
    @Transactional(readOnly = true, noRollbackFor = CustomerNotFoundException.class)
    @PreAuthorize("hasPermission(null, 'crm.customer:read')")
    public Customer getCustomer(CustomerId id) {
        return customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'crm.customer:read')")
    public Page<Customer> listCustomers(Pageable pageable) {
        return customers.findAll(pageable);
    }

    /** Resolves a selection filter (name and/or email, exact match) to the matching customer ids. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'crm.customer:read')")
    public List<CustomerId> findCustomerIds(String name, String email) {
        return customers.findIds(name, email);
    }

    /**
     * Updates with an explicit optimistic-lock check (REST CRUD via ETag/If-Match): rejects the write if
     * the resource changed since the caller read {@code expectedVersion}.
     */
    @Transactional
    @PreAuthorize("hasPermission(null, 'crm.customer:update')")
    public Customer updateCustomer(CustomerId id, long expectedVersion, String name, String email) {
        Customer customer = customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
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
    @PreAuthorize("hasPermission(null, 'crm.customer:update')")
    public Customer updateCustomer(CustomerId id, String name, String email) {
        Customer customer = customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
        customer.rename(name);
        customer.changeEmail(email);
        return customers.save(customer);
    }
}
