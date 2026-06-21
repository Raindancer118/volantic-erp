package de.volantic.erp.crm.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface SupplierJpaRepository extends JpaRepository<SupplierEntity, UUID> {

    boolean existsBySupplierNumber(String supplierNumber);

    /** Ids of suppliers matching the (optional) equality filter; null parameters are ignored (ANDed). */
    @Query("""
            select e.id from SupplierEntity e
            where (:name is null or e.name = :name)
              and (:email is null or lower(e.email) = lower(:email))""")
    List<UUID> findIdsByFilter(@Param("name") String name, @Param("email") String email);
}
