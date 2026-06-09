package de.volantic.erp.crm.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure domain tests of the {@link Supplier} aggregate. */
class SupplierTest {

    @Test
    void createAssignsIdentityAndNormalisesEmail() {
        Supplier supplier = Supplier.create("S-1001", " Globex ", "Sales@Globex.de");

        assertThat(supplier.id().value()).isNotNull();
        assertThat(supplier.supplierNumber()).isEqualTo("S-1001");
        assertThat(supplier.name()).isEqualTo("Globex");
        assertThat(supplier.email()).isEqualTo("sales@globex.de");
    }

    @Test
    void invalidInputIsRejected() {
        assertThatThrownBy(() -> Supplier.create("S-1", "  ", "a@b.de")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Supplier.create("S-1", "Globex", "no-at")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void renameAndChangeEmailMutate() {
        Supplier supplier = Supplier.create("S-1", "Globex", "a@globex.de");

        supplier.rename("Globex Corp");
        supplier.changeEmail("contact@globex.de");

        assertThat(supplier.name()).isEqualTo("Globex Corp");
        assertThat(supplier.email()).isEqualTo("contact@globex.de");
        assertThat(supplier.supplierNumber()).isEqualTo("S-1");
    }
}
