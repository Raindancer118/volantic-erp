package de.volantic.erp.crm.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure domain tests of the {@link Customer} aggregate — identity, invariants, mutators. */
class CustomerTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void createAssignsIdentityAndNormalisesEmail() {
        Customer customer = Customer.create(ORG, "C-1001", "  ACME GmbH ", "Info@ACME.de");

        assertThat(customer.id()).isNotNull();
        assertThat(customer.id().value()).isNotNull();
        assertThat(customer.orgUnitId()).isEqualTo(ORG);
        assertThat(customer.customerNumber()).isEqualTo("C-1001");
        assertThat(customer.name()).isEqualTo("ACME GmbH");
        assertThat(customer.email()).isEqualTo("info@acme.de");
    }

    @Test
    void blankNameOrNumberIsRejected() {
        assertThatThrownBy(() -> Customer.create(ORG, "C-1", "  ", "a@b.de"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Customer.create(ORG, " ", "ACME", "a@b.de"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingOrgUnitIsRejected() {
        assertThatThrownBy(() -> Customer.create(null, "C-1", "ACME", "a@b.de"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidEmailIsRejectedAndEmptyBecomesNull() {
        assertThatThrownBy(() -> Customer.create(ORG, "C-1", "ACME", "not-an-email"))
                .isInstanceOf(IllegalArgumentException.class);
        // Syntactically broken addresses a bare contains("@") would have let through.
        assertThatThrownBy(() -> Customer.create(ORG, "C-1", "ACME", "a@b"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Customer.create(ORG, "C-1", "ACME", "a@@b.de"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(Customer.create(ORG, "C-1", "ACME", "  ").email()).isNull();
    }

    @Test
    void renameAndChangeEmailMutate() {
        Customer customer = Customer.create(ORG, "C-1", "ACME", "a@acme.de");

        customer.rename("ACME AG");
        customer.changeEmail("contact@acme.de");

        assertThat(customer.name()).isEqualTo("ACME AG");
        assertThat(customer.email()).isEqualTo("contact@acme.de");
        assertThat(customer.customerNumber()).isEqualTo("C-1"); // immutable
    }
}
