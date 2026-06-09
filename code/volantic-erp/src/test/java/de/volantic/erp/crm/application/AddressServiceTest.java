package de.volantic.erp.crm.application;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.crm.application.port.out.AddressRepository;
import de.volantic.erp.crm.domain.event.PartnerAddressLinked;
import de.volantic.erp.crm.domain.event.PartnerAddressUnlinked;
import de.volantic.erp.crm.domain.model.Address;
import de.volantic.erp.crm.domain.model.AddressId;
import de.volantic.erp.crm.domain.model.AddressType;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Address use-case logic incl. 360° event publication, over mocked port + event publisher. */
class AddressServiceTest {

    private final AddressRepository repository = mock(AddressRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final AddressService service = new AddressService(repository, events);

    private final PartnerRef owner = PartnerRef.of(PartnerType.SUPPLIER, UuidV7.randomUuid());

    @Test
    void createPersistsAndPublishesLinkEvent() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Address created = service.createAddress(owner, AddressType.BILLING, "Main St 1", "20095", "Hamburg", "DE");

        verify(repository).save(any(Address.class));
        verify(events).publishEvent(new PartnerAddressLinked(owner, created.id()));
    }

    @Test
    void deletePublishesUnlinkEvent() {
        Address address = Address.create(owner, AddressType.BILLING, "Main St 1", "20095", "Hamburg", "DE");
        when(repository.findById(address.id())).thenReturn(Optional.of(address));
        when(repository.deleteById(address.id())).thenReturn(true);

        service.deleteAddress(address.id());

        verify(events).publishEvent(new PartnerAddressUnlinked(owner, address.id()));
    }

    @Test
    void deleteMissingThrowsAndPublishesNothing() {
        AddressId id = new AddressId(UuidV7.randomUuid());
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteAddress(id)).isInstanceOf(AddressNotFoundException.class);
        verify(events, never()).publishEvent(any());
    }
}
