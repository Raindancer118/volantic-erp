package de.volantic.erp.audit.infrastructure.persistence;

import de.volantic.erp.audit.application.port.out.AuditLogStore;
import de.volantic.erp.audit.domain.model.AuditEntry;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Outbound adapter for {@link AuditLogStore}. Appends are serialized with a PostgreSQL transaction-level
 * advisory lock so the hash chain is built without interleaving; the lock releases automatically at
 * commit. Maps between the domain {@link AuditEntry} and the JPA entity.
 */
@Component
class AuditLogStoreAdapter implements AuditLogStore {

    /** Fixed advisory-lock key dedicated to audit appends. */
    private static final long ADVISORY_LOCK_KEY = 0x4155_4449_544CL; // "AUDITL"

    private final AuditLogJpaRepository jpa;
    private final EntityManager entityManager;

    AuditLogStoreAdapter(AuditLogJpaRepository jpa, EntityManager entityManager) {
        this.jpa = jpa;
        this.entityManager = entityManager;
    }

    @Override
    public void lockForAppend() {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:key)")
                .setParameter("key", ADVISORY_LOCK_KEY)
                .getSingleResult();
    }

    @Override
    public Optional<AuditEntry> head() {
        return jpa.findTopByOrderBySequenceDesc().map(AuditLogEntity::toDomain);
    }

    @Override
    public void append(AuditEntry entry) {
        jpa.save(AuditLogEntity.from(entry));
    }

    @Override
    public List<AuditEntry> findAllOrdered() {
        return jpa.findAllByOrderBySequenceAsc().stream().map(AuditLogEntity::toDomain).toList();
    }

    @Override
    public Page<AuditEntry> findPage(Pageable pageable) {
        return jpa.findAllByOrderBySequenceDesc(pageable).map(AuditLogEntity::toDomain);
    }
}
