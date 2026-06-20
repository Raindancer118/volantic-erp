package de.volantic.erp.crm.infrastructure.revision;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.core.revision.AbstractFieldMapHandler;
import de.volantic.erp.crm.application.CrmNotFoundException;
import de.volantic.erp.crm.application.SupplierService;
import de.volantic.erp.crm.domain.model.Supplier;
import de.volantic.erp.crm.domain.model.SupplierId;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * {@code core.revision} handler for {@link Supplier} (resource type {@code crm.supplier}). Mass edits and
 * Rollback Engine reversal run through {@link SupplierService}; {@code supplierNumber} is the business key
 * and not bulk-editable.
 */
@Component
class SupplierBulkHandler extends AbstractFieldMapHandler {

    static final String TYPE = "crm.supplier";
    static final String FIELD_NAME = "name";
    static final String FIELD_EMAIL = "email";

    private final SupplierService suppliers;

    SupplierBulkHandler(SupplierService suppliers, ObjectMapper json) {
        super(json);
        this.suppliers = suppliers;
    }

    @Override
    public String resourceType() {
        return TYPE;
    }

    @Override
    public Set<String> editableFields() {
        return Set.of(FIELD_NAME, FIELD_EMAIL);
    }

    @Override
    protected Map<String, String> readFields(UUID id) {
        Supplier supplier;
        try {
            supplier = suppliers.getSupplier(new SupplierId(id));
        } catch (CrmNotFoundException notFound) {
            return null;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(FIELD_NAME, supplier.name());
        fields.put(FIELD_EMAIL, supplier.email());
        return fields;
    }

    @Override
    protected void writeFields(UUID id, Map<String, String> fields) {
        suppliers.updateSupplier(new SupplierId(id), fields.get(FIELD_NAME), fields.get(FIELD_EMAIL));
    }
}
