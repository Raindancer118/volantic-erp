package de.volantic.erp.changeset.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ChangeSetJpaRepository extends JpaRepository<ChangeSetEntity, UUID> {
}
