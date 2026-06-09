package de.volantic.erp.audit.api;

import de.volantic.erp.audit.domain.model.AuditEntry;

/** Read view of an audit entry (REST v1). Exposes the chain hashes so integrity can be checked externally. */
public record AuditEntryResponse(
        long sequence, String eventType, String entityType, String entityId,
        String actor, String payload, String occurredAt, String previousHash, String entryHash) {

    static AuditEntryResponse from(AuditEntry entry) {
        return new AuditEntryResponse(
                entry.sequence(),
                entry.eventType(),
                entry.entityType(),
                entry.entityId() == null ? null : entry.entityId().toString(),
                entry.actor(),
                entry.payload(),
                entry.occurredAt().toString(),
                entry.previousHash(),
                entry.entryHash());
    }
}
