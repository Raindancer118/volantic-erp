package de.volantic.erp.security;

import de.volantic.erp.security.domain.model.UserStatus;

import java.util.Set;

/**
 * Administrative write API of the security module (ADR-0004 read side: {@link AuthorizationService}).
 * Lets other modules / the admin UI define permissions and roles, mirror users from the IdP, change
 * their status, and assign roles (optionally scoped). Every change that affects a user's
 * authorization also invalidates that user's entry in the authorization cache.
 *
 * <p>Permissions are {@code resource:action} keys (e.g. {@code "hr.salary:read"}); roles bundle them.
 */
public interface SecurityAdmin {

    /** Defines a permission (idempotent). */
    void definePermission(String key, String description);

    /** Creates or updates a role and sets its permission set (the permissions must already exist). */
    void defineRole(String roleKey, String name, Set<String> permissionKeys);

    /** Mirrors a user from the IdP into the ERP if not already present (idempotent). */
    void provisionUser(String oidcSubject, String username, String email);

    /** Enables or disables a user; a disabled user holds no permissions. */
    void setUserStatus(String oidcSubject, UserStatus status);

    /** Assigns a role to a user, optionally restricted to an {@link AccessScope}. */
    void assignRole(String oidcSubject, String roleKey, AccessScope scope);
}
