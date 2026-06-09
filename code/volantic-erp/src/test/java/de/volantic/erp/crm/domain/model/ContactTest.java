package de.volantic.erp.crm.domain.model;

import de.volantic.erp.core.UuidV7;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure domain tests of the {@link Contact} aggregate. */
class ContactTest {

    private final PartnerRef owner = PartnerRef.of(PartnerType.SUPPLIER, UuidV7.randomUuid());

    @Test
    void createNormalisesEmailAndAllowsBlankPhone() {
        Contact contact = Contact.create(owner, "Erika", "Mustermann", "Erika@ACME.de", "  ");

        assertThat(contact.id().value()).isNotNull();
        assertThat(contact.email()).isEqualTo("erika@acme.de");
        assertThat(contact.phone()).isNull();
    }

    @Test
    void rejectsBlankNamesAndBadEmail() {
        assertThatThrownBy(() -> Contact.create(owner, " ", "Mustermann", "a@b.de", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Contact.create(owner, "Erika", "Mustermann", "no-at", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeMutatesFields() {
        Contact contact = Contact.create(owner, "Erika", "Mustermann", "a@acme.de", "+49 40 1");

        contact.change("Max", "Muster", "max@acme.de", null);

        assertThat(contact.firstName()).isEqualTo("Max");
        assertThat(contact.lastName()).isEqualTo("Muster");
        assertThat(contact.email()).isEqualTo("max@acme.de");
        assertThat(contact.phone()).isNull();
    }
}
