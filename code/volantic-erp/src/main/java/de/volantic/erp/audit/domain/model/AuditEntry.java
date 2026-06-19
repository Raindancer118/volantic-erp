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
        // Length-prefixed canonical form: each field is encoded as "<byteLength>:<field>". This makes the
        // concatenation injective, so untrusted inputs (actor, payload) cannot forge field boundaries by
        // smuggling the delimiter — unlike a plain String.join("|", ...). The byte length is unambiguous,
        // so any change to a field changes its length prefix and/or content and thus the hash.
        StringBuilder canonical = new StringBuilder();
        appendField(canonical, previousHash);
        appendField(canonical, Long.toString(sequence));
        appendField(canonical, nullSafe(eventType));
        appendField(canonical, nullSafe(entityType));
        appendField(canonical, entityId == null ? "" : entityId.toString());
        appendField(canonical, nullSafe(actor));
        appendField(canonical, nullSafe(payload));
        appendField(canonical, occurredAt == null ? "" : occurredAt.toInstant().toString());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static void appendField(StringBuilder target, String value) {
        target.append(value.getBytes(StandardCharsets.UTF_8).length).append(':').append(value).append('|');
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
