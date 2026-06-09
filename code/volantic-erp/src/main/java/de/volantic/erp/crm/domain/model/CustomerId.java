package de.volantic.erp.crm.domain.model;

import java.util.UUID;

/** Identity of a {@link Customer} — a typed wrapper around a UUIDv7, so ids can't be mixed up across aggregates. */
public record CustomerId(UUID value) {

    public CustomerId {
        if (value == null) {
            throw new IllegalArgumentException("customer id must not be null");
        }
    }
}
