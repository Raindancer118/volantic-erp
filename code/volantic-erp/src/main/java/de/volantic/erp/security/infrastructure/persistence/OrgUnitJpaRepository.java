package de.volantic.erp.security.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface OrgUnitJpaRepository extends JpaRepository<OrgUnitEntity, UUID> {

    boolean existsByCode(String code);

    /** All (id, parentId) edges of the tree — the minimal projection the hierarchy cache needs. */
    @Query("select e.id as id, e.parentId as parentId from OrgUnitEntity e")
    List<OrgEdgeView> findAllEdges();

    /** Closed projection for {@link #findAllEdges()}; {@code parentId} is null for a root unit. */
    interface OrgEdgeView {
        UUID getId();

        UUID getParentId();
    }
}
