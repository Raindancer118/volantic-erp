package de.volantic.erp.crm.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface CustomerJpaRepository extends JpaRepository<CustomerEntity, UUID> {

    boolean existsByCustomerNumber(String customerNumber);

    /** Ids of customers matching the (optional) equality filter; null parameters are ignored (ANDed). */
    @Query("""
            select e.id from CustomerEntity e
            where (:name is null or e.name = :name)
              and (:email is null or lower(e.email) = lower(:email))""")
    List<UUID> findIdsByFilter(@Param("name") String name, @Param("email") String email);
}
