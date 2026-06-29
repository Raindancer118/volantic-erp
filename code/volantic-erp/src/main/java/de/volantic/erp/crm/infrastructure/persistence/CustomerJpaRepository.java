package de.volantic.erp.crm.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

interface CustomerJpaRepository extends JpaRepository<CustomerEntity, UUID> {

    boolean existsByCustomerNumber(String customerNumber);

    @Query("select e from CustomerEntity e where e.orgUnitId in :ids")
    Page<CustomerEntity> findAllByOrgUnitIdIn(@Param("ids") Set<UUID> ids, Pageable pageable);

    /** Ids of customers matching the (optional) equality filter; null parameters are ignored (ANDed). */
    @Query("""
            select e.id from CustomerEntity e
            where (:name is null or e.name = :name)
              and (:email is null or lower(e.email) = lower(:email))""")
    List<UUID> findIdsByFilter(@Param("name") String name, @Param("email") String email);
}
