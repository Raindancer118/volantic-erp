package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.application.port.out.UserDirectory;
import de.volantic.erp.security.domain.model.User;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence integration test against a real PostgreSQL (Testcontainers). Validates that the JPA
 * mappings match the Flyway schema ({@code ddl-auto=validate}), that the {@link UserDirectoryAdapter}
 * maps onto the domain correctly, and — as an N+1 guard — that it loads the role/permission graph in
 * exactly one query.
 *
 * <p>Without a running Docker daemon the class is skipped ({@code disabledWithoutDocker}); in CI
 * (runner with Docker) it runs fully.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserDirectoryAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class SecurityPersistenceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RoleJpaRepository roles;

    @Autowired
    private PermissionJpaRepository permissions;

    @Autowired
    private UserDirectory directory;

    @Autowired
    private TestEntityManager em;

    @Autowired
    private EntityManagerFactory emf;

    @Test
    void loadsUserGraphInOneQueryAndMapsToDomain() {
        PermissionEntity salaryRead = permissions.save(new PermissionEntity("hr.salary:read", "Read salary"));
        RoleEntity hrManager = new RoleEntity("hr-mgr", "HR Manager");
        hrManager.addPermission(salaryRead);
        roles.save(hrManager);

        AppUserEntity user = new AppUserEntity("oidc-subject-123", "m.muster", "m@example.de");
        user.assignRole(hrManager, AccessScope.GLOBAL);
        em.persist(user);

        // Clear the persistence context so the load really hits the DB (otherwise a cache hit).
        em.flush();
        em.clear();
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        User loaded = directory.findByOidcSubject("oidc-subject-123").orElseThrow();

        assertThat(loaded.oidcSubject()).isEqualTo("oidc-subject-123");
        assertThat(loaded.permissionKeys()).containsExactly("hr.salary:read");
        assertThat(loaded.isPermitted("hr.salary:read", AccessScope.GLOBAL)).isTrue();
        // N+1 guard: user + roles + permissions in exactly one SQL query (@EntityGraph).
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1L);
    }
}
