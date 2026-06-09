package de.volantic.erp.crm.application.port.out;

import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Outbound port for customer persistence. The implementation (infrastructure) maps between the pure
 * domain {@link Customer} and its JPA representation.
 */
public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(CustomerId id);

    boolean existsByCustomerNumber(String customerNumber);

    Page<Customer> findAll(Pageable pageable);
}
