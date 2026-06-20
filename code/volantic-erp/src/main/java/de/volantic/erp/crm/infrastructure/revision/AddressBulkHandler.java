package de.volantic.erp.crm.infrastructure.revision;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.core.revision.AbstractFieldMapHandler;
import de.volantic.erp.crm.application.AddressService;
import de.volantic.erp.crm.application.CrmNotFoundException;
import de.volantic.erp.crm.domain.model.Address;
import de.volantic.erp.crm.domain.model.AddressId;
import de.volantic.erp.crm.domain.model.AddressType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * {@code core.revision} handler for {@link Address} (resource type {@code crm.address}). The address'
 * owner is immutable; its type and postal fields are bulk-editable. {@code type} is exchanged as the
 * {@link AddressType} enum name. Reads/writes run through {@link AddressService}.
 */
@Component
class AddressBulkHandler extends AbstractFieldMapHandler {

    static final String TYPE = "crm.address";
    static final String FIELD_TYPE = "type";
    static final String FIELD_STREET = "street";
    static final String FIELD_POSTAL_CODE = "postalCode";
    static final String FIELD_CITY = "city";
    static final String FIELD_COUNTRY_CODE = "countryCode";

    private final AddressService addresses;

    AddressBulkHandler(AddressService addresses, ObjectMapper json) {
        super(json);
        this.addresses = addresses;
    }

    @Override
    public String resourceType() {
        return TYPE;
    }

    @Override
    public Set<String> editableFields() {
        return Set.of(FIELD_TYPE, FIELD_STREET, FIELD_POSTAL_CODE, FIELD_CITY, FIELD_COUNTRY_CODE);
    }

    @Override
    protected Map<String, String> readFields(UUID id) {
        Address address;
        try {
            address = addresses.getAddress(new AddressId(id));
        } catch (CrmNotFoundException notFound) {
            return null;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(FIELD_TYPE, address.type().name());
        fields.put(FIELD_STREET, address.street());
        fields.put(FIELD_POSTAL_CODE, address.postalCode());
        fields.put(FIELD_CITY, address.city());
        fields.put(FIELD_COUNTRY_CODE, address.countryCode());
        return fields;
    }

    @Override
    protected void writeFields(UUID id, Map<String, String> fields) {
        addresses.updateAddress(new AddressId(id),
                parseType(fields.get(FIELD_TYPE)),
                fields.get(FIELD_STREET), fields.get(FIELD_POSTAL_CODE),
                fields.get(FIELD_CITY), fields.get(FIELD_COUNTRY_CODE));
    }

    @Override
    public Set<String> filterableFields() {
        return Set.of(FIELD_TYPE, FIELD_CITY, FIELD_POSTAL_CODE, FIELD_COUNTRY_CODE);
    }

    @Override
    public List<UUID> selectIds(Map<String, String> filter) {
        AddressType type = filterType(filter.get(FIELD_TYPE));
        String countryCode = filter.get(FIELD_COUNTRY_CODE);
        return addresses.findAddressIds(type, filter.get(FIELD_CITY), filter.get(FIELD_POSTAL_CODE),
                        countryCode == null ? null : countryCode.strip().toUpperCase())
                .stream().map(AddressId::value).toList();
    }

    /** A blank/absent value applied to a write defaults to {@link AddressType#DEFAULT}. */
    private static AddressType parseType(String value) {
        AddressType type = filterType(value);
        return type == null ? AddressType.DEFAULT : type;
    }

    /** As a filter, a blank/absent type means "do not filter by type" ({@code null}), not DEFAULT. */
    private static AddressType filterType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AddressType.valueOf(value.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown address type: " + value);
        }
    }
}
