package de.volantic.erp.crm.domain.model;

import de.volantic.erp.core.UuidV7;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure domain tests of the {@link Address} aggregate. */
class AddressTest {

    private final PartnerRef owner = PartnerRef.of(PartnerType.CUSTOMER, UuidV7.randomUuid());

    @Test
    void createNormalisesCountryAndDefaultsType() {
        Address address = Address.create(owner, null, "Main St 1", "20095", "Hamburg", "de");

        assertThat(address.id().value()).isNotNull();
        assertThat(address.owner()).isEqualTo(owner);
        assertThat(address.type()).isEqualTo(AddressType.DEFAULT);
        assertThat(address.countryCode()).isEqualTo("DE");
    }

    @Test
    void rejectsBlankFieldsAndBadCountry() {
        assertThatThrownBy(() -> Address.create(owner, AddressType.BILLING, " ", "20095", "HH", "DE"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Address.create(owner, AddressType.BILLING, "Main", "20095", "HH", "DEU"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeMutatesFields() {
        Address address = Address.create(owner, AddressType.BILLING, "Main St 1", "20095", "Hamburg", "DE");

        address.change(AddressType.SHIPPING, "Side St 2", "10115", "Berlin", "DE");

        assertThat(address.type()).isEqualTo(AddressType.SHIPPING);
        assertThat(address.city()).isEqualTo("Berlin");
    }
}
