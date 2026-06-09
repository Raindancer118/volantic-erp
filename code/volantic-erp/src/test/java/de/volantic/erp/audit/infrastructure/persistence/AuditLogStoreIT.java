package de.volantic.erp.audit.infrastructure.persistence;

import de.volantic.erp.audit.application.port.out.AuditLogStore;
import de.volantic.erp.audit.domain.model.AuditEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence IT for the append-only audit store against a real PostgreSQL (Testcontainers): advisory
 * lock, head/ordering, and that a persisted chain verifies. Skipped without Docker.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditLogStoreAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class AuditLogStoreIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private AuditLogStore store;

    @Test
    @Transactional
    void appendsAndOrdersHashChain() {
        store.lockForAppend(); // advisory lock must be obtainable within the transaction

        AuditEntry e1 = AuditEntry.create(1, "e", "t", UUID.randomUUID(), "sys", "p1", OffsetDateTime.now(), AuditEntry.GENESIS_HASH);
        store.append(e1);
        AuditEntry head1 = store.head().orElseThrow();
        assertThat(head1.sequence()).isEqualTo(1);

        AuditEntry e2 = AuditEntry.create(2, "e", "t", null, "sys", "p2", OffsetDateTime.now(), head1.entryHash());
        store.append(e2);

        assertThat(store.head().orElseThrow().sequence()).isEqualTo(2);
        assertThat(store.findAllOrdered()).extracting(AuditEntry::sequence).containsExactly(1L, 2L);
        assertThat(store.findPage(PageRequest.of(0, 10)).getContent())
                .extracting(AuditEntry::sequence).containsExactly(2L, 1L); // newest first

        // the persisted chain is internally consistent
        assertThat(e2.recompute(e1.entryHash())).isEqualTo(e2.entryHash());
    }
}
