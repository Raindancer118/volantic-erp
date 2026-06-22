package de.volantic.erp.security.domain.model;

import de.volantic.erp.security.AccessScope;

/**
 * Assignment of a {@link Role} to a {@link User}, optionally restricted to a data {@link AccessScope}.
 * A global assignment ({@link AccessScope#GLOBAL}) covers every scope; a scoped assignment covers
 * only its exact scope and no global request.
 */
public final class RoleAssignment {

    private final Role role;
    private final AccessScope scope;

    public RoleAssignment(Role role, AccessScope scope) {
        this.role = role;
        this.scope = scope == null ? AccessScope.GLOBAL : scope;
    }

    /** Does this assignment grant the permission in the requested scope? */
    public boolean grants(String permission, AccessScope requested) {
        return covers(requested) && role.grants(permission);
    }

    boolean covers(AccessScope requested) {
        if (scope.isGlobal()) {
            return true;
        }
        if (requested == null || requested.isGlobal()) {
            return false;
        }
        return scope.equals(requested);
    }

    /** Is this a global (unrestricted) assignment? */
    boolean isGlobal() {
        return scope.isGlobal();
    }

    /**
     * The org-unit id this assignment is scoped to, but only when it grants the given permission via an
     * {@code ORG_UNIT} scope; {@code null} for a global assignment, a non-org-unit scope, or a role that
     * does not grant the permission. Used to collect the units a user may act in (hierarchy is expanded
     * to descendants in the authorization layer, not here).
     */
    java.util.UUID orgUnitGranting(String permission) {
        if (scope.isOrgUnit() && role.grants(permission)) {
            return scope.id();
        }
        return null;
    }

    Role role() {
        return role;
    }
}
