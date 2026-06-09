package de.volantic.erp.security.application;

import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.AuthorizationService;
import de.volantic.erp.security.application.port.out.UserDirectory;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * RBAC-basierte Umsetzung des {@link AuthorizationService} (ADR-0004). Orchestriert nur: lädt den
 * Nutzer über den {@link UserDirectory}-Port und delegiert die eigentliche Entscheidung an das
 * Domänen-Aggregat {@code User}. Keine Persistenz-, keine Scope-Logik hier — die liegt in der Domäne.
 */
@Service
class AuthorizationServiceImpl implements AuthorizationService {

    private final UserDirectory users;

    AuthorizationServiceImpl(UserDirectory users) {
        this.users = users;
    }

    @Override
    public boolean isPermitted(String oidcSubject, String permission) {
        return isPermitted(oidcSubject, permission, AccessScope.GLOBAL);
    }

    @Override
    public boolean isPermitted(String oidcSubject, String permission, AccessScope scope) {
        return users.findByOidcSubject(oidcSubject)
                .map(user -> user.isPermitted(permission, scope))
                .orElse(false);
    }

    @Override
    public Set<String> permissionsOf(String oidcSubject) {
        return users.findByOidcSubject(oidcSubject)
                .map(de.volantic.erp.security.domain.model.User::permissionKeys)
                .orElseGet(Set::of);
    }
}
