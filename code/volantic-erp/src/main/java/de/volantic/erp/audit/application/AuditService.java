package de.volantic.erp.audit.application;

import de.volantic.erp.audit.AuditTrail;
import de.volantic.erp.audit.application.port.out.AuditLogStore;
import de.volantic.erp.audit.domain.model.AuditEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Implements {@link AuditTrail} and the audit read/verify use cases. Appends are serialized via the
 * store's advisory lock so the hash chain is built consistently: read the head, compute the next
 * entry's hash from it, append. The acting OIDC subject is taken from the security context.
 */
@Service
public class AuditService implements AuditTrail {

    /** Entries verified per round trip — bounds the memory footprint of a full-chain verification. */
    private static final int VERIFY_BATCH_SIZE = 500;

    private final AuditLogStore store;

    AuditService(AuditLogStore store) {
        this.store = store;
    }

    @Override
    @Transactional
    public void record(String eventType, String entityType, UUID entityId, String payload) {
        store.lockForAppend();
        AuditEntry head = store.head().orElse(null);
        long sequence = head == null ? 1L : head.sequence() + 1;
        String previousHash = head == null ? AuditEntry.GENESIS_HASH : head.entryHash();
        store.append(AuditEntry.create(
                sequence, eventType, entityType, entityId, currentActor(), payload,
                OffsetDateTime.now(ZoneOffset.UTC), previousHash));
    }

    @Transactional(readOnly = true)
    public Page<AuditEntry> entries(Pageable pageable) {
        return store.findPage(pageable);
    }

    /**
     * Recomputes the whole chain and reports the first tampered entry, if any. The append-only log is
     * unbounded, so it is verified in {@value #VERIFY_BATCH_SIZE}-entry pages rather than loaded into a
     * single in-memory list — otherwise a large trail would exhaust the heap (OOM).
     */
    @Transactional(readOnly = true)
    public IntegrityResult verifyIntegrity() {
        String previousHash = AuditEntry.GENESIS_HASH;
        long checked = 0;
        Page<AuditEntry> page;
        int pageNumber = 0;
        do {
            page = store.findAscending(PageRequest.of(pageNumber, VERIFY_BATCH_SIZE));
            for (AuditEntry entry : page) {
                checked++;
                if (!entry.entryHash().equals(entry.recompute(previousHash))) {
                    return IntegrityResult.broken(checked, entry.sequence());
                }
                previousHash = entry.entryHash();
            }
            pageNumber++;
        } while (page.hasNext());
        return IntegrityResult.ok(checked);
    }

    private static String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        return authentication.getName();
    }
}
