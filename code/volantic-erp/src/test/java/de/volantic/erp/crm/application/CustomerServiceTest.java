package de.volantic.erp.crm.application;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.crm.application.port.out.CustomerRepository;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import de.volantic.erp.crm.domain.model.OrgUnitId;
import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.AuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Use-case logic of {@link CustomerService} over mocked ports (no Spring/DB), incl. org-unit scope. */
class CustomerServiceTest {

    private static final OrgUnitId ORG = new OrgUnitId(UUID.randomUUID());

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final CustomerService service = new CustomerService(repository, authorization);

    @BeforeEach
    void authenticateAsAlice() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null, AuthorityUtils.NO_AUTHORITIES));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void permit() {
        when(authorization.isPermitted(anyString(), anyString(), any(AccessScope.class))).thenReturn(true);
    }

    @Test
    void createChecksTheOrgUnitScopeThenPersists() {
        permit();
        when(repository.existsByCustomerNumber("C-1")).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Customer created = service.createCustomer(ORG, "C-1", "ACME", "a@acme.de");

        assertThat(created.customerNumber()).isEqualTo("C-1");
        assertThat(created.orgUnitId()).isEqualTo(ORG);
        // the scope checked is the ORG_UNIT of the new customer
        ArgumentCaptor<AccessScope> scope = ArgumentCaptor.forClass(AccessScope.class);
        verify(authorization).isPermitted(eq("alice"), eq("crm.customer:create"), scope.capture());
        assertThat(scope.getValue().type()).isEqualTo("ORG_UNIT");
        assertThat(scope.getValue().id()).isEqualTo(ORG.value());
    }

    @Test
    void createDeniedWhenNotPermittedInScope() {
        when(authorization.isPermitted(anyString(), anyString(), any(AccessScope.class))).thenReturn(false);

        assertThatThrownBy(() -> service.createCustomer(ORG, "C-1", "ACME", "a@acme.de"))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).existsByCustomerNumber(any());
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateNumber() {
        permit();
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
    void getDeniedWhenNotPermittedInTheCustomersScope() {
        CustomerId id = new CustomerId(UuidV7.randomUuid());
        when(repository.findById(id)).thenReturn(Optional.of(Customer.reconstitute(id, ORG, "C-1", "ACME", null)));
        when(authorization.isPermitted(anyString(), eq("crm.customer:read"), any(AccessScope.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getCustomer(id)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateMutatesAndSavesWhenPermittedInScope() {
        permit();
        CustomerId id = new CustomerId(UuidV7.randomUuid());
        Customer existing = Customer.reconstitute(id, ORG, "C-1", "ACME", "a@acme.de");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Customer updated = service.updateCustomer(id, "ACME AG", "neu@acme.de");

        assertThat(updated.name()).isEqualTo("ACME AG");
        assertThat(updated.email()).isEqualTo("neu@acme.de");
        verify(repository).save(existing);
    }

    @Test
    void updateDeniedWhenNotPermittedInScopeAndNotSaved() {
        CustomerId id = new CustomerId(UuidV7.randomUuid());
        when(repository.findById(id)).thenReturn(Optional.of(Customer.reconstitute(id, ORG, "C-1", "ACME", null)));
        when(authorization.isPermitted(anyString(), eq("crm.customer:update"), any(AccessScope.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.updateCustomer(id, "ACME AG", "neu@acme.de"))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
    }
}
