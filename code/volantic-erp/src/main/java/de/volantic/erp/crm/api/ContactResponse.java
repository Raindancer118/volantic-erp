package de.volantic.erp.crm.api;

import de.volantic.erp.crm.domain.model.Contact;

/** Response body representing a contact (REST v1). */
public record ContactResponse(
        String id, long version, String ownerType, String ownerId,
        String firstName, String lastName, String email, String phone) {

    static ContactResponse from(Contact contact) {
        return new ContactResponse(
                contact.id().value().toString(),
                contact.version() == null ? 0L : contact.version(),
                contact.owner().type().name(),
                contact.owner().id().toString(),
                contact.firstName(),
                contact.lastName(),
                contact.email(),
                contact.phone());
    }
}
