package de.volantic.erp.crm.application;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.crm.application.port.out.ContactRepository;
import de.volantic.erp.crm.domain.event.PartnerContactLinked;
import de.volantic.erp.crm.domain.event.PartnerContactUnlinked;
import de.volantic.erp.crm.domain.model.Contact;
import de.volantic.erp.crm.domain.model.ContactId;
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

/** Contact use-case logic incl. 360° event publication, over mocked port + event publisher. */
class ContactServiceTest {

    private final ContactRepository repository = mock(ContactRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ContactService service = new ContactService(repository, events);

    private final PartnerRef owner = PartnerRef.of(PartnerType.CUSTOMER, UuidV7.randomUuid());

    @Test
    void createPersistsAndPublishesLinkEvent() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Contact created = service.createContact(owner, "Erika", "Mustermann", "e@acme.de", null);

        verify(repository).save(any(Contact.class));
        verify(events).publishEvent(new PartnerContactLinked(owner, created.id()));
    }

    @Test
    void deletePublishesUnlinkEvent() {
        Contact contact = Contact.create(owner, "Erika", "Mustermann", "e@acme.de", null);
        when(repository.findById(contact.id())).thenReturn(Optional.of(contact));
        when(repository.deleteById(contact.id())).thenReturn(true);

        service.deleteContact(contact.id());

        verify(events).publishEvent(new PartnerContactUnlinked(owner, contact.id()));
    }

    @Test
    void deleteMissingThrowsAndPublishesNothing() {
        ContactId id = new ContactId(UuidV7.randomUuid());
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteContact(id)).isInstanceOf(ContactNotFoundException.class);
        verify(events, never()).publishEvent(any());
    }
}
