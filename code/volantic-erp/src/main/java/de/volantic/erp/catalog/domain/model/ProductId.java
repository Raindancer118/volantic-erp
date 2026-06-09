package de.volantic.erp.catalog.domain.model;

import java.util.UUID;

/** Identity of a {@link Product}. */
public record ProductId(UUID value) {

    public ProductId {
        if (value == null) {
            throw new IllegalArgumentException("product id must not be null");
        }
    }
}
