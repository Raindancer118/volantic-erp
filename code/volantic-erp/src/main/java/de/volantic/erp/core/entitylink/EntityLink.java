package de.volantic.erp.core.entitylink;

/**
 * A directed, typed edge between two entities: {@code from --linkType--> to} (e.g. a customer
 * {@code HAS_CONTACT} a contact). Immutable value object exposed to other modules for 360° queries.
 */
public record EntityLink(EntityRef from, EntityRef to, String linkType) {

    public EntityLink {
        if (from == null || to == null) {
            throw new IllegalArgumentException("link endpoints must not be null");
        }
        if (linkType == null || linkType.isBlank()) {
            throw new IllegalArgumentException("linkType must not be blank");
        }
    }
}
