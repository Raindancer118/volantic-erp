package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.crm.application.port.out.AddressRepository;
import de.volantic.erp.crm.application.port.out.ContactRepository;
import de.volantic.erp.crm.domain.model.Address;
import de.volantic.erp.crm.domain.model.AddressType;
import de.volantic.erp.crm.domain.model.Contact;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence IT for the address and contact adapters against a real PostgreSQL (Testcontainers):
 * owner-scoped queries, update and delete. Skipped without Docker.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AddressRepositoryAdapter.class, ContactRepositoryAdapter.class})
@Testcontainers(disabledWithoutDocker = true)
class AddressContactRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private AddressRepository addresses;

    @Autowired
    private ContactRepository contacts;

    @Test
    void addressesAreScopedToTheirOwner() {
        PartnerRef customer = PartnerRef.of(PartnerType.CUSTOMER, UuidV7.randomUuid());
        PartnerRef supplier = PartnerRef.of(PartnerType.SUPPLIER, UuidV7.randomUuid());
        addresses.save(Address.create(customer, AddressType.BILLING, "Main St 1", "20095", "Hamburg", "DE"));
        addresses.save(Address.create(customer, AddressType.SHIPPING, "Side St 2", "10115", "Berlin", "DE"));
        addresses.save(Address.create(supplier, AddressType.DEFAULT, "Ind. Rd 9", "80331", "Munich", "DE"));

        assertThat(addresses.findByOwner(customer)).hasSize(2);
        assertThat(addresses.findByOwner(supplier)).hasSize(1);
    }

    @Test
    void addressUpdateAndDelete() {
        PartnerRef customer = PartnerRef.of(PartnerType.CUSTOMER, UuidV7.randomUuid());
        Address address = addresses.save(
                Address.create(customer, AddressType.BILLING, "Main St 1", "20095", "Hamburg", "DE"));

        address.change(AddressType.SHIPPING, "New St 3", "50667", "Cologne", "DE");
        addresses.save(address);
        assertThat(addresses.findById(address.id()).orElseThrow().city()).isEqualTo("Cologne");

        assertThat(addresses.deleteById(address.id())).isTrue();
        assertThat(addresses.findById(address.id())).isEmpty();
        assertThat(addresses.deleteById(new de.volantic.erp.crm.domain.model.AddressId(UUID.randomUUID()))).isFalse();
    }

    @Test
    void contactsRoundtripAndOwnerScope() {
        PartnerRef customer = PartnerRef.of(PartnerType.CUSTOMER, UuidV7.randomUuid());
        Contact contact = contacts.save(Contact.create(customer, "Erika", "Mustermann", "e@acme.de", "+49 40 1"));

        assertThat(contacts.findByOwner(customer)).singleElement()
                .satisfies(c -> assertThat(c.lastName()).isEqualTo("Mustermann"));

        contact.change("Max", "Muster", "max@acme.de", null);
        contacts.save(contact);
        assertThat(contacts.findById(contact.id()).orElseThrow().firstName()).isEqualTo("Max");

        assertThat(contacts.deleteById(contact.id())).isTrue();
        assertThat(contacts.findByOwner(customer)).isEmpty();
    }
}
