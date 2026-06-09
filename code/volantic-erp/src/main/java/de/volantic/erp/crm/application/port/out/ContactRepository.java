package de.volantic.erp.crm.application.port.out;

import de.volantic.erp.crm.domain.model.Contact;
import de.volantic.erp.crm.domain.model.ContactId;
import de.volantic.erp.crm.domain.model.PartnerRef;

import java.util.List;
import java.util.Optional;

/** Outbound port for contact persistence. */
public interface ContactRepository {

    Contact save(Contact contact);

    Optional<Contact> findById(ContactId id);

    List<Contact> findByOwner(PartnerRef owner);

    boolean deleteById(ContactId id);
}
