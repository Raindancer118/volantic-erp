package de.volantic.erp.crm.application;

import de.volantic.erp.crm.application.port.out.ContactRepository;
import de.volantic.erp.crm.domain.model.Contact;
import de.volantic.erp.crm.domain.model.ContactId;
import de.volantic.erp.crm.domain.model.PartnerRef;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Contact use cases. Authorization enforced here at the service boundary (ADR-0004). */
@Service
public class ContactService {

    private final ContactRepository contacts;

    ContactService(ContactRepository contacts) {
        this.contacts = contacts;
    }

    @Transactional
    @PreAuthorize("hasPermission(null, 'crm.contact:write')")
    public Contact createContact(PartnerRef owner, String firstName, String lastName, String email, String phone) {
        return contacts.save(Contact.create(owner, firstName, lastName, email, phone));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'crm.contact:read')")
    public Contact getContact(ContactId id) {
        return contacts.findById(id).orElseThrow(() -> new ContactNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'crm.contact:read')")
    public List<Contact> listContacts(PartnerRef owner) {
        return contacts.findByOwner(owner);
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
        if (!contacts.deleteById(id)) {
            throw new ContactNotFoundException(id);
        }
    }
}
