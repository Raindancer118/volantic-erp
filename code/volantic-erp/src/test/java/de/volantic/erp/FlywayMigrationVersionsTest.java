package de.volantic.erp;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the Flyway-per-module setup: Spring Boot aggregates every {@code db/migration/<module>}
 * location into a single {@code flyway_schema_history}, so versions must be globally unique. A
 * collision (e.g. two modules both shipping {@code V001}) otherwise only surfaces in the
 * Docker-backed integration tests in CI. This plain test catches it locally, without Docker.
 *
 * <p>Convention (CLAUDE.md): one hundreds-block per module — security {@code V0xx}, crm {@code V1xx},
 * catalog {@code V2xx}, …
 */
class FlywayMigrationVersionsTest {

    private static final Pattern VERSIONED = Pattern.compile("(V\\d+)__.*\\.sql$");

    @Test
    void migrationVersionsAreGloballyUnique() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:db/migration/**/V*.sql");

        List<String> versions = new ArrayList<>();
        for (Resource resource : resources) {
            String name = resource.getFilename();
            if (name == null) {
                continue;
            }
            Matcher matcher = VERSIONED.matcher(name);
            if (matcher.find()) {
                versions.add(matcher.group(1));
            }
        }

        assertThat(versions).as("expected to discover Flyway migrations on the classpath").isNotEmpty();

        List<String> duplicates = versions.stream()
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();

        assertThat(duplicates)
                .as("Flyway versions must be globally unique across module folders (see CLAUDE.md hundreds-block convention)")
                .isEmpty();
    }
}
