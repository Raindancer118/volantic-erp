package de.volantic.erp.crm.infrastructure.link;

import de.volantic.erp.core.entitylink.EntityLinkRegistry;
import de.volantic.erp.core.entitylink.EntityRef;
import de.volantic.erp.crm.domain.event.PartnerAddressLinked;
import de.volantic.erp.crm.domain.event.PartnerAddressUnlinked;
import de.volantic.erp.crm.domain.event.PartnerContactLinked;
import de.volantic.erp.crm.domain.event.PartnerContactUnlinked;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Keeps the 360° entity-link graph in sync with CRM changes. Listens to CRM domain events and records
 * the corresponding edges via the core {@link EntityLinkRegistry} (allowed: core is an OPEN module).
 *
 * <p>Uses {@link ApplicationModuleListener}: the publication is persisted in the event publication
 * registry (Outbox) before delivery and marked complete afterwards, so the link is durable — a crash
 * between the CRM commit and recording the link leaves an incomplete publication that is retried,
 * rather than silently lost. Each handler runs in its own transaction after the publisher commits, so
 * recording the link never rolls back the CRM write. This is the "maintained via domain events"
 * mechanism (DB architecture §5.3).
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

    @ApplicationModuleListener
    void on(PartnerContactLinked event) {
        links.link(ownerRef(event.owner()), EntityRef.of(TYPE_CONTACT, event.contactId().value()), LINK_HAS_CONTACT);
    }

    @ApplicationModuleListener
    void on(PartnerContactUnlinked event) {
        links.unlink(ownerRef(event.owner()), EntityRef.of(TYPE_CONTACT, event.contactId().value()), LINK_HAS_CONTACT);
    }

    @ApplicationModuleListener
    void on(PartnerAddressLinked event) {
        links.link(ownerRef(event.owner()), EntityRef.of(TYPE_ADDRESS, event.addressId().value()), LINK_HAS_ADDRESS);
    }

    @ApplicationModuleListener
    void on(PartnerAddressUnlinked event) {
        links.unlink(ownerRef(event.owner()), EntityRef.of(TYPE_ADDRESS, event.addressId().value()), LINK_HAS_ADDRESS);
    }

    /** Maps the CRM partner reference to the entity-link type string, e.g. {@code crm.customer}. */
    private static EntityRef ownerRef(PartnerRef owner) {
        String type = owner.type() == PartnerType.CUSTOMER ? "crm.customer" : "crm.supplier";
        return EntityRef.of(type, owner.id());
    }
}
