package de.volantic.erp.security.application;

import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.AuthorizationService;
import de.volantic.erp.security.application.port.out.UserDirectory;
import de.volantic.erp.security.domain.model.User;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * RBAC-based implementation of {@link AuthorizationService} (ADR-0004). Orchestration only: loads the
 * user via the {@link UserDirectory} port and delegates the actual decision to the {@code User} domain
 * aggregate. No persistence and no scope logic here — that lives in the domain.
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
                .map(User::permissionKeys)
                .orElseGet(Set::of);
    }
}
