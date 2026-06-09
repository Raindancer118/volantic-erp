package de.volantic.erp.crm.api;

import de.volantic.erp.crm.domain.model.Address;

/** Response body representing an address (REST v1). */
public record AddressResponse(
        String id, String ownerType, String ownerId, String type,
        String street, String postalCode, String city, String countryCode) {

    static AddressResponse from(Address address) {
        return new AddressResponse(
                address.id().value().toString(),
                address.owner().type().name(),
                address.owner().id().toString(),
                address.type().name(),
                address.street(),
                address.postalCode(),
                address.city(),
                address.countryCode());
    }
}
