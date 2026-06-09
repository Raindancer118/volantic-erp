package de.volantic.erp.security.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface AppUserJpaRepository extends JpaRepository<AppUserEntity, UUID> {

    /**
     * Lädt den Nutzer samt vollständigem Rollen-/Rechte-Graph in <em>einer</em> Abfrage. Der
     * {@link EntityGraph} verhindert das N+1 auf dem Autorisierungs-Hotpath: ohne ihn wären es
     * 2 + 2·R Abfragen (R = Anzahl Rollen) pro Berechtigungsprüfung.
     */
    @EntityGraph(attributePaths = {"roleAssignments", "roleAssignments.role", "roleAssignments.role.permissions"})
    Optional<AppUserEntity> findWithRolesByOidcSubject(String oidcSubject);

    Optional<AppUserEntity> findByOidcSubject(String oidcSubject);
}
