package de.volantic.erp.security.infrastructure.persistence;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serializable, JSON-friendly snapshot of a user's authorization-relevant state, stored in Redis
 * (cache name {@link CacheNames#USER_PERMISSIONS}). Deliberately flat and free of JPA so it can be
 * (de)serialized cheaply; the pure domain {@code User} is rebuilt from it in the adapter.
 */
public record CachedUser(String oidcSubject, String status, List<CachedAssignment> assignments) {

    /** One role assignment, collapsed to its permission keys and optional scope (null = global). */
    public record CachedAssignment(String roleKey, Set<String> permissionKeys, String scopeType, UUID scopeId) {
    }

    /** Builds the snapshot from the fully-fetched JPA aggregate (no additional queries). */
    static CachedUser from(AppUserEntity entity) {
        List<CachedAssignment> assignments = entity.roleAssignments().stream()
                .map(CachedUser::from)
                .toList();
        return new CachedUser(entity.oidcSubject(), entity.status().name(), assignments);
    }

    private static CachedAssignment from(UserRoleEntity assignment) {
        RoleEntity role = assignment.role();
        Set<String> permissionKeys = role.permissions().stream()
                .map(PermissionEntity::key)
                .collect(Collectors.toSet());
        var scope = assignment.scope();
        return scope.isGlobal()
                ? new CachedAssignment(role.key(), permissionKeys, null, null)
                : new CachedAssignment(role.key(), permissionKeys, scope.type(), scope.id());
    }
}
