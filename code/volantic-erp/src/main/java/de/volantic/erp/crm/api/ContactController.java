package de.volantic.erp.crm.api;

import de.volantic.erp.core.web.ETags;
import de.volantic.erp.core.web.PageResponse;
import de.volantic.erp.crm.application.ContactService;
import de.volantic.erp.crm.domain.model.Contact;
import de.volantic.erp.crm.domain.model.ContactId;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/** REST v1 endpoints for partner contacts. {@link ContactService} enforces authorization. */
@RestController
@RequestMapping("/v1/crm/contacts")
class ContactController {

    private final ContactService contacts;

    ContactController(ContactService contacts) {
        this.contacts = contacts;
    }

    @PostMapping
    ResponseEntity<ContactResponse> create(@Valid @RequestBody ContactRequest request) {
        Contact created = contacts.createContact(
                PartnerRef.of(request.ownerType(), request.ownerId()),
                request.firstName(), request.lastName(), request.email(), request.phone());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id().value()).toUri();
        ContactResponse body = ContactResponse.from(created);
        return ResponseEntity.created(location).eTag(ETags.format(body.version())).body(body);
    }

    @GetMapping("/{id}")
    ResponseEntity<ContactResponse> getById(@PathVariable UUID id) {
        ContactResponse body = ContactResponse.from(contacts.getContact(new ContactId(id)));
        return ResponseEntity.ok().eTag(ETags.format(body.version())).body(body);
    }

    /** Lists contacts of one owner, e.g. {@code GET /v1/crm/contacts?ownerType=CUSTOMER&ownerId=...}. */
    @GetMapping
    PageResponse<ContactResponse> listByOwner(@RequestParam PartnerType ownerType,
                                              @RequestParam UUID ownerId,
                                              @PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(
                contacts.listContacts(PartnerRef.of(ownerType, ownerId), pageable), ContactResponse::from);
    }

    @PutMapping("/{id}")
    ResponseEntity<ContactResponse> update(@PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody UpdateContactRequest request) {
        Contact updated = contacts.updateContact(new ContactId(id), ETags.parseIfMatch(ifMatch),
                request.firstName(), request.lastName(), request.email(), request.phone());
        ContactResponse body = ContactResponse.from(updated);
        return ResponseEntity.ok().eTag(ETags.format(body.version())).body(body);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        contacts.deleteContact(new ContactId(id));
        return ResponseEntity.noContent().build();
    }
}
