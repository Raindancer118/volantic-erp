package de.volantic.erp.core.entitylink.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface EntityLinkJpaRepository extends JpaRepository<EntityLinkEntity, UUID> {

    List<EntityLinkEntity> findByFromTypeAndFromId(String fromType, UUID fromId);

    List<EntityLinkEntity> findByToTypeAndToId(String toType, UUID toId);

    Optional<EntityLinkEntity> findByFromTypeAndFromIdAndToTypeAndToIdAndLinkType(
            String fromType, UUID fromId, String toType, UUID toId, String linkType);
}
