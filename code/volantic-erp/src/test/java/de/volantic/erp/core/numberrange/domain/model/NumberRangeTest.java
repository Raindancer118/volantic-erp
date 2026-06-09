package de.volantic.erp.core.numberrange.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure domain tests for {@link NumberRange}: formatting and consecutive allocation. */
class NumberRangeTest {

    @Test
    void allocatesConsecutiveZeroPaddedNumbers() {
        NumberRange range = new NumberRange("sales.invoice", "RE-", 6, 1);

        assertThat(range.allocate()).isEqualTo("RE-000001");
        assertThat(range.allocate()).isEqualTo("RE-000002");
        assertThat(range.nextValue()).isEqualTo(3);
    }

    @Test
    void numberWiderThanPaddingIsNotTruncated() {
        NumberRange range = new NumberRange("x", "", 2, 1000);
        assertThat(range.allocate()).isEqualTo("1000");
    }

    @Test
    void rejectsInvalidArguments() {
        assertThatThrownBy(() -> new NumberRange(" ", "RE-", 6, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NumberRange("x", "RE-", -1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NumberRange("x", "RE-", 6, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
