package de.volantic.erp.crm.infrastructure.revision;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.crm.application.SupplierNotFoundException;
import de.volantic.erp.crm.application.SupplierService;
import de.volantic.erp.crm.domain.model.Supplier;
import de.volantic.erp.crm.domain.model.SupplierId;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test for {@link SupplierLifecycleHandler} field wiring onto {@link SupplierService} (mocked). */
class SupplierLifecycleHandlerTest {

    private final SupplierService suppliers = mock(SupplierService.class);
    private final SupplierLifecycleHandler handler = new SupplierLifecycleHandler(suppliers, new ObjectMapper());

    @Test
    void resourceType() {
        assertThat(handler.resourceType()).isEqualTo("crm.supplier");
    }

    @Test
    void createDelegatesWithNumberNameEmail() {
        UUID id = UUID.randomUUID();
        when(suppliers.createSupplier("S-1", "Globex", "sales@globex.de"))
                .thenReturn(Supplier.reconstitute(new SupplierId(id), "S-1", "Globex", "sales@globex.de"));

        UUID created = handler.create(Map.of("supplierNumber", "S-1", "name", "Globex", "email", "sales@globex.de"));

        assertThat(created).isEqualTo(id);
    }

    @Test
    void snapshotThenRecreateRestoresOriginalNumberAndId() {
        UUID id = UUID.randomUUID();
        when(suppliers.getSupplier(new SupplierId(id)))
                .thenReturn(Supplier.reconstitute(new SupplierId(id), "S-1", "Globex", "sales@globex.de"));

        String snapshot = handler.snapshot(id);
        assertThat(snapshot).contains("\"supplierNumber\":\"S-1\"");

        handler.recreate(id, snapshot);
        verify(suppliers).recreateSupplier(eq(new SupplierId(id)), eq("S-1"), eq("Globex"), eq("sales@globex.de"));
    }

    @Test
    void snapshotReturnsNullWhenMissing() {
        UUID id = UUID.randomUUID();
        when(suppliers.getSupplier(new SupplierId(id))).thenThrow(new SupplierNotFoundException(new SupplierId(id)));

        assertThat(handler.snapshot(id)).isNull();
    }

    @Test
    void deleteDelegates() {
        UUID id = UUID.randomUUID();
        handler.delete(id);
        verify(suppliers).deleteSupplier(new SupplierId(id));
    }
}
