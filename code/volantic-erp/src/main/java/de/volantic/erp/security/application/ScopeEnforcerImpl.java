package de.volantic.erp.security.application;

import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.AuthorizationService;
import de.volantic.erp.security.ScopeEnforcer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Default {@link ScopeEnforcer}: resolves the acting OIDC subject from the security context and delegates
 * to {@link AuthorizationService}. Throwing {@link AccessDeniedException} yields the same 403 mapping as
 * a failed {@code @PreAuthorize}, so module exception handlers need no special case.
 */
@Component
class ScopeEnforcerImpl implements ScopeEnforcer {

    private final AuthorizationService authorization;

    ScopeEnforcerImpl(AuthorizationService authorization) {
        this.authorization = authorization;
    }

    @Override
    public void require(String permission, UUID orgUnitId) {
        String subject = currentSubject();
        boolean ok = orgUnitId == null
                ? authorization.isPermitted(subject, permission)
                : authorization.isPermitted(subject, permission, AccessScope.orgUnit(orgUnitId));
        if (!ok) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    @Override
    public void requireAnywhere(String permission) {
        if (!authorization.hasPermissionAnywhere(currentSubject(), permission)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    @Override
    public Optional<Set<UUID>> permittedOrgUnits(String permission) {
        return authorization.permittedOrgUnits(currentSubject(), permission);
    }

    private static String currentSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Access is denied");
        }
        return authentication.getName();
    }
}
