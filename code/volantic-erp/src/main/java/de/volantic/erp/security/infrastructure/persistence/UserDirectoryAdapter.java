package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.application.port.out.UserDirectory;
import de.volantic.erp.security.domain.model.Permission;
import de.volantic.erp.security.domain.model.Role;
import de.volantic.erp.security.domain.model.RoleAssignment;
import de.volantic.erp.security.domain.model.User;
import de.volantic.erp.security.domain.model.UserStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Outbound adapter for {@link UserDirectory}: reads the (cached) {@link CachedUser} snapshot via
 * {@link UserGraphCache} and maps it onto the pure domain {@code User}. This keeps the domain free of
 * persistence and the authorization hot path free of both N+1 (single-query load) and repeated DB
 * round trips (Redis cache).
 *
 * <p>The transaction is needed only on a cache miss, when {@link UserGraphCache#load} runs the
 * database query within it; on a cache hit it is an empty read-only transaction.
 */
@Component
class UserDirectoryAdapter implements UserDirectory {

    private final UserGraphCache cache;

    UserDirectoryAdapter(UserGraphCache cache) {
        this.cache = cache;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByOidcSubject(String oidcSubject) {
        return Optional.ofNullable(cache.load(oidcSubject)).map(UserDirectoryAdapter::toDomain);
    }

    private static User toDomain(CachedUser snapshot) {
        List<RoleAssignment> assignments = snapshot.assignments().stream()
                .map(UserDirectoryAdapter::toDomain)
                .toList();
        return new User(snapshot.oidcSubject(), UserStatus.valueOf(snapshot.status()), assignments);
    }

    private static RoleAssignment toDomain(CachedUser.CachedAssignment assignment) {
        Set<Permission> permissions = assignment.permissionKeys().stream()
                .map(Permission::new)
                .collect(Collectors.toSet());
        Role role = new Role(assignment.roleKey(), permissions);
        AccessScope scope = assignment.scopeType() == null
                ? AccessScope.GLOBAL
                : AccessScope.of(assignment.scopeType(), assignment.scopeId());
        return new RoleAssignment(role, scope);
    }
}
