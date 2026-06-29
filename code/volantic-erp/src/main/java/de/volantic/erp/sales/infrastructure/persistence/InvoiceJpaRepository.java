package de.volantic.erp.sales.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.util.UUID;

interface InvoiceJpaRepository extends JpaRepository<InvoiceEntity, UUID> {

    @Query("select e from InvoiceEntity e where e.orgUnitId in :ids")
    Page<InvoiceEntity> findAllByOrgUnitIdIn(@Param("ids") Set<UUID> ids, Pageable pageable);
}
