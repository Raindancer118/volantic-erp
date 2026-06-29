package de.volantic.erp.crm.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

interface SupplierJpaRepository extends JpaRepository<SupplierEntity, UUID> {

    boolean existsBySupplierNumber(String supplierNumber);

    @Query("select e from SupplierEntity e where e.orgUnitId in :ids")
    Page<SupplierEntity> findAllByOrgUnitIdIn(@Param("ids") Set<UUID> ids, Pageable pageable);

    /** Ids of suppliers matching the (optional) equality filter; null parameters are ignored (ANDed). */
    @Query("""
            select e.id from SupplierEntity e
            where (:name is null or e.name = :name)
              and (:email is null or lower(e.email) = lower(:email))""")
    List<UUID> findIdsByFilter(@Param("name") String name, @Param("email") String email);
}
