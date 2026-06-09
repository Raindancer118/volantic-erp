package de.volantic.erp.audit.infrastructure.persistence;

import de.volantic.erp.audit.domain.model.AuditEntry;
import de.volantic.erp.core.AbstractEntity;
import de.volantic.erp.core.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** JPA representation of one hash-chained audit entry. Table {@code audit.audit_log} (append-only). */
@Entity
@Table(schema = "audit", name = "audit_log")
class AuditLogEntity extends AbstractEntity {

    @Column(name = "sequence", nullable = false, unique = true, updatable = false)
    private long sequence;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @Column(name = "entity_type", updatable = false)
    private String entityType;

    @Column(name = "entity_id", updatable = false)
    private UUID entityId;

    @Column(name = "actor", nullable = false, updatable = false)
    private String actor;

    @Column(name = "payload", updatable = false)
    private String payload;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "previous_hash", nullable = false, updatable = false)
    private String previousHash;

    @Column(name = "entry_hash", nullable = false, unique = true, updatable = false)
    private String entryHash;

    protected AuditLogEntity() {
    }

    private AuditLogEntity(AuditEntry entry) {
        super(UuidV7.randomUuid());
        this.sequence = entry.sequence();
        this.eventType = entry.eventType();
        this.entityType = entry.entityType();
        this.entityId = entry.entityId();
        this.actor = entry.actor();
        this.payload = entry.payload();
        this.occurredAt = entry.occurredAt();
        this.previousHash = entry.previousHash();
        this.entryHash = entry.entryHash();
    }

    static AuditLogEntity from(AuditEntry entry) {
        return new AuditLogEntity(entry);
    }

    AuditEntry toDomain() {
        return new AuditEntry(sequence, eventType, entityType, entityId, actor, payload, occurredAt, previousHash, entryHash);
    }
}
