package de.volantic.erp.crm.domain.model;

import java.util.UUID;

/**
 * Typed reference to the owning business partner (customer or supplier) of an address or contact.
 * A flat {@code (type, id)} reference rather than a polymorphic JPA association — integrity is kept by
 * the application, matching the DB architecture's flat {@code address}/{@code contact} tables.
 */
public record PartnerRef(PartnerType type, UUID id) {

    public PartnerRef {
        if (type == null) {
            throw new IllegalArgumentException("partner type must not be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("partner id must not be null");
        }
    }

    public static PartnerRef of(PartnerType type, UUID id) {
        return new PartnerRef(type, id);
    }
}
