package de.volantic.erp.changeset.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.changeset.application.port.out.ChangeSetStore;
import de.volantic.erp.changeset.domain.model.ChangeSet;
import de.volantic.erp.changeset.domain.model.ChangeSetMode;
import de.volantic.erp.changeset.domain.model.ChangeSetStatus;
import de.volantic.erp.changeset.domain.model.RecordedOperation;
import de.volantic.erp.core.entitylink.EntityRef;
import de.volantic.erp.core.revision.ChangeOperation;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence IT for the change-set store against a real PostgreSQL (Testcontainers). {@code flush()} +
 * {@code clear()} force a real round-trip, so the JPA mapping (enum→varchar, the operations JSON column,
 * {@code OffsetDateTime}) and the upsert path are exercised end-to-end. Skipped without Docker.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ChangeSetStoreAdapter.class, ChangeSetStoreIT.JacksonConfig.class})
@Testcontainers(disabledWithoutDocker = true)
class ChangeSetStoreIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ChangeSetStore store;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndReconstitutesSessionWithOperations() {
        UUID target = UUID.randomUUID();
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.LIVE);
        session.record(new RecordedOperation(EntityRef.of("crm.customer", target),
                ChangeOperation.UPDATE, "{\"name\":\"old\"}", "{\"name\":\"new\"}", OffsetDateTime.now()));
        store.save(session);

        entityManager.flush();
        entityManager.clear();

        ChangeSet loaded = store.findById(session.id()).orElseThrow();
        assertThat(loaded.actor()).isEqualTo("alice");
        assertThat(loaded.mode()).isEqualTo(ChangeSetMode.LIVE);
        assertThat(loaded.status()).isEqualTo(ChangeSetStatus.OPEN);
        assertThat(loaded.operations()).singleElement().satisfies(op -> {
            assertThat(op.target()).isEqualTo(EntityRef.of("crm.customer", target));
            assertThat(op.operation()).isEqualTo(ChangeOperation.UPDATE);
            assertThat(op.beforeState()).isEqualTo("{\"name\":\"old\"}");
            assertThat(op.payload()).isEqualTo("{\"name\":\"new\"}");
        });
    }

    @Test
    @Transactional
    void savingAnExistingSessionUpdatesItsStatusInPlace() {
        ChangeSet session = ChangeSet.open("bob", ChangeSetMode.DEFERRED);
        store.save(session);
        entityManager.flush();

        session.commit();
        store.save(session);
        entityManager.flush();
        entityManager.clear();

        assertThat(store.findById(session.id()).orElseThrow().status()).isEqualTo(ChangeSetStatus.COMMITTED);
    }

    @Test
    @Transactional
    void findByActorReturnsOnlyThatActorsSessionsNewestFirst() {
        store.save(ChangeSet.open("carol", ChangeSetMode.LIVE));
        store.save(ChangeSet.open("dave", ChangeSetMode.LIVE));
        ChangeSet carolsSecond = ChangeSet.open("carol", ChangeSetMode.DEFERRED);
        store.save(carolsSecond);
        entityManager.flush();
        entityManager.clear();

        var page = store.findByActor("carol", org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allSatisfy(session -> assertThat(session.actor()).isEqualTo("carol"));
        // Newest first: the DEFERRED session was opened last, so it sorts ahead of the first LIVE one.
        assertThat(page.getContent().getFirst().id()).isEqualTo(carolsSecond.id());
    }

    @TestConfiguration
    static class JacksonConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}
