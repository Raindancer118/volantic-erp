package de.volantic.erp.crm.application.port.out;

import de.volantic.erp.crm.domain.model.Contact;
import de.volantic.erp.crm.domain.model.ContactId;
import de.volantic.erp.crm.domain.model.PartnerRef;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/** Outbound port for contact persistence. */
public interface ContactRepository {

    Contact save(Contact contact);

    Optional<Contact> findById(ContactId id);

    Page<Contact> findByOwner(PartnerRef owner, Pageable pageable);

    boolean deleteById(ContactId id);
}
