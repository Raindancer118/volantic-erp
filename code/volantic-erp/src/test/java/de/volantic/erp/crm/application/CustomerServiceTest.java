package de.volantic.erp.crm.application;

import de.volantic.erp.crm.application.port.out.CustomerRepository;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import de.volantic.erp.core.UuidV7;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Use-case logic of {@link CustomerService} over a mocked {@link CustomerRepository} (no Spring/DB). */
class CustomerServiceTest {

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final CustomerService service = new CustomerService(repository);

    @Test
    void createPersistsWhenNumberIsFree() {
        when(repository.existsByCustomerNumber("C-1")).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Customer created = service.createCustomer("C-1", "ACME", "a@acme.de");

        assertThat(created.customerNumber()).isEqualTo("C-1");
        verify(repository).save(any(Customer.class));
    }

    @Test
    void createRejectsDuplicateNumber() {
        when(repository.existsByCustomerNumber("C-1")).thenReturn(true);

        assertThatThrownBy(() -> service.createCustomer("C-1", "ACME", "a@acme.de"))
                .isInstanceOf(CustomerNumberAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void getThrowsWhenMissing() {
        CustomerId id = new CustomerId(UuidV7.randomUuid());
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCustomer(id)).isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void updateMutatesAndSaves() {
        Customer existing = Customer.create("C-1", "ACME", "a@acme.de");
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Customer updated = service.updateCustomer(existing.id(), "ACME AG", "neu@acme.de");

        assertThat(updated.name()).isEqualTo("ACME AG");
        assertThat(updated.email()).isEqualTo("neu@acme.de");
        verify(repository).save(existing);
    }
}
