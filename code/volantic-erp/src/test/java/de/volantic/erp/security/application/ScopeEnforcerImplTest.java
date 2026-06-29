package de.volantic.erp.security.application;

import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.AuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Unit tests of {@link ScopeEnforcerImpl}: subject resolution from the context + delegation to authz. */
class ScopeEnforcerImplTest {

    private static final UUID UNIT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final ScopeEnforcerImpl enforcer = new ScopeEnforcerImpl(authorization);

    private void authenticatedAs(String subject) {
        var auth = new UsernamePasswordAuthenticationToken(subject, null, AuthorityUtils.NO_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requirePassesWhenAuthorizedForTheUnit() {
        authenticatedAs("sub-1");
        when(authorization.isPermitted("sub-1", "crm.customer:read", AccessScope.orgUnit(UNIT))).thenReturn(true);

        enforcer.require("crm.customer:read", UNIT); // does not throw
    }

    @Test
    void requireThrowsWhenNotAuthorizedForTheUnit() {
        authenticatedAs("sub-1");
        when(authorization.isPermitted("sub-1", "crm.customer:read", AccessScope.orgUnit(UNIT))).thenReturn(false);

        assertThatThrownBy(() -> enforcer.require("crm.customer:read", UNIT))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireWithNullOrgUnitFallsBackToGlobalCheck() {
        authenticatedAs("sub-1");
        when(authorization.isPermitted("sub-1", "crm.customer:read")).thenReturn(true);

        enforcer.require("crm.customer:read", null);
    }

    @Test
    void requireAnywhereGatesOnHavingThePermissionInSomeScope() {
        authenticatedAs("sub-1");
        when(authorization.hasPermissionAnywhere("sub-1", "crm.customer:read")).thenReturn(false);

        assertThatThrownBy(() -> enforcer.requireAnywhere("crm.customer:read"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void permittedOrgUnitsDelegatesToAuthorization() {
        authenticatedAs("sub-1");
        when(authorization.permittedOrgUnits("sub-1", "crm.customer:read")).thenReturn(Optional.of(Set.of(UNIT)));

        assertThat(enforcer.permittedOrgUnits("crm.customer:read")).contains(Set.of(UNIT));
    }

    @Test
    void unauthenticatedContextIsDenied() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> enforcer.requireAnywhere("crm.customer:read"))
                .isInstanceOf(AccessDeniedException.class);
    }
}
