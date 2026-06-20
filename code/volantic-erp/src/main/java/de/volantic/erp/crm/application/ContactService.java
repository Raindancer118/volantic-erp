package de.volantic.erp.crm.application;

import de.volantic.erp.crm.application.port.out.ContactRepository;
import de.volantic.erp.crm.domain.event.PartnerContactLinked;
import de.volantic.erp.crm.domain.event.PartnerContactUnlinked;
import de.volantic.erp.crm.domain.model.Contact;
import de.volantic.erp.crm.domain.model.ContactId;
import de.volantic.erp.crm.domain.model.PartnerRef;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Contact use cases. Authorization enforced here at the service boundary (ADR-0004). */
@Service
public class ContactService {

    private final ContactRepository contacts;
    private final ApplicationEventPublisher events;

    ContactService(ContactRepository contacts, ApplicationEventPublisher events) {
        this.contacts = contacts;
        this.events = events;
    }

    @Transactional
    @PreAuthorize("hasPermission(null, 'crm.contact:write')")
    public Contact createContact(PartnerRef owner, String firstName, String lastName, String email, String phone) {
        Contact contact = contacts.save(Contact.create(owner, firstName, lastName, email, phone));
        events.publishEvent(new PartnerContactLinked(owner, contact.id()));
        return contact;
    }

    // noRollbackFor: a not-found read must not poison a surrounding transaction (see CustomerService).
    @Transactional(readOnly = true, noRollbackFor = ContactNotFoundException.class)
    @PreAuthorize("hasPermission(null, 'crm.contact:read')")
    public Contact getContact(ContactId id) {
        return contacts.findById(id).orElseThrow(() -> new ContactNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'crm.contact:read')")
    public Page<Contact> listContacts(PartnerRef owner, Pageable pageable) {
        return contacts.findByOwner(owner, pageable);
    }

    /** Resolves a selection filter (first/last name and/or email, exact match) to matching contact ids. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'crm.contact:read')")
    public List<ContactId> findContactIds(String firstName, String lastName, String email) {
        return contacts.findIds(firstName, lastName, email);
    }

    @Transactional
    @PreAuthorize("hasPermission(null, 'crm.contact:write')")
    public Contact updateContact(ContactId id, String firstName, String lastName, String email, String phone) {
        Contact contact = contacts.findById(id).orElseThrow(() -> new ContactNotFoundException(id));
        contact.change(firstName, lastName, email, phone);
        return contacts.save(contact);
    }

    @Transactional
    @PreAuthorize("hasPermission(null, 'crm.contact:write')")
    public void deleteContact(ContactId id) {
        PartnerRef owner = contacts.findById(id).orElseThrow(() -> new ContactNotFoundException(id)).owner();
        if (!contacts.deleteById(id)) {
            throw new ContactNotFoundException(id);
        }
        events.publishEvent(new PartnerContactUnlinked(owner, id));
    }
}
