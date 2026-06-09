package de.volantic.erp.crm.application;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.crm.application.port.out.SupplierRepository;
import de.volantic.erp.crm.domain.model.Supplier;
import de.volantic.erp.crm.domain.model.SupplierId;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Use-case logic of {@link SupplierService} over a mocked {@link SupplierRepository}. */
class SupplierServiceTest {

    private final SupplierRepository repository = mock(SupplierRepository.class);
    private final SupplierService service = new SupplierService(repository);

    @Test
    void createPersistsWhenNumberIsFree() {
        when(repository.existsBySupplierNumber("S-1")).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier created = service.createSupplier("S-1", "Globex", "a@globex.de");

        assertThat(created.supplierNumber()).isEqualTo("S-1");
        verify(repository).save(any(Supplier.class));
    }

    @Test
    void createRejectsDuplicateNumber() {
        when(repository.existsBySupplierNumber("S-1")).thenReturn(true);

        assertThatThrownBy(() -> service.createSupplier("S-1", "Globex", "a@globex.de"))
                .isInstanceOf(SupplierNumberAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void getThrowsWhenMissing() {
        SupplierId id = new SupplierId(UuidV7.randomUuid());
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSupplier(id)).isInstanceOf(SupplierNotFoundException.class);
    }

    @Test
    void updateMutatesAndSaves() {
        Supplier existing = Supplier.create("S-1", "Globex", "a@globex.de");
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier updated = service.updateSupplier(existing.id(), "Globex Corp", "neu@globex.de");

        assertThat(updated.name()).isEqualTo("Globex Corp");
        verify(repository).save(existing);
    }
}
