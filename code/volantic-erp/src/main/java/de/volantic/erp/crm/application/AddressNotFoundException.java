package de.volantic.erp.crm.application;

import de.volantic.erp.crm.domain.model.AddressId;

/** Thrown when an address is requested by an id that does not exist. Mapped to HTTP 404. */
public class AddressNotFoundException extends CrmNotFoundException {

    public AddressNotFoundException(AddressId id) {
        super("address not found: " + id.value());
    }
}
