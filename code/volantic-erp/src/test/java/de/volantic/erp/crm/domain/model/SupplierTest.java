package de.volantic.erp.crm.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure domain tests of the {@link Supplier} aggregate. */
class SupplierTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void createAssignsIdentityAndNormalisesEmail() {
        Supplier supplier = Supplier.create(ORG, "S-1001", " Globex ", "Sales@Globex.de");

        assertThat(supplier.id().value()).isNotNull();
        assertThat(supplier.orgUnitId()).isEqualTo(ORG);
        assertThat(supplier.supplierNumber()).isEqualTo("S-1001");
        assertThat(supplier.name()).isEqualTo("Globex");
        assertThat(supplier.email()).isEqualTo("sales@globex.de");
    }

    @Test
    void invalidInputIsRejected() {
        assertThatThrownBy(() -> Supplier.create(ORG, "S-1", "  ", "a@b.de")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Supplier.create(ORG, "S-1", "Globex", "no-at")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingOrgUnitIsRejected() {
        assertThatThrownBy(() -> Supplier.create(null, "S-1", "Globex", "a@b.de"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emailValidationMatchesTheSharedCrmRule() {
        // Supplier must apply the exact same e-mail rule as Customer/Contact (EmailAddresses), not a
        // weaker contains("@") check: a domain without a dot is garbage and must be rejected.
        assertThatThrownBy(() -> Supplier.create(ORG, "S-1", "Globex", "a@b"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Supplier.create(ORG, "S-1", "Globex", "a@"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankEmailIsAllowedAndStoredAsNull() {
        assertThat(Supplier.create(ORG, "S-1", "Globex", "  ").email()).isNull();
    }

    @Test
    void renameAndChangeEmailMutate() {
        Supplier supplier = Supplier.create(ORG, "S-1", "Globex", "a@globex.de");

        supplier.rename("Globex Corp");
        supplier.changeEmail("contact@globex.de");

        assertThat(supplier.name()).isEqualTo("Globex Corp");
        assertThat(supplier.email()).isEqualTo("contact@globex.de");
        assertThat(supplier.supplierNumber()).isEqualTo("S-1");
    }
}
