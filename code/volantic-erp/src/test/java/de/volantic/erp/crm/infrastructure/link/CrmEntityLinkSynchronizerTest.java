package de.volantic.erp.crm.infrastructure.link;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.core.entitylink.EntityLinkRegistry;
import de.volantic.erp.core.entitylink.EntityRef;
import de.volantic.erp.crm.domain.event.PartnerAddressLinked;
import de.volantic.erp.crm.domain.event.PartnerContactLinked;
import de.volantic.erp.crm.domain.event.PartnerContactUnlinked;
import de.volantic.erp.crm.domain.model.AddressId;
import de.volantic.erp.crm.domain.model.ContactId;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Maps CRM domain events onto the core {@link EntityLinkRegistry} with correct type strings. */
class CrmEntityLinkSynchronizerTest {

    private final EntityLinkRegistry registry = mock(EntityLinkRegistry.class);
    private final CrmEntityLinkSynchronizer sync = new CrmEntityLinkSynchronizer(registry);

    @Test
    void customerContactLinkRecordsCrmCustomerHasContact() {
        UUID customerId = UuidV7.randomUuid();
        ContactId contactId = new ContactId(UuidV7.randomUuid());
        sync.on(new PartnerContactLinked(PartnerRef.of(PartnerType.CUSTOMER, customerId), contactId));

        verify(registry).link(
                EntityRef.of("crm.customer", customerId),
                EntityRef.of("crm.contact", contactId.value()),
                "HAS_CONTACT");
    }

    @Test
    void supplierAddressLinkRecordsCrmSupplierHasAddress() {
        UUID supplierId = UuidV7.randomUuid();
        AddressId addressId = new AddressId(UuidV7.randomUuid());
        sync.on(new PartnerAddressLinked(PartnerRef.of(PartnerType.SUPPLIER, supplierId), addressId));

        verify(registry).link(
                EntityRef.of("crm.supplier", supplierId),
                EntityRef.of("crm.address", addressId.value()),
                "HAS_ADDRESS");
    }

    @Test
    void contactUnlinkRemovesTheEdge() {
        UUID customerId = UuidV7.randomUuid();
        ContactId contactId = new ContactId(UuidV7.randomUuid());
        sync.on(new PartnerContactUnlinked(PartnerRef.of(PartnerType.CUSTOMER, customerId), contactId));

        verify(registry).unlink(
                EntityRef.of("crm.customer", customerId),
                EntityRef.of("crm.contact", contactId.value()),
                "HAS_CONTACT");
    }
}
