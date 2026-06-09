package de.volantic.erp.core.numberrange.application;

import de.volantic.erp.core.numberrange.NumberRangeNotDefinedException;
import de.volantic.erp.core.numberrange.application.port.out.NumberRangeStore;
import de.volantic.erp.core.numberrange.domain.model.NumberRange;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Orchestration of {@link NumberRangeService} over the mocked {@link NumberRangeStore}. */
class NumberRangeServiceTest {

    private final NumberRangeStore store = mock(NumberRangeStore.class);
    private final NumberRangeService service = new NumberRangeService(store);

    @Test
    void defineRangeIsIdempotent() {
        when(store.exists("sales.invoice")).thenReturn(true);
        service.defineRange("sales.invoice", "RE-", 6, 1);
        verify(store, never()).create(any());
    }

    @Test
    void defineRangeCreatesWhenAbsent() {
        when(store.exists("sales.invoice")).thenReturn(false);
        service.defineRange("sales.invoice", "RE-", 6, 1);
        verify(store).create(any(NumberRange.class));
    }

    @Test
    void nextAllocatesAndSaves() {
        when(store.findForUpdate("sales.invoice"))
                .thenReturn(Optional.of(new NumberRange("sales.invoice", "RE-", 6, 1)));

        assertThat(service.next("sales.invoice")).isEqualTo("RE-000001");
        verify(store).save(any(NumberRange.class));
    }

    @Test
    void nextThrowsWhenUndefined() {
        when(store.findForUpdate("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.next("missing")).isInstanceOf(NumberRangeNotDefinedException.class);
    }
}
