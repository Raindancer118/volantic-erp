package de.volantic.erp.security.application.port.out;

import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.domain.model.UserStatus;

import java.util.Set;

/**
 * Outbound port for the security write side: persistence of users, roles and permissions plus the
 * resulting cache invalidation. The implementation (infrastructure) owns both the JPA writes and the
 * eviction of the authorization cache, so the application layer stays free of infrastructure concerns.
 */
public interface SecurityWriteStore {

    void upsertPermission(String key, String description);

    /** Creates or updates the role and replaces its permission set. Permissions must already exist. */
    void upsertRole(String roleKey, String name, Set<String> permissionKeys);

    boolean userExists(String oidcSubject);

    void createUser(String oidcSubject, String username, String email);

    void setUserStatus(String oidcSubject, UserStatus status);

    void assignRole(String oidcSubject, String roleKey, AccessScope scope);
}
