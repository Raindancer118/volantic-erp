package de.volantic.erp.security.application;

import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.application.port.out.OrgUnitHierarchy;
import de.volantic.erp.security.application.port.out.UserDirectory;
import de.volantic.erp.security.domain.model.Permission;
import de.volantic.erp.security.domain.model.Role;
import de.volantic.erp.security.domain.model.RoleAssignment;
import de.volantic.erp.security.domain.model.User;
import de.volantic.erp.security.domain.model.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Orchestration tests of the service over the mocked {@link UserDirectory} port — no database. */
class AuthorizationServiceImplTest {

    private final UserDirectory users = mock(UserDirectory.class);
    private final OrgUnitHierarchy hierarchy = mock(OrgUnitHierarchy.class);
    private final AuthorizationServiceImpl service = new AuthorizationServiceImpl(users, hierarchy);

    private void directoryReturns(User user) {
        when(users.findByOidcSubject("sub-1")).thenReturn(Optional.of(user));
    }

    private User userWith(String permissionKey, AccessScope scope) {
        Role role = new Role("hr-mgr", Set.of(new Permission(permissionKey)));
        return new User("sub-1", UserStatus.ACTIVE, List.of(new RoleAssignment(role, scope)));
    }

    @Test
    void delegatesDecisionToTheDomain() {
        directoryReturns(userWith("hr.salary:read", AccessScope.GLOBAL));

        assertThat(service.isPermitted("sub-1", "hr.salary:read")).isTrue();
        assertThat(service.isPermitted("sub-1", "hr.employee:write")).isFalse();
    }

    @Test
    void unknownUserHasNoRights() {
        when(users.findByOidcSubject(any())).thenReturn(Optional.empty());

        assertThat(service.isPermitted("stranger", "hr.salary:read")).isFalse();
        assertThat(service.isPermitted("stranger", "hr.salary:read", AccessScope.GLOBAL)).isFalse();
        assertThat(service.permissionsOf("stranger")).isEmpty();
    }

    @Test
    void permissionsOfListsAllKeys() {
        Role role = new Role("hr-mgr", Set.of(new Permission("hr.salary:read"), new Permission("hr.employee:read")));
        directoryReturns(new User("sub-1", UserStatus.ACTIVE, List.of(new RoleAssignment(role, AccessScope.GLOBAL))));

        assertThat(service.permissionsOf("sub-1"))
                .containsExactlyInAnyOrder("hr.salary:read", "hr.employee:read");
    }

    // --- Org-unit hierarchy resolution (ADR-0007) ---

    private static final UUID ROOT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CHILD = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID SIBLING = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    @Test
    void grantOnAncestorCoversDescendantUnit() {
        directoryReturns(userWith("crm.customer:read", AccessScope.orgUnit(ROOT)));
        // CHILD's ancestor chain is [CHILD, ROOT]; the grant sits on ROOT.
        when(hierarchy.ancestorIds(CHILD)).thenReturn(List.of(CHILD, ROOT));

        assertThat(service.isPermitted("sub-1", "crm.customer:read", AccessScope.orgUnit(CHILD))).isTrue();
    }

    @Test
    void grantOnChildDoesNotCoverSiblingOrParent() {
        directoryReturns(userWith("crm.customer:read", AccessScope.orgUnit(CHILD)));
        when(hierarchy.ancestorIds(SIBLING)).thenReturn(List.of(SIBLING, ROOT));
        when(hierarchy.ancestorIds(ROOT)).thenReturn(List.of(ROOT));

        assertThat(service.isPermitted("sub-1", "crm.customer:read", AccessScope.orgUnit(SIBLING))).isFalse();
        assertThat(service.isPermitted("sub-1", "crm.customer:read", AccessScope.orgUnit(ROOT))).isFalse();
    }

    @Test
    void globalGrantCoversAnyOrgUnitWithoutTreeLookup() {
        directoryReturns(userWith("crm.customer:read", AccessScope.GLOBAL));
        when(hierarchy.ancestorIds(any())).thenReturn(List.of()); // not even consulted

        assertThat(service.isPermitted("sub-1", "crm.customer:read", AccessScope.orgUnit(CHILD))).isTrue();
    }

    @Test
    void permittedOrgUnitsIsEmptyOptionalForGlobalGrant() {
        directoryReturns(userWith("crm.customer:read", AccessScope.GLOBAL));

        assertThat(service.permittedOrgUnits("sub-1", "crm.customer:read")).isEmpty();
    }

    @Test
    void permittedOrgUnitsExpandsScopedGrantToDescendants() {
        directoryReturns(userWith("crm.customer:read", AccessScope.orgUnit(ROOT)));
        when(hierarchy.descendantIds(Set.of(ROOT))).thenReturn(Set.of(ROOT, CHILD, SIBLING));

        assertThat(service.permittedOrgUnits("sub-1", "crm.customer:read"))
                .contains(Set.of(ROOT, CHILD, SIBLING));
    }

    @Test
    void permittedOrgUnitsIsEmptySetWhenPermissionHeldNowhere() {
        directoryReturns(userWith("crm.customer:read", AccessScope.orgUnit(CHILD)));

        // Holds crm.customer:read in CHILD, but not the requested permission anywhere.
        assertThat(service.permittedOrgUnits("sub-1", "crm.customer:update")).contains(Set.of());
        assertThat(service.hasPermissionAnywhere("sub-1", "crm.customer:update")).isFalse();
        assertThat(service.hasPermissionAnywhere("sub-1", "crm.customer:read")).isTrue();
    }
}
