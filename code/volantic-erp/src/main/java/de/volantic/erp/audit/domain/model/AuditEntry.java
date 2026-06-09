package de.volantic.erp.audit.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * One immutable, hash-chained audit record. The {@link #entryHash()} is the SHA-256 over the previous
 * entry's hash and this entry's canonical content, so any change to a past entry breaks every
 * subsequent hash and is detectable (GoBD/NIS2 tamper evidence).
 */
public record AuditEntry(
        long sequence,
        String eventType,
        String entityType,
        UUID entityId,
        String actor,
        String payload,
        OffsetDateTime occurredAt,
        String previousHash,
        String entryHash) {

    /** Hash used as the predecessor of the very first entry. */
    public static final String GENESIS_HASH = "0".repeat(64);

    /** Creates an entry, computing its hash from the chain head ({@code previousHash}). */
    public static AuditEntry create(long sequence, String eventType, String entityType, UUID entityId,
                                    String actor, String payload, OffsetDateTime occurredAt, String previousHash) {
        String hash = hash(sequence, eventType, entityType, entityId, actor, payload, occurredAt, previousHash);
        return new AuditEntry(sequence, eventType, entityType, entityId, actor, payload, occurredAt, previousHash, hash);
    }

    /** Recomputes the hash for the given predecessor — used to verify the chain. */
    public String recompute(String predecessorHash) {
        return hash(sequence, eventType, entityType, entityId, actor, payload, occurredAt, predecessorHash);
    }

    private static String hash(long sequence, String eventType, String entityType, UUID entityId,
                               String actor, String payload, OffsetDateTime occurredAt, String previousHash) {
        String canonical = String.join("|",
                previousHash,
                Long.toString(sequence),
                nullSafe(eventType),
                nullSafe(entityType),
                entityId == null ? "" : entityId.toString(),
                nullSafe(actor),
                nullSafe(payload),
                occurredAt == null ? "" : occurredAt.toInstant().toString());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
