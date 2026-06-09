package de.volantic.erp.audit.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {

    Optional<AuditLogEntity> findTopByOrderBySequenceDesc();

    List<AuditLogEntity> findAllByOrderBySequenceAsc();

    Page<AuditLogEntity> findAllByOrderBySequenceDesc(Pageable pageable);
}
