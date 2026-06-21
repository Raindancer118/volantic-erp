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
 * Unit test that {@link CustomerLifecycleHandler} maps create/snapshot/delete/recreate onto
 * {@link CustomerService} correctly — in particular that the snapshot keeps the business key so recreate
 * restores the original number. Service mocked; this pins the field wiring, not persistence.
 */
class CustomerLifecycleHandlerTest {

    private final CustomerService customers = mock(CustomerService.class);
    private final CustomerLifecycleHandler handler = new CustomerLifecycleHandler(customers, new ObjectMapper());

    @Test
    void resourceType() {
        assertThat(handler.resourceType()).isEqualTo("crm.customer");
    }

    @Test
    void createDelegatesWithNumberNameEmail() {
        UUID id = UUID.randomUUID();
        when(customers.createCustomer("C-1", "Acme", "info@acme.de"))
                .thenReturn(Customer.reconstitute(new CustomerId(id), "C-1", "Acme", "info@acme.de"));

        UUID created = handler.create(Map.of("customerNumber", "C-1", "name", "Acme", "email", "info@acme.de"));

        assertThat(created).isEqualTo(id);
        verify(customers).createCustomer("C-1", "Acme", "info@acme.de");
    }

    @Test
    void snapshotThenRecreateRestoresTheOriginalNumberAndId() {
        UUID id = UUID.randomUUID();
        when(customers.getCustomer(new CustomerId(id)))
                .thenReturn(Customer.reconstitute(new CustomerId(id), "C-1", "Acme", "info@acme.de"));

        String snapshot = handler.snapshot(id);
        assertThat(snapshot).contains("\"customerNumber\":\"C-1\"");

        handler.recreate(id, snapshot);
        verify(customers).recreateCustomer(eq(new CustomerId(id)), eq("C-1"), eq("Acme"), eq("info@acme.de"));
    }

    @Test
    void snapshotReturnsNullWhenMissing() {
        UUID id = UUID.randomUUID();
        when(customers.getCustomer(new CustomerId(id))).thenThrow(new CustomerNotFoundException(new CustomerId(id)));

        assertThat(handler.snapshot(id)).isNull();
    }

    @Test
    void deleteDelegates() {
        UUID id = UUID.randomUUID();
        handler.delete(id);
        verify(customers).deleteCustomer(new CustomerId(id));
    }
}
