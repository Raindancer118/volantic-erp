package de.volantic.erp.core.entitylink;

import java.util.UUID;

/**
 * Typed reference to any entity in the system, e.g. {@code EntityRef.of("crm.customer", id)}. The
 * {@code type} is the module-qualified entity name; this is how the {@link EntityLinkRegistry} stays
 * generic and module-agnostic (the 360° foundation, DB architecture §5.3).
 */
public record EntityRef(String type, UUID id) {

    public EntityRef {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("entity type must not be blank");
        }
        if (id == null) {
            throw new IllegalArgumentException("entity id must not be null");
        }
    }

    public static EntityRef of(String type, UUID id) {
        return new EntityRef(type, id);
    }
}
