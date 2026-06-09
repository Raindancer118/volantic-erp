package de.volantic.erp.crm.domain.event;

import de.volantic.erp.crm.domain.model.AddressId;
import de.volantic.erp.crm.domain.model.PartnerRef;

/** Published when an address is removed from a partner; drives the 360° entity-link graph. */
public record PartnerAddressUnlinked(PartnerRef owner, AddressId addressId) {
}
