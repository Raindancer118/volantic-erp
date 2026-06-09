package de.volantic.erp.security.domain.model;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.security.AccessScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure domain tests of the RBAC/scope decision — no Spring, no database. */
class UserAuthorizationTest {

    private User userWith(AccessScope scope, String... permissionKeys) {
        return userWith(UserStatus.ACTIVE, scope, permissionKeys);
    }

    private User userWith(UserStatus status, AccessScope scope, String... permissionKeys) {
        Set<Permission> perms = Set.of(permissionKeys).stream().map(Permission::new)
                .collect(Collectors.toSet());
        Role role = new Role("hr-mgr", perms);
        return new User("sub-1", status, List.of(new RoleAssignment(role, scope)));
    }

    @Test
    void globalAssignmentGrantsPermission() {
        User user = userWith(AccessScope.GLOBAL, "hr.salary:read");

        assertThat(user.isPermitted("hr.salary:read", AccessScope.GLOBAL)).isTrue();
        assertThat(user.isPermitted("hr.employee:write", AccessScope.GLOBAL)).isFalse();
    }

    @Test
    void scopedAssignmentCoversOnlyItsScope() {
        UUID departmentA = UuidV7.randomUuid();
        UUID departmentB = UuidV7.randomUuid();
        User user = userWith(AccessScope.of("DEPT", departmentA), "hr.salary:read");

        assertThat(user.isPermitted("hr.salary:read", AccessScope.of("DEPT", departmentA))).isTrue();
        assertThat(user.isPermitted("hr.salary:read", AccessScope.of("DEPT", departmentB))).isFalse();
        // a global request is not covered by a scoped assignment:
        assertThat(user.isPermitted("hr.salary:read", AccessScope.GLOBAL)).isFalse();
    }

    @Test
    void globalAssignmentAlsoCoversScopedRequest() {
        User user = userWith(AccessScope.GLOBAL, "hr.salary:read");

        assertThat(user.isPermitted("hr.salary:read", AccessScope.of("DEPT", UuidV7.randomUuid()))).isTrue();
    }

    @Test
    void permissionKeysListsAllKeys() {
        User user = userWith(AccessScope.GLOBAL, "hr.salary:read", "hr.employee:read");

        assertThat(user.permissionKeys())
                .containsExactlyInAnyOrder("hr.salary:read", "hr.employee:read");
    }

    @Test
    void disabledUserHasNoRightsDespiteRole() {
        User user = userWith(UserStatus.DISABLED, AccessScope.GLOBAL, "hr.salary:read");

        assertThat(user.isPermitted("hr.salary:read", AccessScope.GLOBAL)).isFalse();
        assertThat(user.permissionKeys()).isEmpty();
    }
}
