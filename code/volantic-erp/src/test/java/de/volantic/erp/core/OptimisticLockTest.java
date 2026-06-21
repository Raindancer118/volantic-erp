package de.volantic.erp.core;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the shared optimistic-lock guard used by every module's update use cases. */
class OptimisticLockTest {

    @Test
    void passesWhenVersionsMatch() {
        assertThatCode(() -> OptimisticLock.check(5L, 5L, "res")).doesNotThrowAnyException();
    }

    @Test
    void failsWhenVersionsDiffer() {
        assertThatThrownBy(() -> OptimisticLock.check(6L, 5L, "res"))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void failsWhenActualVersionIsUnknown() {
        assertThatThrownBy(() -> OptimisticLock.check(null, 0L, "res"))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
