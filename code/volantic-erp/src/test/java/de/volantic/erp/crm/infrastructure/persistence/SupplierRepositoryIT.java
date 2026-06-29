package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.crm.application.port.out.SupplierRepository;
import de.volantic.erp.crm.domain.model.Supplier;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Persistence IT for the supplier adapter against a real PostgreSQL (Testcontainers). Skipped without Docker. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SupplierRepositoryAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class SupplierRepositoryIT {

    private static final UUID ORG_A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID ORG_B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SupplierRepository repository;

    @Test
    void savesLoadsAndUpdatesSupplier() {
        Supplier supplier = Supplier.create(ORG_A, "S-1001", "Globex", "sales@globex.de");
        repository.save(supplier);

        assertThat(repository.existsBySupplierNumber("S-1001")).isTrue();

        Supplier loaded = repository.findById(supplier.id()).orElseThrow();
        assertThat(loaded.name()).isEqualTo("Globex");
        assertThat(loaded.orgUnitId()).isEqualTo(ORG_A);

        loaded.rename("Globex Corp");
        repository.save(loaded);

        assertThat(repository.findById(supplier.id()).orElseThrow().name()).isEqualTo("Globex Corp");
        assertThat(repository.findAll(org.springframework.data.domain.Pageable.unpaged()).getTotalElements()).isEqualTo(1);
    }

    @Test
    void findAllInOrgUnitsReturnsOnlyTheGivenUnits() {
        repository.save(Supplier.create(ORG_A, "A-1", "Alpha", null));
        repository.save(Supplier.create(ORG_B, "B-1", "Beta", null));

        var onlyA = repository.findAllInOrgUnits(Set.of(ORG_A),
                org.springframework.data.domain.Pageable.unpaged());

        assertThat(onlyA.getTotalElements()).isEqualTo(1);
        assertThat(onlyA.getContent()).allSatisfy(s -> assertThat(s.orgUnitId()).isEqualTo(ORG_A));
    }
}
