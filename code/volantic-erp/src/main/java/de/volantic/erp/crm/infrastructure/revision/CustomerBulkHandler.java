package de.volantic.erp.crm.infrastructure.revision;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.core.revision.AbstractFieldMapHandler;
import de.volantic.erp.crm.application.CrmNotFoundException;
import de.volantic.erp.crm.application.CustomerService;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * {@code core.revision} handler for {@link Customer} (resource type {@code crm.customer}). Lets customers
 * take part in mass edits and the Rollback Engine (ADR-0006). All reads/writes go through
 * {@link CustomerService}, so authorization, validation and the audit trail run — never a direct DB write.
 * {@code customerNumber} is the business key and deliberately not bulk-editable.
 */
@Component
class CustomerBulkHandler extends AbstractFieldMapHandler {

    static final String TYPE = "crm.customer";
    static final String FIELD_NAME = "name";
    static final String FIELD_EMAIL = "email";

    private final CustomerService customers;

    CustomerBulkHandler(CustomerService customers, ObjectMapper json) {
        super(json);
        this.customers = customers;
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
        Customer customer;
        try {
            customer = customers.getCustomer(new CustomerId(id));
        } catch (CrmNotFoundException notFound) {
            return null;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(FIELD_NAME, customer.name());
        fields.put(FIELD_EMAIL, customer.email());
        return fields;
    }

    @Override
    protected void writeFields(UUID id, Map<String, String> fields) {
        customers.updateCustomer(new CustomerId(id), fields.get(FIELD_NAME), fields.get(FIELD_EMAIL));
    }

    @Override
    public Set<String> filterableFields() {
        return Set.of(FIELD_NAME, FIELD_EMAIL);
    }

    @Override
    public List<UUID> selectIds(Map<String, String> filter) {
        return customers.findCustomerIds(filter.get(FIELD_NAME), filter.get(FIELD_EMAIL)).stream()
                .map(CustomerId::value).toList();
    }
}
