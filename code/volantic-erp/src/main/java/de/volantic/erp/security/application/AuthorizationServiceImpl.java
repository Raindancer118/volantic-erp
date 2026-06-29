package de.volantic.erp.security.application;

import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.AuthorizationService;
import de.volantic.erp.security.application.port.out.OrgUnitHierarchy;
import de.volantic.erp.security.application.port.out.UserDirectory;
import de.volantic.erp.security.domain.model.User;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * RBAC-based implementation of {@link AuthorizationService} (ADR-0004, ADR-0007). Orchestration only:
 * loads the user via the {@link UserDirectory} port and delegates the per-scope decision to the
 * {@code User} domain aggregate. Hierarchy resolution for org-unit scopes lives here (it needs the
 * org tree via {@link OrgUnitHierarchy}); the domain itself stays free of tree knowledge.
 */
@Service
class AuthorizationServiceImpl implements AuthorizationService {

    private final UserDirectory users;
    private final OrgUnitHierarchy hierarchy;

    AuthorizationServiceImpl(UserDirectory users, OrgUnitHierarchy hierarchy) {
        this.users = users;
        this.hierarchy = hierarchy;
    }

    @Override
    public boolean isPermitted(String oidcSubject, String permission) {
        return isPermitted(oidcSubject, permission, AccessScope.GLOBAL);
    }

    @Override
    public boolean isPermitted(String oidcSubject, String permission, AccessScope scope) {
        return users.findByOidcSubject(oidcSubject)
                .map(user -> decide(user, permission, scope))
                .orElse(false);
    }

    private boolean decide(User user, String permission, AccessScope scope) {
        if (scope == null || !scope.isOrgUnit()) {
            // Global or another (exact-match) instance dimension: the domain decides directly.
            return user.isPermitted(permission, scope == null ? AccessScope.GLOBAL : scope);
        }
        // Org-unit scope (ADR-0007): a global grant, or a grant on the unit itself or any ancestor, wins.
        if (user.isPermittedGlobally(permission)) {
            return true;
        }
        for (UUID ancestor : hierarchy.ancestorIds(scope.id())) {
            if (user.isPermitted(permission, AccessScope.orgUnit(ancestor))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> permissionsOf(String oidcSubject) {
        return users.findByOidcSubject(oidcSubject)
                .map(User::permissionKeys)
                .orElseGet(Set::of);
    }

    @Override
    public boolean hasPermissionAnywhere(String oidcSubject, String permission) {
        return permissionsOf(oidcSubject).contains(permission);
    }

    @Override
    public Optional<Set<UUID>> permittedOrgUnits(String oidcSubject, String permission) {
        User user = users.findByOidcSubject(oidcSubject).orElse(null);
        if (user == null) {
            return Optional.of(Set.of()); // unknown user: sees nothing
        }
        if (user.isPermittedGlobally(permission)) {
            return Optional.empty(); // unrestricted — do not filter
        }
        Set<UUID> granted = user.orgUnitsGranting(permission);
        if (granted.isEmpty()) {
            return Optional.of(Set.of()); // holds the permission in no unit
        }
        return Optional.of(hierarchy.descendantIds(granted));
    }
}
