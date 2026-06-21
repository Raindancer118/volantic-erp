package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.crm.application.port.out.CustomerRepository;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Outbound adapter for {@link CustomerRepository}: maps between the pure domain {@link Customer} and the
 * JPA {@link CustomerEntity}. Insert vs. update is decided by whether an entity with the domain id
 * already exists (the customer number is immutable).
 */
@Component
class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerJpaRepository jpa;

    CustomerRepositoryAdapter(CustomerJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Customer save(Customer customer) {
        // A versioned aggregate is saved as a detached, version-checked merge (optimistic locking); a
        // new one (no version) is inserted. We deliberately do NOT re-fetch first — re-fetching would
        // load the latest row version and discard the caller's expected version, reopening the
        // lost-update window the @Version field exists to close.
        CustomerEntity entity = customer.version() == null
                ? new CustomerEntity(customer.id().value(), customer.customerNumber(), customer.name(), customer.email())
                : CustomerEntity.forUpdate(customer.id().value(), customer.customerNumber(),
                        customer.name(), customer.email(), customer.version());
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByCustomerNumber(String customerNumber) {
        return jpa.existsByCustomerNumber(customerNumber);
    }

    @Override
    public Page<Customer> findAll(Pageable pageable) {
        return jpa.findAll(pageable).map(this::toDomain);
    }

    @Override
    public List<CustomerId> findIds(String name, String email) {
        return jpa.findIdsByFilter(name, email).stream().map(CustomerId::new).toList();
    }

    @Override
    public boolean deleteById(CustomerId id) {
        if (!jpa.existsById(id.value())) {
            return false;
        }
        jpa.deleteById(id.value());
        return true;
    }

    private Customer toDomain(CustomerEntity entity) {
        return Customer.reconstitute(new CustomerId(entity.getId()), entity.getVersion(),
                entity.customerNumber(), entity.name(), entity.email());
    }
}
