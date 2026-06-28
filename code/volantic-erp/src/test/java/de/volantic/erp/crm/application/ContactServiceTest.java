package de.volantic.erp.crm.application;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.crm.application.port.out.ContactRepository;
import de.volantic.erp.crm.domain.event.PartnerContactLinked;
import de.volantic.erp.crm.domain.event.PartnerContactUnlinked;
import de.volantic.erp.crm.domain.model.Contact;
import de.volantic.erp.crm.domain.model.ContactId;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
import de.volantic.erp.security.ScopeEnforcer;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Contact use-case logic incl. 360° event publication and inherited org-unit scope, over mocked deps. */
class ContactServiceTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final ContactRepository repository = mock(ContactRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final PartnerOrgUnits partnerOrgUnits = mock(PartnerOrgUnits.class);
    private final ScopeEnforcer scopeEnforcer = mock(ScopeEnforcer.class);
    private final ContactService service = new ContactService(repository, events, partnerOrgUnits, scopeEnforcer);

    private final PartnerRef owner = PartnerRef.of(PartnerType.CUSTOMER, UuidV7.randomUuid());

    @Test
    void createPersistsAndPublishesLinkEvent() {
        when(partnerOrgUnits.of(owner)).thenReturn(ORG);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Contact created = service.createContact(owner, "Erika", "Mustermann", "e@acme.de", null);

        verify(scopeEnforcer).require("crm.contact:write", ORG);
        verify(repository).save(any(Contact.class));
        verify(events).publishEvent(new PartnerContactLinked(owner, created.id()));
    }

    @Test
    void createDeniedOutOfOwnersScopeWritesNothing() {
        when(partnerOrgUnits.of(owner)).thenReturn(ORG);
        doThrow(new AccessDeniedException("out of scope")).when(scopeEnforcer).require("crm.contact:write", ORG);

        assertThatThrownBy(() -> service.createContact(owner, "Erika", "Mustermann", "e@acme.de", null))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void deletePublishesUnlinkEvent() {
        Contact contact = Contact.create(owner, "Erika", "Mustermann", "e@acme.de", null);
        when(repository.findById(contact.id())).thenReturn(Optional.of(contact));
        when(partnerOrgUnits.of(owner)).thenReturn(ORG);
        when(repository.deleteById(contact.id())).thenReturn(true);

        service.deleteContact(contact.id());

        verify(scopeEnforcer).require("crm.contact:write", ORG);
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
