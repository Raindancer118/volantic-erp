package de.volantic.erp.audit.application;

import de.volantic.erp.audit.application.port.out.AuditLogStore;
import de.volantic.erp.audit.domain.model.AuditEntry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Orchestration of {@link AuditService}: hash-chained append and integrity verification. */
class AuditServiceTest {

    private final AuditLogStore store = mock(AuditLogStore.class);
    private final AuditService service = new AuditService(store);

    @Test
    void firstEntryStartsFromGenesis() {
        when(store.head()).thenReturn(Optional.empty());

        service.record("evt", "type", UUID.randomUUID(), "payload");

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(store).lockForAppend();
        verify(store).append(captor.capture());
        AuditEntry appended = captor.getValue();
        assertThat(appended.sequence()).isEqualTo(1);
        assertThat(appended.previousHash()).isEqualTo(AuditEntry.GENESIS_HASH);
        assertThat(appended.entryHash()).isEqualTo(appended.recompute(AuditEntry.GENESIS_HASH));
    }

    @Test
    void nextEntryChainsOnHead() {
        AuditEntry head = AuditEntry.create(7, "e", "t", null, "a", "p", OffsetDateTime.now(), AuditEntry.GENESIS_HASH);
        when(store.head()).thenReturn(Optional.of(head));

        service.record("evt", "type", null, "payload");

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(store).append(captor.capture());
        assertThat(captor.getValue().sequence()).isEqualTo(8);
        assertThat(captor.getValue().previousHash()).isEqualTo(head.entryHash());
    }

    @Test
    void verifyDetectsIntactChain() {
        AuditEntry e1 = AuditEntry.create(1, "e", "t", null, "a", "p1", OffsetDateTime.now(), AuditEntry.GENESIS_HASH);
        AuditEntry e2 = AuditEntry.create(2, "e", "t", null, "a", "p2", OffsetDateTime.now(), e1.entryHash());
        when(store.findAllOrdered()).thenReturn(List.of(e1, e2));

        IntegrityResult result = service.verifyIntegrity();

        assertThat(result.intact()).isTrue();
        assertThat(result.entriesChecked()).isEqualTo(2);
    }

    @Test
    void verifyDetectsTampering() {
        AuditEntry e1 = AuditEntry.create(1, "e", "t", null, "a", "p1", OffsetDateTime.now(), AuditEntry.GENESIS_HASH);
        // e2 carries a payload that does not match its stored hash (simulating an edited row)
        AuditEntry tampered = new AuditEntry(2, "e", "t", null, "a", "EDITED",
                OffsetDateTime.now(), e1.entryHash(), e1.recompute(AuditEntry.GENESIS_HASH));
        when(store.findAllOrdered()).thenReturn(List.of(e1, tampered));

        IntegrityResult result = service.verifyIntegrity();

        assertThat(result.intact()).isFalse();
        assertThat(result.brokenAtSequence()).isEqualTo(2);
    }
}
