package de.volantic.erp.workflow.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Invariants of the {@link ApprovalSubject} value object (pure domain, no Spring). */
class ApprovalSubjectTest {

    @Test
    void trimsTypeAndId() {
        ApprovalSubject subject = new ApprovalSubject("  purchase-order ", " 42 ");

        assertThat(subject.type()).isEqualTo("purchase-order");
        assertThat(subject.id()).isEqualTo("42");
    }

    @Test
    void rejectsBlankType() {
        assertThatThrownBy(() -> new ApprovalSubject("  ", "42"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankId() {
        assertThatThrownBy(() -> new ApprovalSubject("purchase-order", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
