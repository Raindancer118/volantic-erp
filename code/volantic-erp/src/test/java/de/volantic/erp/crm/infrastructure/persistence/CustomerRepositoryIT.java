package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.crm.application.port.out.CustomerRepository;
import de.volantic.erp.crm.domain.model.Customer;
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

import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistence integration test for the customer adapter against a real PostgreSQL (Testcontainers):
 * insert, lookup, uniqueness, update roundtrip and org-unit scoping, validating the JPA mapping against
 * the Flyway schema. Skipped without Docker; runs in CI.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CustomerRepositoryAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class CustomerRepositoryIT {

    private static final UUID ORG_A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID ORG_B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CustomerRepository repository;

    @Autowired
    private CustomerJpaRepository jpa;

    @Autowired
    private TestEntityManager em;

    @Test
    void auditTimestampsArePopulatedOnInsert() {
        Customer customer = Customer.create(ORG_A, "C-2001", "Audited GmbH", null);
        repository.save(customer);

        // flush the INSERT and detach, so the reload reads the persisted row (timestamps written)
        em.flush();
        em.clear();

        CustomerEntity entity = jpa.findById(customer.id().value()).orElseThrow();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getModifiedAt()).isNotNull();
    }

    @Test
    void savesLoadsAndUpdatesCustomer() {
        Customer customer = Customer.create(ORG_A, "C-1001", "ACME GmbH", "info@acme.de");
        repository.save(customer);

        assertThat(repository.existsByCustomerNumber("C-1001")).isTrue();
        assertThat(repository.existsByCustomerNumber("C-9999")).isFalse();

        Customer loaded = repository.findById(customer.id()).orElseThrow();
        assertThat(loaded.id()).isEqualTo(customer.id());
        assertThat(loaded.name()).isEqualTo("ACME GmbH");
        assertThat(loaded.orgUnitId()).isEqualTo(ORG_A);

        loaded.rename("ACME AG");
        loaded.changeEmail("contact@acme.de");
        repository.save(loaded);

        Customer reloaded = repository.findById(customer.id()).orElseThrow();
        assertThat(reloaded.name()).isEqualTo("ACME AG");
        assertThat(reloaded.email()).isEqualTo("contact@acme.de");
        assertThat(reloaded.customerNumber()).isEqualTo("C-1001");
        assertThat(reloaded.orgUnitId()).isEqualTo(ORG_A);
        assertThat(repository.findAll(org.springframework.data.domain.Pageable.unpaged()).getTotalElements()).isEqualTo(1);
    }

    @Test
    void rejectsAStaleUpdateInsteadOfLosingTheConcurrentChange() {
        Customer customer = Customer.create(ORG_A, "C-LOCK", "Locked GmbH", null);
        repository.save(customer);
        em.flush();
        em.clear();

        // Two independent loads of the same row, both at the same version (simulating two requests).
        Customer first = repository.findById(customer.id()).orElseThrow();
        Customer second = repository.findById(customer.id()).orElseThrow();
        assertThat(first.version()).isEqualTo(second.version());

        // The first writer wins.
        first.rename("Winner GmbH");
        repository.save(first);
        em.flush();

        // The second writer holds a now-stale version — its update must be rejected, not silently applied.
        second.rename("Loser GmbH");
        assertThatThrownBy(() -> {
            repository.save(second);
            em.flush();
        }).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void findAllSlicesAndCountsTotal() {
        for (int i = 1; i <= 3; i++) {
            repository.save(Customer.create(ORG_A, "P-" + i, "Partner " + i, null));
        }

        var firstPage = repository.findAll(org.springframework.data.domain.PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findAllInOrgUnitsReturnsOnlyTheGivenUnits() {
        repository.save(Customer.create(ORG_A, "A-1", "Alpha", null));
        repository.save(Customer.create(ORG_A, "A-2", "Beta", null));
        repository.save(Customer.create(ORG_B, "B-1", "Gamma", null));

        var onlyA = repository.findAllInOrgUnits(Set.of(ORG_A),
                org.springframework.data.domain.Pageable.unpaged());

        assertThat(onlyA.getTotalElements()).isEqualTo(2);
        assertThat(onlyA.getContent()).allSatisfy(c -> assertThat(c.orgUnitId()).isEqualTo(ORG_A));
    }
}
