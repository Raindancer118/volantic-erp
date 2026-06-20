package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.crm.application.port.out.ContactRepository;
import de.volantic.erp.crm.domain.model.Contact;
import de.volantic.erp.crm.domain.model.ContactId;
import de.volantic.erp.crm.domain.model.PartnerRef;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Outbound adapter for {@link ContactRepository}: maps between domain {@link Contact} and JPA. */
@Component
class ContactRepositoryAdapter implements ContactRepository {

    private final ContactJpaRepository jpa;

    ContactRepositoryAdapter(ContactJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Contact save(Contact contact) {
        ContactEntity entity = jpa.findById(contact.id().value())
                .orElseGet(() -> new ContactEntity(
                        contact.id().value(), contact.owner().type(), contact.owner().id(),
                        contact.firstName(), contact.lastName(), contact.email(), contact.phone()));
        entity.apply(contact.firstName(), contact.lastName(), contact.email(), contact.phone());
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Contact> findById(ContactId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Page<Contact> findByOwner(PartnerRef owner, Pageable pageable) {
        return jpa.findByOwnerTypeAndOwnerId(owner.type(), owner.id(), pageable).map(this::toDomain);
    }

    @Override
    public boolean deleteById(ContactId id) {
        if (!jpa.existsById(id.value())) {
            return false;
        }
        jpa.deleteById(id.value());
        return true;
    }

    @Override
    public List<ContactId> findIds(String firstName, String lastName, String email) {
        return jpa.findIdsByFilter(firstName, lastName, email).stream().map(ContactId::new).toList();
    }

    private Contact toDomain(ContactEntity entity) {
        return Contact.reconstitute(
                new ContactId(entity.getId()),
                PartnerRef.of(entity.ownerType(), entity.ownerId()),
                entity.firstName(), entity.lastName(), entity.email(), entity.phone());
    }
}
