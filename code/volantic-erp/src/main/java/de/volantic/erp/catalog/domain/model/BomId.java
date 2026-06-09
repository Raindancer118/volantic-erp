package de.volantic.erp.catalog.domain.model;

import java.util.UUID;

/** Identity of a {@link Bom}. */
public record BomId(UUID value) {

    public BomId {
        if (value == null) {
            throw new IllegalArgumentException("bom id must not be null");
        }
    }
}
