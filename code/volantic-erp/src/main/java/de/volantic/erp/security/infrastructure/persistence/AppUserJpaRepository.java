package de.volantic.erp.security.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface AppUserJpaRepository extends JpaRepository<AppUserEntity, UUID> {

    /**
     * Loads the user together with the full role/permission graph in <em>one</em> query. The
     * {@link EntityGraph} avoids the N+1 on the authorization hot path: without it there would be
     * 2 + 2·R queries (R = number of roles) per permission check.
     */
    @EntityGraph(attributePaths = {"roleAssignments", "roleAssignments.role", "roleAssignments.role.permissions"})
    Optional<AppUserEntity> findWithRolesByOidcSubject(String oidcSubject);

    Optional<AppUserEntity> findByOidcSubject(String oidcSubject);
}
