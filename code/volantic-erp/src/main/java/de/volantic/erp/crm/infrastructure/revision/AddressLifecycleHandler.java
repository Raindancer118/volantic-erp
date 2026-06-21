package de.volantic.erp.crm.infrastructure.revision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.core.revision.LifecycleResourceHandler;
import de.volantic.erp.crm.application.AddressService;
import de.volantic.erp.crm.application.CrmNotFoundException;
import de.volantic.erp.crm.domain.model.Address;
import de.volantic.erp.crm.domain.model.AddressId;
import de.volantic.erp.crm.domain.model.AddressType;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@code core.revision} lifecycle handler for {@link Address} (resource type {@code crm.address}): bulk
 * create and delete with Rollback-Engine reversal (ADR-0006 §2). The full snapshot includes the immutable
 * owner so a deleted address can be re-created with its original id. All operations go through
 * {@link AddressService}.
 */
@Component
class AddressLifecycleHandler implements LifecycleResourceHandler {

    private static final TypeReference<LinkedHashMap<String, String>> MAP = new TypeReference<>() {
    };
    private static final String OWNER_TYPE = "ownerType";
    private static final String OWNER_ID = "ownerId";
    private static final String TYPE = "type";
    private static final String STREET = "street";
    private static final String POSTAL_CODE = "postalCode";
    private static final String CITY = "city";
    private static final String COUNTRY_CODE = "countryCode";

    private final AddressService addresses;
    private final ObjectMapper json;

    AddressLifecycleHandler(AddressService addresses, ObjectMapper json) {
        this.addresses = addresses;
        this.json = json;
    }

    @Override
    public String resourceType() {
        return "crm.address";
    }

    @Override
    public UUID create(Map<String, String> data) {
        Address created = addresses.createAddress(ownerOf(data), AddressType.valueOf(data.get(TYPE)),
                data.get(STREET), data.get(POSTAL_CODE), data.get(CITY), data.get(COUNTRY_CODE));
        return created.id().value();
    }

    @Override
    public String snapshot(UUID id) {
        Address address;
        try {
            address = addresses.getAddress(new AddressId(id));
        } catch (CrmNotFoundException notFound) {
            return null;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(OWNER_TYPE, address.owner().type().name());
        fields.put(OWNER_ID, address.owner().id().toString());
        fields.put(TYPE, address.type().name());
        fields.put(STREET, address.street());
        fields.put(POSTAL_CODE, address.postalCode());
        fields.put(CITY, address.city());
        fields.put(COUNTRY_CODE, address.countryCode());
        return serialize(fields);
    }

    @Override
    public void delete(UUID id) {
        addresses.deleteAddress(new AddressId(id));
    }

    @Override
    public void recreate(UUID id, String snapshot) {
        Map<String, String> data = deserialize(snapshot);
        addresses.recreateAddress(new AddressId(id), ownerOf(data), AddressType.valueOf(data.get(TYPE)),
                data.get(STREET), data.get(POSTAL_CODE), data.get(CITY), data.get(COUNTRY_CODE));
    }

    private static PartnerRef ownerOf(Map<String, String> data) {
        return PartnerRef.of(PartnerType.valueOf(data.get(OWNER_TYPE)), UUID.fromString(data.get(OWNER_ID)));
    }

    private String serialize(Map<String, String> fields) {
        try {
            return json.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialize address snapshot", e);
        }
    }

    private Map<String, String> deserialize(String snapshot) {
        try {
            return json.readValue(snapshot, MAP);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not deserialize address snapshot", e);
        }
    }
}
