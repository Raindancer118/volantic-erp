package de.volantic.erp.crm.application;

import de.volantic.erp.crm.domain.model.CustomerId;

/** Thrown when a customer is requested by an id that does not exist. Mapped to HTTP 404 in the api layer. */
public class CustomerNotFoundException extends CrmNotFoundException {

    public CustomerNotFoundException(CustomerId id) {
        super("customer not found: " + id.value());
    }
}
