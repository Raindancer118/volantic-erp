package de.volantic.erp.crm.domain.model;

import java.util.UUID;

/** Identity of a {@link Contact}. */
public record ContactId(UUID value) {

    public ContactId {
        if (value == null) {
            throw new IllegalArgumentException("contact id must not be null");
        }
    }
}
