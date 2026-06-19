package de.volantic.erp.crm.api;

import de.volantic.erp.crm.domain.model.Customer;

/**
 * Response body representing a customer (REST v1). No JPA entity ever leaves the api layer. The
 * {@code version} is the optimistic-locking token (also returned as the {@code ETag} header); clients
 * echo it back in {@code If-Match} on update. {@code null} only for a not-yet-persisted instance.
 */
public record CustomerResponse(String id, String customerNumber, String name, String email, Long version) {

    static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.id().value().toString(),
                customer.customerNumber(),
                customer.name(),
                customer.email(),
                customer.version());
    }
}
