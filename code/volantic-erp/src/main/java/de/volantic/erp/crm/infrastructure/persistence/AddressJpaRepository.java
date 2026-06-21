package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.crm.domain.model.AddressType;
import de.volantic.erp.crm.domain.model.PartnerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface AddressJpaRepository extends JpaRepository<AddressEntity, UUID> {

    Page<AddressEntity> findByOwnerTypeAndOwnerId(PartnerType ownerType, UUID ownerId, Pageable pageable);

    /** Ids of addresses matching the (optional) equality filter; null parameters are ignored (ANDed). */
    @Query("""
            select e.id from AddressEntity e
            where (:type is null or e.type = :type)
              and (:city is null or e.city = :city)
              and (:postalCode is null or e.postalCode = :postalCode)
              and (:countryCode is null or e.countryCode = :countryCode)""")
    List<UUID> findIdsByFilter(@Param("type") AddressType type,
                               @Param("city") String city,
                               @Param("postalCode") String postalCode,
                               @Param("countryCode") String countryCode);
}
