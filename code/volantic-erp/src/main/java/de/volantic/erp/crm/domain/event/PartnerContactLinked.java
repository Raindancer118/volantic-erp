package de.volantic.erp.crm.domain.event;

import de.volantic.erp.crm.domain.model.ContactId;
import de.volantic.erp.crm.domain.model.PartnerRef;

/** Published when a contact is added to a partner; drives the 360° entity-link graph. */
public record PartnerContactLinked(PartnerRef owner, ContactId contactId) {
}
