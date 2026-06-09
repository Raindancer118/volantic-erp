package de.volantic.erp.security.domain.model;

import de.volantic.erp.security.AccessScope;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A user mirrored into the ERP, as a domain aggregate. Carries <strong>no password</strong> — only
 * the OIDC subject reference to Authentik; authentication stays entirely with the IdP. The
 * authorization decision is encapsulated here in a domain-pure way (no Spring, no DB).
 */
public final class User {

    private final String oidcSubject;
    private final UserStatus status;
    private final List<RoleAssignment> roleAssignments;

    public User(String oidcSubject, UserStatus status, List<RoleAssignment> roleAssignments) {
        this.oidcSubject = oidcSubject;
        this.status = status == null ? UserStatus.ACTIVE : status;
        this.roleAssignments = List.copyOf(roleAssignments);
    }

    /**
     * Does the user hold the permission in the requested {@link AccessScope}? A disabled
     * ({@link UserStatus#DISABLED}) user holds <em>no</em> permission, regardless of roles.
     */
    public boolean isPermitted(String permission, AccessScope scope) {
        return isActive() && roleAssignments.stream().anyMatch(a -> a.grants(permission, scope));
    }

    /** All permission keys across all roles (empty if the user is disabled). */
    public Set<String> permissionKeys() {
        if (!isActive()) {
            return Set.of();
        }
        return roleAssignments.stream()
                .flatMap(a -> a.role().permissions().stream())
                .map(Permission::key)
                .collect(Collectors.toSet());
    }

    private boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public String oidcSubject() {
        return oidcSubject;
    }
}
