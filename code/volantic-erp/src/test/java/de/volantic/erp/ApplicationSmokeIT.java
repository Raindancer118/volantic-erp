package de.volantic.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full-context smoke test against a real PostgreSQL (Testcontainers): boots the entire application —
 * web/security, JPA + Flyway (all module migrations), the Redis cache config, and the Spring Modulith
 * JDBC event publication registry. This catches wiring and schema problems (e.g. the event_publication
 * table not matching the registry) that the sliced @DataJpaTest/@WebMvcTest tests cannot. The OIDC
 * decoder is lazy (jwk-set-uri) and Redis connects lazily, so no external service is needed to boot.
 *
 * <p>Skipped without a Docker daemon; runs fully in CI.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ApplicationSmokeIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void contextLoads() {
        // The application context started successfully with all modules, migrations and the
        // event publication registry wired — that is the assertion.
    }
}
