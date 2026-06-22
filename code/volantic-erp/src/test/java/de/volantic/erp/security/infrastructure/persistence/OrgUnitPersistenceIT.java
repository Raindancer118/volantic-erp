package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.security.application.port.out.OrgUnitRepository;
import de.volantic.erp.security.domain.model.OrgUnit;
import de.volantic.erp.security.domain.model.OrgUnitId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence integration test for the org-unit adapter against a real PostgreSQL (Testcontainers):
 * insert, lookup, existsByCode/existsById checks, version population on save, and parentId round-trip.
 * Validates the JPA mapping against the Flyway schema ({@code ddl-auto=validate}).
 *
 * <p>Skipped without a Docker daemon ({@code disabledWithoutDocker}); runs fully in CI.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OrgUnitRepositoryAdapter.class, OrgTreeCache.class})
@Testcontainers(disabledWithoutDocker = true)
class OrgUnitPersistenceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OrgUnitRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    void savingRootPopulatesVersionAndNullParent() {
        OrgUnit root = OrgUnit.create("ROOT", "Root Organization", null);

        OrgUnit saved = repository.save(root);

        assertThat(saved.version()).isNotNull();
        assertThat(saved.parentId()).isNull();
        assertThat(saved.isRoot()).isTrue();
        assertThat(saved.code()).isEqualTo("ROOT");
        assertThat(saved.name()).isEqualTo("Root Organization");
    }

    @Test
    void savingChildRoundTripsParentId() {
        OrgUnit root = repository.save(OrgUnit.create("ROOT", "Root Organization", null));
        em.flush();
        em.clear();

        OrgUnit child = OrgUnit.create("BERLIN", "Berlin", root.id());
        OrgUnit savedChild = repository.save(child);
        em.flush();
        em.clear();

        OrgUnit loaded = repository.findById(savedChild.id()).orElseThrow();
        assertThat(loaded.parentId()).isEqualTo(root.id());
        assertThat(loaded.isRoot()).isFalse();
        assertThat(loaded.code()).isEqualTo("BERLIN");
        assertThat(loaded.name()).isEqualTo("Berlin");
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        OrgUnitId unknown = new OrgUnitId(java.util.UUID.randomUUID());

        Optional<OrgUnit> result = repository.findById(unknown);

        assertThat(result).isEmpty();
    }

    @Test
    void existsByCodeReturnsTrueForExistingAndFalseForMissing() {
        repository.save(OrgUnit.create("PRESENT", "Present Org", null));
        em.flush();

        assertThat(repository.existsByCode("PRESENT")).isTrue();
        assertThat(repository.existsByCode("ABSENT")).isFalse();
    }

    @Test
    void existsByIdReturnsTrueForSavedAndFalseForUnknown() {
        OrgUnit saved = repository.save(OrgUnit.create("HQ", "Headquarters", null));
        em.flush();
        OrgUnitId unknown = new OrgUnitId(java.util.UUID.randomUUID());

        assertThat(repository.existsById(saved.id())).isTrue();
        assertThat(repository.existsById(unknown)).isFalse();
    }

    @Test
    void findAllPagedReturnsCorrectSliceAndCount() {
        for (int i = 1; i <= 3; i++) {
            repository.save(OrgUnit.create("ORG-" + i, "Organization " + i, null));
        }
        em.flush();
        em.clear();

        Page<OrgUnit> firstPage = repository.findAll(PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }
}
