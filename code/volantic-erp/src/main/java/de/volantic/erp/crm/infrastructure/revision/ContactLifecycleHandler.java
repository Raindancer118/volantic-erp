package de.volantic.erp.crm.infrastructure.revision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.core.revision.LifecycleResourceHandler;
import de.volantic.erp.crm.application.ContactService;
import de.volantic.erp.crm.application.CrmNotFoundException;
import de.volantic.erp.crm.domain.model.Contact;
import de.volantic.erp.crm.domain.model.ContactId;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@code core.revision} lifecycle handler for {@link Contact} (resource type {@code crm.contact}): bulk
 * create and delete with Rollback-Engine reversal (ADR-0006 §2). The full snapshot includes the immutable
 * owner so a deleted contact can be re-created with its original id. All operations go through
 * {@link ContactService}.
 */
@Component
class ContactLifecycleHandler implements LifecycleResourceHandler {

    private static final TypeReference<LinkedHashMap<String, String>> MAP = new TypeReference<>() {
    };
    private static final String OWNER_TYPE = "ownerType";
    private static final String OWNER_ID = "ownerId";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String EMAIL = "email";
    private static final String PHONE = "phone";

    private final ContactService contacts;
    private final ObjectMapper json;

    ContactLifecycleHandler(ContactService contacts, ObjectMapper json) {
        this.contacts = contacts;
        this.json = json;
    }

    @Override
    public String resourceType() {
        return "crm.contact";
    }

    @Override
    public UUID create(Map<String, String> data) {
        Contact created = contacts.createContact(ownerOf(data),
                data.get(FIRST_NAME), data.get(LAST_NAME), data.get(EMAIL), data.get(PHONE));
        return created.id().value();
    }

    @Override
    public String snapshot(UUID id) {
        Contact contact;
        try {
            contact = contacts.getContact(new ContactId(id));
        } catch (CrmNotFoundException notFound) {
            return null;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(OWNER_TYPE, contact.owner().type().name());
        fields.put(OWNER_ID, contact.owner().id().toString());
        fields.put(FIRST_NAME, contact.firstName());
        fields.put(LAST_NAME, contact.lastName());
        fields.put(EMAIL, contact.email());
        fields.put(PHONE, contact.phone());
        return serialize(fields);
    }

    @Override
    public void delete(UUID id) {
        contacts.deleteContact(new ContactId(id));
    }

    @Override
    public void recreate(UUID id, String snapshot) {
        Map<String, String> data = deserialize(snapshot);
        contacts.recreateContact(new ContactId(id), ownerOf(data),
                data.get(FIRST_NAME), data.get(LAST_NAME), data.get(EMAIL), data.get(PHONE));
    }

    private static PartnerRef ownerOf(Map<String, String> data) {
        return PartnerRef.of(PartnerType.valueOf(data.get(OWNER_TYPE)), UUID.fromString(data.get(OWNER_ID)));
    }

    private String serialize(Map<String, String> fields) {
        try {
            return json.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialize contact snapshot", e);
        }
    }

    private Map<String, String> deserialize(String snapshot) {
        try {
            return json.readValue(snapshot, MAP);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not deserialize contact snapshot", e);
        }
    }
}
