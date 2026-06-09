package de.volantic.erp.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface BomJpaRepository extends JpaRepository<BomEntity, UUID> {

    boolean existsByProductIdAndBomVersion(UUID productId, int bomVersion);

    List<BomEntity> findByProductIdOrderByBomVersionAsc(UUID productId);
}
