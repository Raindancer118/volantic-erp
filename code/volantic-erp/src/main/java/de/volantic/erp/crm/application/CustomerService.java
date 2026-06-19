package de.volantic.erp.crm.application;

import de.volantic.erp.crm.application.port.out.CustomerRepository;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import de.volantic.erp.crm.domain.model.OrgUnitId;
import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.AuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer use cases. Enforcement happens at this service boundary (ADR-0004). For operations on a
 * specific customer the data scope is the customer's {@link OrgUnitId} — which is only known once the
 * aggregate is loaded, so authorization is checked programmatically through the central
 * {@link AuthorizationService} rather than via a target-less {@code @PreAuthorize}. A globally granted
 * permission covers every org unit; a scoped grant only its own. The coarse {@code @PreAuthorize} is
 * kept only where no instance scope applies yet (listing).
 */
@Service
public class CustomerService {

    private static final String SCOPE_ORG_UNIT = "ORG_UNIT";

    private final CustomerRepository customers;
    private final AuthorizationService authorization;

    CustomerService(CustomerRepository customers, AuthorizationService authorization) {
        this.customers = customers;
        this.authorization = authorization;
    }

    @Transactional
    public Customer createCustomer(OrgUnitId orgUnitId, String customerNumber, String name, String email) {
        requireScoped("crm.customer:create", orgUnitId);
        if (customers.existsByCustomerNumber(customerNumber)) {
            throw new CustomerNumberAlreadyExistsException(customerNumber);
        }
        return customers.save(Customer.create(orgUnitId, customerNumber, name, email));
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(CustomerId id) {
        Customer customer = customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
        requireScoped("crm.customer:read", customer.orgUnitId());
        return customer;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'crm.customer:read')")
    public Page<Customer> listCustomers(Pageable pageable) {
        // Scoped listing (filtering to the caller's permitted org units) is a follow-up; for now this
        // requires the global read permission. See issue #28.
        return customers.findAll(pageable);
    }

    @Transactional
    public Customer updateCustomer(CustomerId id, String name, String email) {
        Customer customer = customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
        requireScoped("crm.customer:update", customer.orgUnitId());
        customer.rename(name);
        customer.changeEmail(email);
        return customers.save(customer);
    }

    /** Denies (→ 403) unless the current subject holds {@code permission} in the customer's org-unit scope. */
    private void requireScoped(String permission, OrgUnitId orgUnitId) {
        AccessScope scope = AccessScope.of(SCOPE_ORG_UNIT, orgUnitId.value());
        if (!authorization.isPermitted(currentSubject(), permission, scope)) {
            throw new AccessDeniedException("not permitted: " + permission + " in org unit " + orgUnitId.value());
        }
    }

    private static String currentSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("authentication required");
        }
        return authentication.getName();
    }
}
