package de.volantic.erp.audit.application.port.out;

import de.volantic.erp.audit.domain.model.AuditEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/** Outbound port for the append-only audit log. */
public interface AuditLogStore {

    /** Serializes concurrent appends within the current transaction (DB advisory lock). */
    void lockForAppend();

    /** The current chain head (highest sequence), or empty if the log is empty. */
    Optional<AuditEntry> head();

    void append(AuditEntry entry);

    /** All entries in ascending sequence order — for integrity verification. */
    List<AuditEntry> findAllOrdered();

    /** A page of entries, newest first — for the read endpoint. */
    Page<AuditEntry> findPage(Pageable pageable);
}
