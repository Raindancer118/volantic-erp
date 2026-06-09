package de.volantic.erp.security.domain.model;

import de.volantic.erp.security.AccessScope;

/**
 * Zuweisung einer {@link Role} an einen {@link User}, optional auf einen Daten-{@link AccessScope}
 * eingeschränkt. Eine globale Zuweisung ({@link AccessScope#GLOBAL}) deckt jeden Scope ab; eine
 * scoped Zuweisung deckt nur ihren exakten Scope ab und keine globale Anfrage.
 */
public final class RoleAssignment {

    private final Role role;
    private final AccessScope scope;

    public RoleAssignment(Role role, AccessScope scope) {
        this.role = role;
        this.scope = scope == null ? AccessScope.GLOBAL : scope;
    }

    /** Gewährt diese Zuweisung die Berechtigung im angefragten Scope? */
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

    Role role() {
        return role;
    }
}
