package de.volantic.erp.audit.application.port.out;

import de.volantic.erp.audit.domain.model.AuditEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/** Outbound port for the append-only audit log. */
public interface AuditLogStore {

    /** Serializes concurrent appends within the current transaction (DB advisory lock). */
    void lockForAppend();

    /** The current chain head (highest sequence), or empty if the log is empty. */
    Optional<AuditEntry> head();

    void append(AuditEntry entry);

    /**
     * A page of entries in ascending sequence order — for streamed integrity verification. Paging keeps
     * verification from loading the (unbounded, append-only) log into memory at once.
     */
    Page<AuditEntry> findAscending(Pageable pageable);

    /** A page of entries, newest first — for the read endpoint. */
    Page<AuditEntry> findPage(Pageable pageable);
}
