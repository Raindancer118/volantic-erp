package de.volantic.erp.crm.domain.model;

import java.util.UUID;

/** Identity of a {@link Supplier} — a typed wrapper around a UUIDv7. */
public record SupplierId(UUID value) {

    public SupplierId {
        if (value == null) {
            throw new IllegalArgumentException("supplier id must not be null");
        }
    }
}
