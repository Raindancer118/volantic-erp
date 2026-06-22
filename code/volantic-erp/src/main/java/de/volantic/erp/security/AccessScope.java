package de.volantic.erp.security;

import java.util.UUID;

/**
 * Data scope of an authorization request or grant (instance/area level).
 *
 * <p>{@link #GLOBAL} means "unrestricted". A concrete scope binds a permission to an instance/area
 * (e.g. {@code AccessScope.of("DEPT", departmentId)}). A globally granted permission covers every
 * requested scope; a scoped grant covers only its own exact scope — and in particular no global
 * request.
 */
public record AccessScope(String type, UUID id) {

    public static final AccessScope GLOBAL = new AccessScope("GLOBAL", null);

    /**
     * Scope type for an organizational unit (ADR-0007). The hierarchical scope dimension: a grant on a
     * unit covers that unit and — resolved in the authorization layer — its descendant units.
     */
    public static final String ORG_UNIT = "ORG_UNIT";

    public static AccessScope of(String type, UUID id) {
        return new AccessScope(type, id);
    }

    /** A scope bound to an organizational unit. */
    public static AccessScope orgUnit(UUID orgUnitId) {
        return new AccessScope(ORG_UNIT, orgUnitId);
    }

    public boolean isGlobal() {
        return id == null;
    }

    /** Is this an organizational-unit scope (as opposed to global or another instance dimension)? */
    public boolean isOrgUnit() {
        return ORG_UNIT.equals(type) && id != null;
    }
}
