package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.crm.domain.model.PartnerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ContactJpaRepository extends JpaRepository<ContactEntity, UUID> {

    Page<ContactEntity> findByOwnerTypeAndOwnerId(PartnerType ownerType, UUID ownerId, Pageable pageable);
}
