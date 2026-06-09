package de.volantic.erp.core.entitylink.application;

import de.volantic.erp.core.entitylink.EntityLink;
import de.volantic.erp.core.entitylink.EntityRef;
import de.volantic.erp.core.entitylink.application.port.out.EntityLinkStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Orchestration of {@link EntityLinkService} over the mocked {@link EntityLinkStore} port. */
class EntityLinkServiceTest {

    private final EntityLinkStore store = mock(EntityLinkStore.class);
    private final EntityLinkService service = new EntityLinkService(store);

    private final EntityRef customer = EntityRef.of("crm.customer", UUID.randomUUID());
    private final EntityRef contact = EntityRef.of("crm.contact", UUID.randomUUID());

    @Test
    void linkDelegatesToStore() {
        service.link(customer, contact, "HAS_CONTACT");

        verify(store).add(new EntityLink(customer, contact, "HAS_CONTACT"));
    }

    @Test
    void unlinkDelegatesToStore() {
        service.unlink(customer, contact, "HAS_CONTACT");

        verify(store).remove(new EntityLink(customer, contact, "HAS_CONTACT"));
    }

    @Test
    void outgoingReturnsStoreResult() {
        EntityLink link = new EntityLink(customer, contact, "HAS_CONTACT");
        when(store.findOutgoing(customer)).thenReturn(List.of(link));

        assertThat(service.outgoing(customer)).containsExactly(link);
    }
}
