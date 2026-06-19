package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.crm.application.port.out.CustomerRepository;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.OrgUnitId;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence integration test for the customer adapter against a real PostgreSQL (Testcontainers):
 * insert, lookup, uniqueness, org-unit scope and update roundtrip, validating the JPA mapping against
 * the Flyway schema. Skipped without Docker; runs in CI.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CustomerRepositoryAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class CustomerRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final OrgUnitId ORG = OrgUnitId.DEFAULT;

    @Autowired
    private CustomerRepository repository;

    @Autowired
    private CustomerJpaRepository jpa;

    @Autowired
    private TestEntityManager em;

    @Test
    void auditTimestampsArePopulatedOnInsert() {
        Customer customer = Customer.create(ORG, "C-2001", "Audited GmbH", null);
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
        Customer customer = Customer.create(ORG, "C-1001", "ACME GmbH", "info@acme.de");
        repository.save(customer);

        assertThat(repository.existsByCustomerNumber("C-1001")).isTrue();
        assertThat(repository.existsByCustomerNumber("C-9999")).isFalse();

        Customer loaded = repository.findById(customer.id()).orElseThrow();
        assertThat(loaded.id()).isEqualTo(customer.id());
        assertThat(loaded.name()).isEqualTo("ACME GmbH");

        loaded.rename("ACME AG");
        loaded.changeEmail("contact@acme.de");
        repository.save(loaded);

        Customer reloaded = repository.findById(customer.id()).orElseThrow();
        assertThat(reloaded.name()).isEqualTo("ACME AG");
        assertThat(reloaded.email()).isEqualTo("contact@acme.de");
        assertThat(reloaded.customerNumber()).isEqualTo("C-1001");
        assertThat(repository.findAll(org.springframework.data.domain.Pageable.unpaged()).getTotalElements()).isEqualTo(1);
    }

    @Test
    void orgUnitIsPersistedAndReloaded() {
        OrgUnitId orgUnit = new OrgUnitId(UUID.randomUUID());
        Customer customer = Customer.create(orgUnit, "C-4001", "Scoped GmbH", null);
        repository.save(customer);
        em.flush();
        em.clear();

        assertThat(repository.findById(customer.id()).orElseThrow().orgUnitId()).isEqualTo(orgUnit);
    }

    @Test
    void findAllSlicesAndCountsTotal() {
        for (int i = 1; i <= 3; i++) {
            repository.save(Customer.create(ORG, "P-" + i, "Partner " + i, null));
        }

        var firstPage = repository.findAll(org.springframework.data.domain.PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }
}
