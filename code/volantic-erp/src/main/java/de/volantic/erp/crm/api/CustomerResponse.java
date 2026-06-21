package de.volantic.erp.crm.api;

import de.volantic.erp.crm.domain.model.Customer;

/** Response body representing a customer (REST v1). No JPA entity ever leaves the api layer. */
public record CustomerResponse(String id, long version, String customerNumber, String name, String email) {

    static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.id().value().toString(),
                customer.version() == null ? 0L : customer.version(),
                customer.customerNumber(),
                customer.name(),
                customer.email());
    }
}
