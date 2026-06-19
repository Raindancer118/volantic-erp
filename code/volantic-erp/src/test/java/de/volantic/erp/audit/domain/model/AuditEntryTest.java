package de.volantic.erp.audit.domain.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure domain tests of the {@link AuditEntry} hash chain. */
class AuditEntryTest {

    private final OffsetDateTime when = OffsetDateTime.parse("2026-06-09T10:00:00Z");
    private final UUID entityId = UUID.randomUUID();

    @Test
    void hashIsDeterministicAndRecomputable() {
        AuditEntry entry = AuditEntry.create(1, "evt", "type", entityId, "actor", "payload", when, AuditEntry.GENESIS_HASH);

        assertThat(entry.entryHash()).isEqualTo(entry.recompute(AuditEntry.GENESIS_HASH));
        assertThat(entry.entryHash()).hasSize(64);
    }

    @Test
    void differentContentYieldsDifferentHash() {
        AuditEntry a = AuditEntry.create(1, "evt", "type", entityId, "actor", "payload", when, AuditEntry.GENESIS_HASH);
        AuditEntry b = AuditEntry.create(1, "evt", "type", entityId, "actor", "TAMPERED", when, AuditEntry.GENESIS_HASH);

        assertThat(a.entryHash()).isNotEqualTo(b.entryHash());
    }

    @Test
    void recomputeWithWrongPredecessorBreaks() {
        AuditEntry entry = AuditEntry.create(2, "evt", "type", entityId, "actor", "payload", when, "aaaa");

        assertThat(entry.recompute("bbbb")).isNotEqualTo(entry.entryHash());
    }

    @Test
    void delimiterInFieldsCannotForgeAnotherEntrysCanonicalForm() {
        // With a naive String.join("|", actor, payload) these two entries would share the same canonical
        // string ("a|b|c") and thus the same hash, letting an attacker rewrite a field while keeping a
        // valid chain. Length-prefixing the fields makes the encoding injective, so the hashes differ.
        AuditEntry split = AuditEntry.create(1, "evt", "type", entityId, "a", "b|c", when, AuditEntry.GENESIS_HASH);
        AuditEntry shifted = AuditEntry.create(1, "evt", "type", entityId, "a|b", "c", when, AuditEntry.GENESIS_HASH);

        assertThat(split.entryHash()).isNotEqualTo(shifted.entryHash());
    }
}
