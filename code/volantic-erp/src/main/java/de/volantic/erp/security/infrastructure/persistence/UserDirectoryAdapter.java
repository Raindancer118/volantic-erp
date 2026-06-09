package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.security.application.port.out.UserDirectory;
import de.volantic.erp.security.domain.model.Permission;
import de.volantic.erp.security.domain.model.Role;
import de.volantic.erp.security.domain.model.RoleAssignment;
import de.volantic.erp.security.domain.model.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Outbound adapter for {@link UserDirectory}: loads the user graph in one query
 * ({@code findWithRolesByOidcSubject}) and maps the JPA aggregate onto the pure domain. This keeps the
 * domain free of persistence and the authorization hot path free of N+1.
 */
@Component
class UserDirectoryAdapter implements UserDirectory {

    private final AppUserJpaRepository users;

    UserDirectoryAdapter(AppUserJpaRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByOidcSubject(String oidcSubject) {
        return users.findWithRolesByOidcSubject(oidcSubject).map(UserDirectoryAdapter::toDomain);
    }

    private static User toDomain(AppUserEntity entity) {
        List<RoleAssignment> assignments = entity.roleAssignments().stream()
                .map(a -> new RoleAssignment(toDomain(a.role()), a.scope()))
                .toList();
        return new User(entity.oidcSubject(), entity.status(), assignments);
    }

    private static Role toDomain(RoleEntity role) {
        var permissions = role.permissions().stream()
                .map(p -> new Permission(p.key()))
                .collect(Collectors.toSet());
        return new Role(role.key(), permissions);
    }
}
