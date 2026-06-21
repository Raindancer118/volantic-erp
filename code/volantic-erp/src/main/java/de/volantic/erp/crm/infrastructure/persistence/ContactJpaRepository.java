package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.crm.domain.model.PartnerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface ContactJpaRepository extends JpaRepository<ContactEntity, UUID> {

    Page<ContactEntity> findByOwnerTypeAndOwnerId(PartnerType ownerType, UUID ownerId, Pageable pageable);

    /** Ids of contacts matching the (optional) equality filter; null parameters are ignored (ANDed). */
    @Query("""
            select e.id from ContactEntity e
            where (:firstName is null or e.firstName = :firstName)
              and (:lastName is null or e.lastName = :lastName)
              and (:email is null or lower(e.email) = lower(:email))""")
    List<UUID> findIdsByFilter(@Param("firstName") String firstName,
                               @Param("lastName") String lastName,
                               @Param("email") String email);
}
