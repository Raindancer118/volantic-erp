package de.volantic.erp.core.entitylink.infrastructure.persistence;

import de.volantic.erp.core.entitylink.EntityLink;
import de.volantic.erp.core.entitylink.EntityRef;
import de.volantic.erp.core.entitylink.application.port.out.EntityLinkStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence IT for the entity-link store against a real PostgreSQL (Testcontainers): idempotent add,
 * outgoing/incoming queries and remove. Skipped without Docker.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EntityLinkStoreAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class EntityLinkStoreIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private EntityLinkStore store;

    private final EntityRef customer = EntityRef.of("crm.customer", UUID.randomUUID());
    private final EntityRef contact = EntityRef.of("crm.contact", UUID.randomUUID());

    @Test
    void addIsIdempotentAndQueryableBothDirections() {
        store.add(new EntityLink(customer, contact, "HAS_CONTACT"));
        store.add(new EntityLink(customer, contact, "HAS_CONTACT")); // duplicate ignored

        assertThat(store.findOutgoing(customer)).singleElement()
                .satisfies(l -> assertThat(l.to()).isEqualTo(contact));
        assertThat(store.findIncoming(contact)).singleElement()
                .satisfies(l -> assertThat(l.from()).isEqualTo(customer));
        assertThat(store.findOutgoing(contact)).isEmpty();
    }

    @Test
    void removeDeletesTheEdge() {
        store.add(new EntityLink(customer, contact, "HAS_CONTACT"));
        store.remove(new EntityLink(customer, contact, "HAS_CONTACT"));

        assertThat(store.findOutgoing(customer)).isEmpty();
    }
}
