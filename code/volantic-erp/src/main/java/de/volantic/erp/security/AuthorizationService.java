package de.volantic.erp.security;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Central authorization port (ADR-0004). <strong>Every</strong> access decision in the system goes
 * through this interface — at the domain/service boundary, not only in the UI.
 *
 * <p>Behind it today: RBAC (role → permission) plus field and instance/scope level. As policy
 * complexity grows (HR, accounting), a policy engine is pulled behind the same port without any
 * caller changing. The interface may grow additively but must never be narrowed.
 *
 * <p>Permissions are {@code resource:action} strings, e.g. {@code "hr.salary:read"} or
 * {@code "sales.order:approve"}. Sensitive fields get their own permission.
 */
public interface AuthorizationService {

    /** Does the user hold the permission globally (unrestricted)? */
    boolean isPermitted(String oidcSubject, String permission);

    /**
     * Does the user hold the permission in the requested {@link AccessScope}? For an
     * {@link AccessScope#ORG_UNIT} scope the decision is hierarchical (ADR-0007): a grant on the unit
     * itself, on any of its ancestors, or globally is sufficient.
     */
    boolean isPermitted(String oidcSubject, String permission, AccessScope scope);

    /** All permission keys of the user (across all roles, regardless of scope). */
    Set<String> permissionsOf(String oidcSubject);

    /**
     * Does the user hold the permission in <em>any</em> scope (global or any org unit)? The gate for
     * list/search endpoints, which then filter their results to {@link #permittedOrgUnits} (ADR-0007).
     */
    boolean hasPermissionAnywhere(String oidcSubject, String permission);

    /**
     * The organizational units whose records the user may see for the permission, for list filtering
     * (ADR-0007). {@link Optional#empty()} means <em>unrestricted</em> (the user holds the permission
     * globally) — do not filter. A present set is the exact, already descendant-expanded set of unit ids
     * to filter to ({@code WHERE org_unit_id IN (…)}); an empty set means "no units" (filter to nothing).
     */
    Optional<Set<UUID>> permittedOrgUnits(String oidcSubject, String permission);
}
