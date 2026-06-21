package de.volantic.erp.crm.infrastructure.revision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.core.revision.LifecycleResourceHandler;
import de.volantic.erp.crm.application.CrmNotFoundException;
import de.volantic.erp.crm.application.CustomerService;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@code core.revision} lifecycle handler for {@link Customer} (resource type {@code crm.customer}): bulk
 * create and delete with Rollback-Engine reversal (ADR-0006 §2). The snapshot keeps the business key
 * ({@code customerNumber}) so a deleted customer is re-created with its original id and number. All
 * operations go through {@link CustomerService}.
 */
@Component
class CustomerLifecycleHandler implements LifecycleResourceHandler {

    private static final TypeReference<LinkedHashMap<String, String>> MAP = new TypeReference<>() {
    };
    private static final String NUMBER = "customerNumber";
    private static final String NAME = "name";
    private static final String EMAIL = "email";

    private final CustomerService customers;
    private final ObjectMapper json;

    CustomerLifecycleHandler(CustomerService customers, ObjectMapper json) {
        this.customers = customers;
        this.json = json;
    }

    @Override
    public String resourceType() {
        return "crm.customer";
    }

    @Override
    public UUID create(Map<String, String> data) {
        return customers.createCustomer(data.get(NUMBER), data.get(NAME), data.get(EMAIL)).id().value();
    }

    @Override
    public String snapshot(UUID id) {
        Customer customer;
        try {
            customer = customers.getCustomer(new CustomerId(id));
        } catch (CrmNotFoundException notFound) {
            return null;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(NUMBER, customer.customerNumber());
        fields.put(NAME, customer.name());
        fields.put(EMAIL, customer.email());
        return serialize(fields);
    }

    @Override
    public void delete(UUID id) {
        customers.deleteCustomer(new CustomerId(id));
    }

    @Override
    public void recreate(UUID id, String snapshot) {
        Map<String, String> data = deserialize(snapshot);
        customers.recreateCustomer(new CustomerId(id), data.get(NUMBER), data.get(NAME), data.get(EMAIL));
    }

    private String serialize(Map<String, String> fields) {
        try {
            return json.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialize customer snapshot", e);
        }
    }

    private Map<String, String> deserialize(String snapshot) {
        try {
            return json.readValue(snapshot, MAP);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not deserialize customer snapshot", e);
        }
    }
}
