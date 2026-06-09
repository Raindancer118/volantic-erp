package de.volantic.erp.security.infrastructure.config;

import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.AuthorizationService;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.UUID;

/**
 * Bridge between Spring method security and the {@link AuthorizationService} (ADR-0004). Translates
 * {@code @PreAuthorize("hasPermission(...)")} expressions into calls on the central authorization port.
 *
 * <p>The OIDC subject is {@link Authentication#getName()} (the JWT's {@code sub} claim). The
 * {@code permission} is the {@code resource:action} key (e.g. {@code "hr.salary:read"}).
 *
 * <p>Usage in a controller/service:
 * <pre>
 *   &#64;PreAuthorize("hasPermission(null, 'hr.employee:read')")                // global
 *   &#64;PreAuthorize("hasPermission(#departmentId, 'DEPT', 'hr.salary:read')") // scoped
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
