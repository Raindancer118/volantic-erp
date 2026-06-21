package de.volantic.erp.crm.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure domain tests of the {@link Customer} aggregate — identity, invariants, mutators. */
class CustomerTest {

    @Test
    void createAssignsIdentityAndNormalisesEmail() {
        Customer customer = Customer.create("C-1001", "  ACME GmbH ", "Info@ACME.de");

        assertThat(customer.id()).isNotNull();
        assertThat(customer.id().value()).isNotNull();
        assertThat(customer.customerNumber()).isEqualTo("C-1001");
        assertThat(customer.name()).isEqualTo("ACME GmbH");
        assertThat(customer.email()).isEqualTo("info@acme.de");
    }

    @Test
    void blankNameOrNumberIsRejected() {
        assertThatThrownBy(() -> Customer.create("C-1", "  ", "a@b.de"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Customer.create(" ", "ACME", "a@b.de"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidEmailIsRejectedAndEmptyBecomesNull() {
        assertThatThrownBy(() -> Customer.create("C-1", "ACME", "not-an-email"))
                .isInstanceOf(IllegalArgumentException.class);
        // Syntactically broken addresses a bare contains("@") would have let through.
        assertThatThrownBy(() -> Customer.create("C-1", "ACME", "a@b"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Customer.create("C-1", "ACME", "a@@b.de"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(Customer.create("C-1", "ACME", "  ").email()).isNull();
    }

    @Test
    void renameAndChangeEmailMutate() {
        Customer customer = Customer.create("C-1", "ACME", "a@acme.de");

        customer.rename("ACME AG");
        customer.changeEmail("contact@acme.de");

        assertThat(customer.name()).isEqualTo("ACME AG");
        assertThat(customer.email()).isEqualTo("contact@acme.de");
        assertThat(customer.customerNumber()).isEqualTo("C-1"); // immutable
    }
}
