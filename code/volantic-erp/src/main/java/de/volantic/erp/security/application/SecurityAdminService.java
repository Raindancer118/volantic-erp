package de.volantic.erp.security.application;

import de.volantic.erp.audit.AuditTrail;
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
 * {@link SecurityWriteStore} outbound port. Access-control-relevant changes are recorded in the
 * tamper-evident {@link AuditTrail} (GoBD/NIS2) within the same transaction.
 */
@Service
class SecurityAdminService implements SecurityAdmin {

    private final SecurityWriteStore store;
    private final AuditTrail auditTrail;

    SecurityAdminService(SecurityWriteStore store, AuditTrail auditTrail) {
        this.store = store;
        this.auditTrail = auditTrail;
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
        auditTrail.record("security.role-defined", "security.role", null, roleKey + " -> " + permissionKeys);
    }

    @Override
    @Transactional
    public void provisionUser(String oidcSubject, String username, String email) {
        if (!store.userExists(oidcSubject)) {
            store.createUser(oidcSubject, username, email);
            auditTrail.record("security.user-provisioned", "security.user", null, oidcSubject);
        }
    }

    @Override
    @Transactional
    public void setUserStatus(String oidcSubject, UserStatus status) {
        store.setUserStatus(oidcSubject, status);
        auditTrail.record("security.user-status-changed", "security.user", null, oidcSubject + " -> " + status);
    }

    @Override
    @Transactional
    public void assignRole(String oidcSubject, String roleKey, AccessScope scope) {
        store.assignRole(oidcSubject, roleKey, scope);
        auditTrail.record("security.role-assigned", "security.user", null, oidcSubject + " <- " + roleKey);
    }
}
