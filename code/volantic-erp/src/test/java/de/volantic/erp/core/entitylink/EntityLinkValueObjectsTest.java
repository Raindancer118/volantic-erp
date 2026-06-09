package de.volantic.erp.core.entitylink;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Validation of the public entity-link value objects. */
class EntityLinkValueObjectsTest {

    private final UUID id = UUID.randomUUID();

    @Test
    void entityRefRejectsBlankTypeOrNullId() {
        assertThatThrownBy(() -> EntityRef.of(" ", id)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EntityRef.of("crm.customer", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void entityLinkRejectsNullEndpointsOrBlankType() {
        EntityRef ref = EntityRef.of("crm.customer", id);
        assertThatThrownBy(() -> new EntityLink(ref, null, "HAS_CONTACT")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntityLink(ref, ref, " ")).isInstanceOf(IllegalArgumentException.class);
    }
}
