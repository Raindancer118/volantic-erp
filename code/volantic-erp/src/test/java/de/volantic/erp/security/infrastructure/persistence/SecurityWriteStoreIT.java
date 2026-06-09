package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.application.port.out.SecurityWriteStore;
import de.volantic.erp.security.application.port.out.UserDirectory;
import de.volantic.erp.security.domain.model.User;
import de.volantic.erp.security.domain.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end persistence test of the write side against a real PostgreSQL (Testcontainers): defining a
 * permission and role, creating a user and assigning the role via {@link SecurityWriteStore}, then
 * reading the authorization back through {@link UserDirectory}. Also covers disabling a user. The
 * idempotent-provisioning use-case logic lives in (and is unit-tested with) the application service.
 *
 * <p>Skipped without a Docker daemon ({@code disabledWithoutDocker}); runs fully in CI.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SecurityWriteStoreAdapter.class, UserGraphCache.class, UserDirectoryAdapter.class})
@Testcontainers(disabledWithoutDocker = true)
class SecurityWriteStoreIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SecurityWriteStore store;

    @Autowired
    private UserDirectory directory;

    @Test
    void persistsRolePermissionAndAssignmentEndToEnd() {
        store.upsertPermission("hr.salary:read", "Read salary");
        store.upsertRole("hr-mgr", "HR Manager", Set.of("hr.salary:read"));
        store.createUser("oidc-1", "m.muster", "m@example.de");
        store.assignRole("oidc-1", "hr-mgr", AccessScope.GLOBAL);

        User user = directory.findByOidcSubject("oidc-1").orElseThrow();
        assertThat(user.isPermitted("hr.salary:read", AccessScope.GLOBAL)).isTrue();
    }

    @Test
    void disablingAUserRevokesAccess() {
        store.upsertPermission("hr.salary:read", "Read salary");
        store.upsertRole("hr-mgr", "HR Manager", Set.of("hr.salary:read"));
        store.createUser("oidc-2", "a.admin", "a@example.de");
        store.assignRole("oidc-2", "hr-mgr", AccessScope.GLOBAL);
        assertThat(directory.findByOidcSubject("oidc-2").orElseThrow()
                .isPermitted("hr.salary:read", AccessScope.GLOBAL)).isTrue();

        store.setUserStatus("oidc-2", UserStatus.DISABLED);

        assertThat(directory.findByOidcSubject("oidc-2").orElseThrow()
                .isPermitted("hr.salary:read", AccessScope.GLOBAL)).isFalse();
    }
}
