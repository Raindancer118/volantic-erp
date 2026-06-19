package de.volantic.erp.crm.api;

import de.volantic.erp.crm.domain.model.Customer;

/**
 * Response body representing a customer (REST v1). No JPA entity ever leaves the api layer.
 * {@code orgUnitId} exposes the customer's organizational unit (its authorization data scope).
 */
public record CustomerResponse(String id, String orgUnitId, String customerNumber, String name, String email) {

    static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.id().value().toString(),
                customer.orgUnitId().value().toString(),
                customer.customerNumber(),
                customer.name(),
                customer.email());
    }
}
