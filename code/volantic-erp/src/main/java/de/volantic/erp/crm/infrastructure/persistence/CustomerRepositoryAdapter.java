package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.crm.application.port.out.CustomerRepository;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
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
        CustomerEntity entity = jpa.findById(customer.id().value())
                .orElseGet(() -> new CustomerEntity(
                        customer.id().value(), customer.customerNumber(), customer.name(), customer.email()));
        entity.apply(customer.name(), customer.email());
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
    public List<Customer> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    private Customer toDomain(CustomerEntity entity) {
        return Customer.reconstitute(
                new CustomerId(entity.getId()), entity.customerNumber(), entity.name(), entity.email());
    }
}
