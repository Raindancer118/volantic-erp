package de.volantic.erp.security.application;

import de.volantic.erp.security.AccessScope;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Orchestration tests of the service over the mocked {@link UserDirectory} port — no database. */
class AuthorizationServiceImplTest {

    private final UserDirectory users = mock(UserDirectory.class);
    private final AuthorizationServiceImpl service = new AuthorizationServiceImpl(users);

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
}
