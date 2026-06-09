package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.crm.domain.model.PartnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AddressJpaRepository extends JpaRepository<AddressEntity, UUID> {

    List<AddressEntity> findByOwnerTypeAndOwnerId(PartnerType ownerType, UUID ownerId);
}
