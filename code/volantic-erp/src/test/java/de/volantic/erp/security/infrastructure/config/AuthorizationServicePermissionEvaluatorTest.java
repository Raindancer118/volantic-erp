package de.volantic.erp.security.infrastructure.config;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Übersetzt {@code hasPermission(...)} korrekt auf den {@link AuthorizationService} — ohne Spring-Kontext. */
class AuthorizationServicePermissionEvaluatorTest {

    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final AuthorizationServicePermissionEvaluator evaluator =
            new AuthorizationServicePermissionEvaluator(authorization);

    private Authentication authAs(String subject) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn(subject);
        return auth;
    }

    @Test
    void globaleAnfrageOhneScope() {
        Authentication auth = authAs("sub-1");
        when(authorization.isPermitted("sub-1", "hr.employee:read")).thenReturn(true);

        assertThat(evaluator.hasPermission(auth, null, "hr.employee:read")).isTrue();
    }

    @Test
    void scopedAnfrageUeberTargetIdUndTyp() {
        Authentication auth = authAs("sub-1");
        UUID dept = UuidV7.randomUuid();
        when(authorization.isPermitted(eq("sub-1"), eq("hr.salary:read"), eq(AccessScope.of("DEPT", dept))))
                .thenReturn(true);

        assertThat(evaluator.hasPermission(auth, dept, "DEPT", "hr.salary:read")).isTrue();
    }

    @Test
    void scopedAnfrageUeberAccessScopeObjekt() {
        Authentication auth = authAs("sub-1");
        UUID dept = UuidV7.randomUuid();
        AccessScope scope = AccessScope.of("DEPT", dept);
        when(authorization.isPermitted("sub-1", "hr.salary:read", scope)).thenReturn(true);

        assertThat(evaluator.hasPermission(auth, scope, "hr.salary:read")).isTrue();
    }

    @Test
    void nichtAuthentifiziertVerweigert() {
        Authentication anonymous = mock(Authentication.class);
        when(anonymous.isAuthenticated()).thenReturn(false);

        assertThat(evaluator.hasPermission(anonymous, null, "hr.employee:read")).isFalse();
        assertThat(evaluator.hasPermission(null, null, "hr.employee:read")).isFalse();
    }
}
