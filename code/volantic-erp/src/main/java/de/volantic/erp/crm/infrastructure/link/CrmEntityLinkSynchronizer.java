package de.volantic.erp.crm.infrastructure.link;

import de.volantic.erp.core.entitylink.EntityLinkRegistry;
import de.volantic.erp.core.entitylink.EntityRef;
import de.volantic.erp.crm.domain.event.PartnerAddressLinked;
import de.volantic.erp.crm.domain.event.PartnerAddressUnlinked;
import de.volantic.erp.crm.domain.event.PartnerContactLinked;
import de.volantic.erp.crm.domain.event.PartnerContactUnlinked;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Keeps the 360° entity-link graph in sync with CRM changes. Listens to CRM domain events and records
 * the corresponding edges via the core {@link EntityLinkRegistry} (allowed: core is an OPEN module).
 *
 * <p>Uses a synchronous {@link TransactionalEventListener} firing AFTER_COMMIT — so the link is only
 * recorded once the contact/address actually persisted, and recording it never rolls back the CRM
 * write. After commit there is no active transaction, so the {@code @Transactional} EntityLinkService
 * opens a fresh one for the link write. This is the "maintained via domain events" mechanism (DB arch
 * §5.3), kept synchronous to avoid the async event-registry schema and stay deterministically testable.
 */
@Component
class CrmEntityLinkSynchronizer {

    static final String LINK_HAS_CONTACT = "HAS_CONTACT";
    static final String LINK_HAS_ADDRESS = "HAS_ADDRESS";
    static final String TYPE_CONTACT = "crm.contact";
    static final String TYPE_ADDRESS = "crm.address";

    private final EntityLinkRegistry links;

    CrmEntityLinkSynchronizer(EntityLinkRegistry links) {
        this.links = links;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(PartnerContactLinked event) {
        links.link(ownerRef(event.owner()), EntityRef.of(TYPE_CONTACT, event.contactId().value()), LINK_HAS_CONTACT);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(PartnerContactUnlinked event) {
        links.unlink(ownerRef(event.owner()), EntityRef.of(TYPE_CONTACT, event.contactId().value()), LINK_HAS_CONTACT);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(PartnerAddressLinked event) {
        links.link(ownerRef(event.owner()), EntityRef.of(TYPE_ADDRESS, event.addressId().value()), LINK_HAS_ADDRESS);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(PartnerAddressUnlinked event) {
        links.unlink(ownerRef(event.owner()), EntityRef.of(TYPE_ADDRESS, event.addressId().value()), LINK_HAS_ADDRESS);
    }

    /** Maps the CRM partner reference to the entity-link type string, e.g. {@code crm.customer}. */
    private static EntityRef ownerRef(PartnerRef owner) {
        String type = owner.type() == PartnerType.CUSTOMER ? "crm.customer" : "crm.supplier";
        return EntityRef.of(type, owner.id());
    }
}
