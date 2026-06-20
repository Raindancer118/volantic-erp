package de.volantic.erp.crm.infrastructure.revision;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.crm.application.AddressNotFoundException;
import de.volantic.erp.crm.application.AddressService;
import de.volantic.erp.crm.domain.model.Address;
import de.volantic.erp.crm.domain.model.AddressId;
import de.volantic.erp.crm.domain.model.AddressType;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
import de.volantic.erp.core.revision.ChangeOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AddressBulkHandler}: the {@link AddressType} enum round-trips by name and the
 * five editable postal fields map onto {@link AddressService}. The immutable owner is never written.
 */
class AddressBulkHandlerTest {

    private final AddressService addresses = mock(AddressService.class);
    private final AddressBulkHandler handler = new AddressBulkHandler(addresses, new ObjectMapper());

    private final UUID id = UUID.randomUUID();
    private final PartnerRef owner = PartnerRef.of(PartnerType.CUSTOMER, UUID.randomUUID());

    @BeforeEach
    void stubAddress() {
        when(addresses.getAddress(new AddressId(id))).thenReturn(Address.reconstitute(
                new AddressId(id), owner, AddressType.BILLING, "Hauptstr. 1", "20095", "Hamburg", "DE"));
    }

    @Test
    void editableFieldsExcludeOwner() {
        assertThat(handler.editableFields())
                .containsExactlyInAnyOrder("type", "street", "postalCode", "city", "countryCode")
                .doesNotContain("owner");
    }

    @Test
    void applyChangeUpdatesOnlyCityKeepingTheRestAndType() {
        handler.applyChange(id, Map.of("city", "Bremen"));

        verify(addresses).updateAddress(eq(new AddressId(id)), eq(AddressType.BILLING),
                eq("Hauptstr. 1"), eq("20095"), eq("Bremen"), eq("DE"));
    }

    @Test
    void compensateRestoresAllFieldsIncludingType() {
        String before = handler.capture(id);

        handler.compensate(ChangeOperation.UPDATE, id, before);

        verify(addresses).updateAddress(eq(new AddressId(id)), eq(AddressType.BILLING),
                eq("Hauptstr. 1"), eq("20095"), eq("Hamburg"), eq("DE"));
    }

    @Test
    void captureReturnsNullWhenAddressMissing() {
        UUID missing = UUID.randomUUID();
        when(addresses.getAddress(new AddressId(missing))).thenThrow(new AddressNotFoundException(new AddressId(missing)));

        assertThat(handler.capture(missing)).isNull();
    }
}
