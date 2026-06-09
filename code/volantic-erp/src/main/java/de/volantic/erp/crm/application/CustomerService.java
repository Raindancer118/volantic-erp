package de.volantic.erp.crm.application;

import de.volantic.erp.crm.application.port.out.CustomerRepository;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'crm.customer:read')")
    public Customer getCustomer(CustomerId id) {
        return customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'crm.customer:read')")
    public Page<Customer> listCustomers(Pageable pageable) {
        return customers.findAll(pageable);
    }

    @Transactional
    @PreAuthorize("hasPermission(null, 'crm.customer:update')")
    public Customer updateCustomer(CustomerId id, String name, String email) {
        Customer customer = customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
        customer.rename(name);
        customer.changeEmail(email);
        return customers.save(customer);
    }
}
