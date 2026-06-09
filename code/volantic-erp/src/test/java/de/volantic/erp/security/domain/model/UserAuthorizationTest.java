package de.volantic.erp.security.domain.model;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.security.AccessScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Reine Domänen-Tests der RBAC-/Scope-Entscheidung — ohne Spring, ohne Datenbank. */
class UserAuthorizationTest {

    private User userWith(AccessScope scope, String... permissionKeys) {
        return userWith(UserStatus.ACTIVE, scope, permissionKeys);
    }

    private User userWith(UserStatus status, AccessScope scope, String... permissionKeys) {
        Set<Permission> perms = Set.of(permissionKeys).stream().map(Permission::new)
                .collect(java.util.stream.Collectors.toSet());
        Role role = new Role("hr-mgr", perms);
        return new User("sub-1", status, List.of(new RoleAssignment(role, scope)));
    }

    @Test
    void globaleZuweisungGewaehrtBerechtigung() {
        User user = userWith(AccessScope.GLOBAL, "hr.salary:read");

        assertThat(user.isPermitted("hr.salary:read", AccessScope.GLOBAL)).isTrue();
        assertThat(user.isPermitted("hr.employee:write", AccessScope.GLOBAL)).isFalse();
    }

    @Test
    void scopedZuweisungDecktNurIhrenScope() {
        UUID abteilungA = UuidV7.randomUuid();
        UUID abteilungB = UuidV7.randomUuid();
        User user = userWith(AccessScope.of("DEPT", abteilungA), "hr.salary:read");

        assertThat(user.isPermitted("hr.salary:read", AccessScope.of("DEPT", abteilungA))).isTrue();
        assertThat(user.isPermitted("hr.salary:read", AccessScope.of("DEPT", abteilungB))).isFalse();
        // eine globale Anfrage wird von einer scoped Zuweisung nicht gedeckt:
        assertThat(user.isPermitted("hr.salary:read", AccessScope.GLOBAL)).isFalse();
    }

    @Test
    void globaleZuweisungDecktAuchScopedAnfrage() {
        User user = userWith(AccessScope.GLOBAL, "hr.salary:read");

        assertThat(user.isPermitted("hr.salary:read", AccessScope.of("DEPT", UuidV7.randomUuid()))).isTrue();
    }

    @Test
    void permissionKeysListetAlleSchluessel() {
        User user = userWith(AccessScope.GLOBAL, "hr.salary:read", "hr.employee:read");

        assertThat(user.permissionKeys())
                .containsExactlyInAnyOrder("hr.salary:read", "hr.employee:read");
    }

    @Test
    void gesperrterNutzerHatTrotzRolleKeineRechte() {
        User user = userWith(UserStatus.DISABLED, AccessScope.GLOBAL, "hr.salary:read");

        assertThat(user.isPermitted("hr.salary:read", AccessScope.GLOBAL)).isFalse();
        assertThat(user.permissionKeys()).isEmpty();
    }
}
