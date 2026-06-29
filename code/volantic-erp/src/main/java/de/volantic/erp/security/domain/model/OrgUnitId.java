package de.volantic.erp.security.domain.model;

import java.util.UUID;

/** Identity of an {@link OrgUnit} — a typed wrapper around a UUIDv7 so ids can't be mixed up. */
public record OrgUnitId(UUID value) {

    public OrgUnitId {
        if (value == null) {
            throw new IllegalArgumentException("org unit id must not be null");
        }
    }
}
