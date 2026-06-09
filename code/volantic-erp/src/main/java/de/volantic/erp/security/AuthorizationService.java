package de.volantic.erp.security;

import java.util.Set;

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

    /** Does the user hold the permission in the requested {@link AccessScope}? */
    boolean isPermitted(String oidcSubject, String permission, AccessScope scope);

    /** All permission keys of the user (across all roles, regardless of scope). */
    Set<String> permissionsOf(String oidcSubject);
}
