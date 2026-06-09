package de.volantic.erp.audit;

import java.util.UUID;

/**
 * Public API of the tamper-evident audit trail (GoBD/NIS2). Other modules record security- and
 * document-relevant changes here; entries are immutable and hash-chained. {@code payload} is a free
 * description or serialized snapshot of the change (never secrets).
 */
public interface AuditTrail {

    void record(String eventType, String entityType, UUID entityId, String payload);
}
