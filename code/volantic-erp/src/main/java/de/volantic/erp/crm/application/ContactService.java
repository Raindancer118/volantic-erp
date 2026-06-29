package de.volantic.erp.crm.application;

import de.volantic.erp.crm.application.port.out.ContactRepository;
import de.volantic.erp.crm.domain.event.PartnerContactLinked;
import de.volantic.erp.crm.domain.event.PartnerContactUnlinked;
import de.volantic.erp.crm.domain.model.Contact;
import de.volantic.erp.crm.domain.model.ContactId;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.security.ScopeEnforcer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Contact use cases. Authorization is enforced here at the service boundary (ADR-0004) with org-unit
 * scoping (ADR-0007): a contact has no org unit of its own — it <em>inherits</em> the unit of its owning
 * partner ({@link PartnerOrgUnits}). Every operation resolves the owner's unit and checks it via
 * {@link ScopeEnforcer}. A global grant still covers every unit. The bulk/change-set path calls these
 * same methods, so it inherits the checks.
 */
@Service
public class ContactService {

    private static final String PERM_READ  = "crm.contact:read";
    private static final String PERM_WRITE = "crm.contact:write";

    private final ContactRepository contacts;
    private final ApplicationEventPublisher events;
    private final PartnerOrgUnits partnerOrgUnits;
    private final ScopeEnforcer scopeEnforcer;

    ContactService(ContactRepository contacts, ApplicationEventPublisher events,
                   PartnerOrgUnits partnerOrgUnits, ScopeEnforcer scopeEnforcer) {
        this.contacts = contacts;
        this.events = events;
        this.partnerOrgUnits = partnerOrgUnits;
        this.scopeEnforcer = scopeEnforcer;
    }

    @Transactional
    public Contact createContact(PartnerRef owner, String firstName, String lastName, String email, String phone) {
        scopeEnforcer.require(PERM_WRITE, partnerOrgUnits.of(owner));
        Contact contact = contacts.save(Contact.create(owner, firstName, lastName, email, phone));
        events.publishEvent(new PartnerContactLinked(owner, contact.id()));
        return contact;
    }

    // noRollbackFor: a not-found read must not poison a surrounding transaction (see CustomerService).
    @Transactional(readOnly = true, noRollbackFor = ContactNotFoundException.class)
    public Contact getContact(ContactId id) {
        Contact contact = contacts.findById(id).orElseThrow(() -> new ContactNotFoundException(id));
        scopeEnforcer.require(PERM_READ, partnerOrgUnits.of(contact.owner()));
        return contact;
    }

    @Transactional(readOnly = true)
    public Page<Contact> listContacts(PartnerRef owner, Pageable pageable) {
        scopeEnforcer.require(PERM_READ, partnerOrgUnits.of(owner));
        return contacts.findByOwner(owner, pageable);
    }

    /**
     * Resolves a selection filter (first/last name and/or email, exact match) to matching contact ids —
     * the bulk-edit selection path. Per-instance scope is enforced when each selected contact is updated.
     */
    @Transactional(readOnly = true)
    public List<ContactId> findContactIds(String firstName, String lastName, String email) {
        scopeEnforcer.requireAnywhere(PERM_READ);
        return contacts.findIds(firstName, lastName, email);
    }

    /** Update with an explicit optimistic-lock check (REST CRUD via ETag/If-Match). */
    @Transactional
    public Contact updateContact(ContactId id, long expectedVersion,
                                 String firstName, String lastName, String email, String phone) {
        Contact contact = contacts.findById(id).orElseThrow(() -> new ContactNotFoundException(id));
        scopeEnforcer.require(PERM_WRITE, partnerOrgUnits.of(contact.owner()));
        de.volantic.erp.core.OptimisticLock.check(contact.version(), expectedVersion, id);
        contact.change(firstName, lastName, email, phone);
        return contacts.save(contact);
    }

    /** Update without an explicit version — internal/bulk callers; still version-safe within the tx. */
    @Transactional
    public Contact updateContact(ContactId id, String firstName, String lastName, String email, String phone) {
        Contact contact = contacts.findById(id).orElseThrow(() -> new ContactNotFoundException(id));
        scopeEnforcer.require(PERM_WRITE, partnerOrgUnits.of(contact.owner()));
        contact.change(firstName, lastName, email, phone);
        return contacts.save(contact);
    }

    /**
     * Re-creates a previously deleted contact with its original id (Rollback Engine compensation of a
     * DELETE). Re-publishes the link event so the 360° graph edge is restored.
     */
    @Transactional
    public Contact recreateContact(ContactId id, PartnerRef owner,
                                   String firstName, String lastName, String email, String phone) {
        scopeEnforcer.require(PERM_WRITE, partnerOrgUnits.of(owner));
        Contact contact = contacts.save(Contact.reconstitute(id, owner, firstName, lastName, email, phone));
        events.publishEvent(new PartnerContactLinked(owner, contact.id()));
        return contact;
    }

    @Transactional
    public void deleteContact(ContactId id) {
        PartnerRef owner = contacts.findById(id).orElseThrow(() -> new ContactNotFoundException(id)).owner();
        scopeEnforcer.require(PERM_WRITE, partnerOrgUnits.of(owner));
        if (!contacts.deleteById(id)) {
            throw new ContactNotFoundException(id);
        }
        events.publishEvent(new PartnerContactUnlinked(owner, id));
    }
}
