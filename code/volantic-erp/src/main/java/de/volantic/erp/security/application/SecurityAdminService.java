package de.volantic.erp.security.application;

import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.SecurityAdmin;
import de.volantic.erp.security.application.port.out.SecurityWriteStore;
import de.volantic.erp.security.domain.model.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Application service for the security write side ({@link SecurityAdmin}). Orchestrates the use cases
 * (idempotent provisioning, transaction boundaries) and delegates persistence + cache eviction to the
 * {@link SecurityWriteStore} outbound port. No persistence or cache details leak into this layer.
 */
@Service
class SecurityAdminService implements SecurityAdmin {

    private final SecurityWriteStore store;

    SecurityAdminService(SecurityWriteStore store) {
        this.store = store;
    }

    @Override
    @Transactional
    public void definePermission(String key, String description) {
        store.upsertPermission(key, description);
    }

    @Override
    @Transactional
    public void defineRole(String roleKey, String name, Set<String> permissionKeys) {
        store.upsertRole(roleKey, name, permissionKeys);
    }

    @Override
    @Transactional
    public void provisionUser(String oidcSubject, String username, String email) {
        if (!store.userExists(oidcSubject)) {
            store.createUser(oidcSubject, username, email);
        }
    }

    @Override
    @Transactional
    public void setUserStatus(String oidcSubject, UserStatus status) {
        store.setUserStatus(oidcSubject, status);
    }

    @Override
    @Transactional
    public void assignRole(String oidcSubject, String roleKey, AccessScope scope) {
        store.assignRole(oidcSubject, roleKey, scope);
    }
}
