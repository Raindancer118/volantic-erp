package de.volantic.erp.crm.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SupplierJpaRepository extends JpaRepository<SupplierEntity, UUID> {

    boolean existsBySupplierNumber(String supplierNumber);
}
