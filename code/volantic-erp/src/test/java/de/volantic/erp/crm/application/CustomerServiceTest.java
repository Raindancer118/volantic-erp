package de.volantic.erp.crm.application;

import de.volantic.erp.crm.application.port.out.CustomerRepository;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import de.volantic.erp.core.UuidV7;
import de.volantic.erp.security.ScopeEnforcer;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Use-case logic of {@link CustomerService} over a mocked {@link CustomerRepository} (no Spring/DB). */
class CustomerServiceTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final ScopeEnforcer scopeEnforcer = mock(ScopeEnforcer.class);
    private final CustomerService service = new CustomerService(repository, scopeEnforcer);

    @Test
    void createPersistsWhenNumberIsFree() {
        when(repository.existsByCustomerNumber("C-1")).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Customer created = service.createCustomer(ORG, "C-1", "ACME", "a@acme.de");

        assertThat(created.customerNumber()).isEqualTo("C-1");
        assertThat(created.orgUnitId()).isEqualTo(ORG);
        verify(repository).save(any(Customer.class));
    }

    @Test
    void createRejectsDuplicateNumber() {
        when(repository.existsByCustomerNumber("C-1")).thenReturn(true);

        assertThatThrownBy(() -> service.createCustomer(ORG, "C-1", "ACME", "a@acme.de"))
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
    void getEnforcesTheCustomersOrgUnitScope() {
        Customer existing = Customer.create(ORG, "C-1", "ACME", "a@acme.de");
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));
        doThrow(new AccessDeniedException("out of scope")).when(scopeEnforcer).require("crm.customer:read", ORG);

        assertThatThrownBy(() -> service.getCustomer(existing.id()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateMutatesAndSaves() {
        Customer existing = Customer.create(ORG, "C-1", "ACME", "a@acme.de");
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Customer updated = service.updateCustomer(existing.id(), "ACME AG", "neu@acme.de");

        assertThat(updated.name()).isEqualTo("ACME AG");
        assertThat(updated.email()).isEqualTo("neu@acme.de");
        verify(scopeEnforcer).require("crm.customer:update", ORG);
        verify(repository).save(existing);
    }

    @Test
    void updateDeniedOutOfScopeIsRejectedBeforeSaving() {
        Customer existing = Customer.create(ORG, "C-1", "ACME", "a@acme.de");
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));
        doThrow(new AccessDeniedException("out of scope")).when(scopeEnforcer).require("crm.customer:update", ORG);

        assertThatThrownBy(() -> service.updateCustomer(existing.id(), "ACME AG", "neu@acme.de"))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
    }
}
