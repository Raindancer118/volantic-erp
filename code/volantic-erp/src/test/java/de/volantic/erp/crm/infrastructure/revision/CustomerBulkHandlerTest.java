package de.volantic.erp.crm.infrastructure.revision;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.crm.application.CustomerNotFoundException;
import de.volantic.erp.crm.application.CustomerService;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test that the {@link CustomerBulkHandler} maps the generic field model onto {@link CustomerService}
 * correctly: capture reads name+email, a partial change preserves the untouched field, and a missing
 * customer captures as {@code null}. The service is mocked — this pins the field wiring, not persistence.
 */
class CustomerBulkHandlerTest {

    private final CustomerService customers = mock(CustomerService.class);
    private final CustomerBulkHandler handler = new CustomerBulkHandler(customers, new ObjectMapper());

    @Test
    void resourceTypeAndEditableFields() {
        assertThat(handler.resourceType()).isEqualTo("crm.customer");
        assertThat(handler.editableFields()).containsExactlyInAnyOrder("name", "email");
    }

    @Test
    void captureSerializesNameAndEmail() {
        UUID id = UUID.randomUUID();
        when(customers.getCustomer(new CustomerId(id)))
                .thenReturn(Customer.reconstitute(new CustomerId(id), "C-1", "Acme", "info@acme.de"));

        assertThat(handler.capture(id)).contains("\"name\":\"Acme\"").contains("\"email\":\"info@acme.de\"");
    }

    @Test
    void captureReturnsNullWhenCustomerMissing() {
        UUID id = UUID.randomUUID();
        when(customers.getCustomer(new CustomerId(id))).thenThrow(new CustomerNotFoundException(new CustomerId(id)));

        assertThat(handler.capture(id)).isNull();
    }

    @Test
    void applyChangePreservesUntouchedEmail() {
        UUID id = UUID.randomUUID();
        when(customers.getCustomer(new CustomerId(id)))
                .thenReturn(Customer.reconstitute(new CustomerId(id), "C-1", "Acme", "info@acme.de"));

        handler.applyChange(id, Map.of("name", "Acme Corp"));

        verify(customers).updateCustomer(eq(new CustomerId(id)), eq("Acme Corp"), eq("info@acme.de"));
    }
}
