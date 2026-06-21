package de.volantic.erp.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {

    boolean existsBySku(String sku);

    /** Ids of products matching the (optional) equality filter; null parameters are ignored (ANDed). */
    @Query("""
            select e.id from ProductEntity e
            where (:name is null or e.name = :name)
              and (:currency is null or e.priceCurrency = :currency)""")
    List<UUID> findIdsByFilter(@Param("name") String name, @Param("currency") String currency);
}
