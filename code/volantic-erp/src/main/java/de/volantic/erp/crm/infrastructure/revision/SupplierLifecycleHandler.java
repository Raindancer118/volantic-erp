package de.volantic.erp.crm.infrastructure.revision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.core.revision.LifecycleResourceHandler;
import de.volantic.erp.crm.application.CrmNotFoundException;
import de.volantic.erp.crm.application.SupplierService;
import de.volantic.erp.crm.domain.model.Supplier;
import de.volantic.erp.crm.domain.model.SupplierId;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@code core.revision} lifecycle handler for {@link Supplier} (resource type {@code crm.supplier}): bulk
 * create and delete with Rollback-Engine reversal (ADR-0006 §2). The snapshot keeps the business key
 * ({@code supplierNumber}) so a deleted supplier is re-created with its original id and number. All
 * operations go through {@link SupplierService}.
 */
@Component
class SupplierLifecycleHandler implements LifecycleResourceHandler {

    private static final TypeReference<LinkedHashMap<String, String>> MAP = new TypeReference<>() {
    };
    private static final String NUMBER = "supplierNumber";
    private static final String ORG_UNIT = "orgUnitId";
    private static final String NAME = "name";
    private static final String EMAIL = "email";

    private final SupplierService suppliers;
    private final ObjectMapper json;

    SupplierLifecycleHandler(SupplierService suppliers, ObjectMapper json) {
        this.suppliers = suppliers;
        this.json = json;
    }

    @Override
    public String resourceType() {
        return "crm.supplier";
    }

    @Override
    public UUID create(Map<String, String> data) {
        return suppliers.createSupplier(UUID.fromString(data.get(ORG_UNIT)), data.get(NUMBER),
                data.get(NAME), data.get(EMAIL)).id().value();
    }

    @Override
    public String snapshot(UUID id) {
        Supplier supplier;
        try {
            supplier = suppliers.getSupplier(new SupplierId(id));
        } catch (CrmNotFoundException notFound) {
            return null;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(NUMBER, supplier.supplierNumber());
        fields.put(ORG_UNIT, supplier.orgUnitId().toString());
        fields.put(NAME, supplier.name());
        fields.put(EMAIL, supplier.email());
        return serialize(fields);
    }

    @Override
    public void delete(UUID id) {
        suppliers.deleteSupplier(new SupplierId(id));
    }

    @Override
    public void recreate(UUID id, String snapshot) {
        Map<String, String> data = deserialize(snapshot);
        suppliers.recreateSupplier(new SupplierId(id), UUID.fromString(data.get(ORG_UNIT)), data.get(NUMBER),
                data.get(NAME), data.get(EMAIL));
    }

    private String serialize(Map<String, String> fields) {
        try {
            return json.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialize supplier snapshot", e);
        }
    }

    private Map<String, String> deserialize(String snapshot) {
        try {
            return json.readValue(snapshot, MAP);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not deserialize supplier snapshot", e);
        }
    }
}
