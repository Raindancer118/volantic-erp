package de.volantic.erp.security.infrastructure.config;

import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.AuthorizationService;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.UUID;

/**
 * Brücke zwischen Spring-Method-Security und dem {@link AuthorizationService} (ADR-0004). Übersetzt
 * {@code @PreAuthorize("hasPermission(...)")}-Ausdrücke in Aufrufe des zentralen Autorisierungs-Ports.
 *
 * <p>Das OIDC-Subject ist {@link Authentication#getName()} (der {@code sub}-Claim des JWT). Die
 * {@code permission} ist der {@code resource:action}-Schlüssel (z. B. {@code "hr.salary:read"}).
 *
 * <p>Nutzung im Controller/Service:
 * <pre>
 *   &#64;PreAuthorize("hasPermission(null, 'hr.employee:read')")                 // global
 *   &#64;PreAuthorize("hasPermission(#abteilungId, 'DEPT', 'hr.salary:read')")   // scoped
 * </pre>
 */
@Component
class AuthorizationServicePermissionEvaluator implements PermissionEvaluator {

    private final AuthorizationService authorization;

    AuthorizationServicePermissionEvaluator(AuthorizationService authorization) {
        this.authorization = authorization;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (!authenticated(authentication)) {
            return false;
        }
        String subject = authentication.getName();
        if (targetDomainObject instanceof AccessScope scope) {
            return authorization.isPermitted(subject, String.valueOf(permission), scope);
        }
        return authorization.isPermitted(subject, String.valueOf(permission));
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (!authenticated(authentication)) {
            return false;
        }
        AccessScope scope = targetId == null
                ? AccessScope.GLOBAL
                : AccessScope.of(targetType, UUID.fromString(targetId.toString()));
        return authorization.isPermitted(authentication.getName(), String.valueOf(permission), scope);
    }

    private static boolean authenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }
}
