package de.volantic.erp.core.measure;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure value-object tests for {@link Money}: scale normalization, arithmetic, currency safety. */
class MoneyTest {

    @Test
    void normalisesToCurrencyScaleWithBankersRounding() {
        assertThat(Money.of("19.985", "EUR").amount()).isEqualTo(new BigDecimal("19.98")); // HALF_EVEN
        assertThat(Money.of("19.995", "EUR").amount()).isEqualTo(new BigDecimal("20.00"));
        assertThat(Money.of("5", "EUR")).isEqualTo(Money.of("5.00", "EUR")); // equal regardless of input scale
    }

    @Test
    void addsAndSubtractsSameCurrency() {
        assertThat(Money.of("10.00", "EUR").plus(Money.of("2.50", "EUR"))).isEqualTo(Money.of("12.50", "EUR"));
        assertThat(Money.of("10.00", "EUR").minus(Money.of("12.00", "EUR")).isNegative()).isTrue();
    }

    @Test
    void multipliesAndRounds() {
        assertThat(Money.of("19.99", "EUR").multiply(new BigDecimal("3"))).isEqualTo(Money.of("59.97", "EUR"));
    }

    @Test
    void rejectsCurrencyMismatch() {
        assertThatThrownBy(() -> Money.of("1.00", "EUR").plus(Money.of("1.00", "USD")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroIsZero() {
        assertThat(Money.of("0.00", "EUR").isZero()).isTrue();
    }
}
