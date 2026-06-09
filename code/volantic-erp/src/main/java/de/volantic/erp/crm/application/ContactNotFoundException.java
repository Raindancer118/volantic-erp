package de.volantic.erp.crm.application;

import de.volantic.erp.crm.domain.model.ContactId;

/** Thrown when a contact is requested by an id that does not exist. Mapped to HTTP 404. */
public class ContactNotFoundException extends CrmNotFoundException {

    public ContactNotFoundException(ContactId id) {
        super("contact not found: " + id.value());
    }
}
