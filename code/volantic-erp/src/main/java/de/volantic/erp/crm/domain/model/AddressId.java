package de.volantic.erp.crm.domain.model;

import java.util.UUID;

/** Identity of an {@link Address}. */
public record AddressId(UUID value) {

    public AddressId {
        if (value == null) {
            throw new IllegalArgumentException("address id must not be null");
        }
    }
}
