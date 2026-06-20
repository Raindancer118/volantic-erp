package de.volantic.erp.crm.infrastructure.revision;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.core.revision.AbstractFieldMapHandler;
import de.volantic.erp.crm.application.ContactService;
import de.volantic.erp.crm.application.CrmNotFoundException;
import de.volantic.erp.crm.domain.model.Contact;
import de.volantic.erp.crm.domain.model.ContactId;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * {@code core.revision} handler for {@link Contact} (resource type {@code crm.contact}). The contact's
 * owner ({@code PartnerRef}) is immutable and therefore not bulk-editable; only the person's own fields
 * are. Reads/writes run through {@link ContactService}.
 */
@Component
class ContactBulkHandler extends AbstractFieldMapHandler {

    static final String TYPE = "crm.contact";
    static final String FIELD_FIRST_NAME = "firstName";
    static final String FIELD_LAST_NAME = "lastName";
    static final String FIELD_EMAIL = "email";
    static final String FIELD_PHONE = "phone";

    private final ContactService contacts;

    ContactBulkHandler(ContactService contacts, ObjectMapper json) {
        super(json);
        this.contacts = contacts;
    }

    @Override
    public String resourceType() {
        return TYPE;
    }

    @Override
    public Set<String> editableFields() {
        return Set.of(FIELD_FIRST_NAME, FIELD_LAST_NAME, FIELD_EMAIL, FIELD_PHONE);
    }

    @Override
    protected Map<String, String> readFields(UUID id) {
        Contact contact;
        try {
            contact = contacts.getContact(new ContactId(id));
        } catch (CrmNotFoundException notFound) {
            return null;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(FIELD_FIRST_NAME, contact.firstName());
        fields.put(FIELD_LAST_NAME, contact.lastName());
        fields.put(FIELD_EMAIL, contact.email());
        fields.put(FIELD_PHONE, contact.phone());
        return fields;
    }

    @Override
    protected void writeFields(UUID id, Map<String, String> fields) {
        contacts.updateContact(new ContactId(id),
                fields.get(FIELD_FIRST_NAME), fields.get(FIELD_LAST_NAME),
                fields.get(FIELD_EMAIL), fields.get(FIELD_PHONE));
    }
}
