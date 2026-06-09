package de.volantic.erp.core.numberrange.infrastructure.persistence;

import de.volantic.erp.core.numberrange.application.port.out.NumberRangeStore;
import de.volantic.erp.core.numberrange.domain.model.NumberRange;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence IT for the number-range store against a real PostgreSQL (Testcontainers): create, locked
 * load, and consecutive gap-free allocation persisting the advanced counter. Skipped without Docker.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(NumberRangeStoreAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class NumberRangeStoreIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private NumberRangeStore store;

    @Test
    @Transactional
    void allocatesConsecutiveNumbersAndPersistsCounter() {
        store.create(new NumberRange("sales.invoice", "RE-", 6, 1));
        assertThat(store.exists("sales.invoice")).isTrue();

        assertThat(allocate("sales.invoice")).isEqualTo("RE-000001");
        assertThat(allocate("sales.invoice")).isEqualTo("RE-000002");
        assertThat(allocate("sales.invoice")).isEqualTo("RE-000003");

        assertThat(store.findForUpdate("sales.invoice").orElseThrow().nextValue()).isEqualTo(4);
    }

    /** Mirrors what NumberRangeService.next does: locked load, allocate, save. */
    private String allocate(String key) {
        NumberRange range = store.findForUpdate(key).orElseThrow();
        String number = range.allocate();
        store.save(range);
        return number;
    }
}
