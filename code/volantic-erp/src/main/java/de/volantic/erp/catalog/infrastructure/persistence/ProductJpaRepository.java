package de.volantic.erp.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {

    boolean existsBySku(String sku);
}
