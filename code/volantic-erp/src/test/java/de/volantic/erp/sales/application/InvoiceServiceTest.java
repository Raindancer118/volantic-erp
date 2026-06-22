package de.volantic.erp.sales.application;

import de.volantic.erp.audit.AuditTrail;
import de.volantic.erp.core.measure.Money;
import de.volantic.erp.core.numberrange.NumberRanges;
import de.volantic.erp.sales.SalesExceptions;
import de.volantic.erp.sales.application.port.out.InvoiceRepository;
import de.volantic.erp.sales.domain.model.Invoice;
import de.volantic.erp.sales.domain.model.InvoiceId;
import de.volantic.erp.sales.domain.model.InvoiceLine;
import de.volantic.erp.sales.domain.model.InvoiceStatus;
import de.volantic.erp.security.ScopeEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Use-case orchestration of {@link InvoiceService} over mocked ports: posting draws a gap-free number,
 * cancellation posts a negated storno into the same range and marks the original cancelled.
 * Also verifies that org-unit scope checks (ADR-0007) are delegated to {@link ScopeEnforcer}.
 */
class InvoiceServiceTest {

    private static final UUID ORG_UNIT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final InvoiceRepository repo = mock(InvoiceRepository.class);
    private final NumberRanges numberRanges = mock(NumberRanges.class);
    private final AuditTrail audit = mock(AuditTrail.class);
    private final ScopeEnforcer scopeEnforcer = mock(ScopeEnforcer.class);
    private InvoiceService service;

    @BeforeEach
    void setUp() {
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        // Default: unrestricted (no filtering)
        when(scopeEnforcer.permittedOrgUnits(any())).thenReturn(Optional.empty());
        service = new InvoiceService(repo, numberRanges, audit, scopeEnforcer);
    }

    private Invoice postedInvoice() {
        Invoice invoice = Invoice.createDraft(ORG_UNIT, UUID.randomUUID(), Currency.getInstance("EUR"),
                List.of(new InvoiceLine("Widget", new BigDecimal("2"), Money.of("10.00", "EUR"))));
        invoice.post("RE-000001");
        return invoice;
    }

    @Test
    void definesTheInvoiceRangeOnConstruction() {
        verify(numberRanges).defineRange(InvoiceService.INVOICE_NUMBER_RANGE, "RE-", 6, 1);
    }

    @Test
    void postDrawsTheNextNumberAndAudits() {
        Invoice draft = Invoice.createDraft(ORG_UNIT, UUID.randomUUID(), Currency.getInstance("EUR"),
                List.of(new InvoiceLine("Widget", new BigDecimal("1"), Money.of("5.00", "EUR"))));
        when(repo.findById(draft.id())).thenReturn(Optional.of(draft));
        when(numberRanges.next(InvoiceService.INVOICE_NUMBER_RANGE)).thenReturn("RE-000007");

        Invoice posted = service.post(draft.id());

        assertThat(posted.status()).isEqualTo(InvoiceStatus.POSTED);
        assertThat(posted.documentNumber()).isEqualTo("RE-000007");
        verify(audit).record(any(), any(), any(), any());
    }

    @Test
    void postCallsRequireWithTheInvoicesOrgUnit() {
        Invoice draft = Invoice.createDraft(ORG_UNIT, UUID.randomUUID(), Currency.getInstance("EUR"),
                List.of(new InvoiceLine("Widget", BigDecimal.ONE, Money.of("5.00", "EUR"))));
        when(repo.findById(draft.id())).thenReturn(Optional.of(draft));
        when(numberRanges.next(InvoiceService.INVOICE_NUMBER_RANGE)).thenReturn("RE-000001");

        service.post(draft.id());

        verify(scopeEnforcer).require("sales.invoice:post", ORG_UNIT);
    }

    @Test
    void cancelPostsAStornoAndMarksTheOriginalCancelled() {
        Invoice original = postedInvoice();
        when(repo.findById(original.id())).thenReturn(Optional.of(original));
        when(numberRanges.next(InvoiceService.INVOICE_NUMBER_RANGE)).thenReturn("RE-000002");

        Invoice storno = service.cancel(original.id());

        assertThat(storno.isStorno()).isTrue();
        assertThat(storno.status()).isEqualTo(InvoiceStatus.POSTED);
        assertThat(storno.documentNumber()).isEqualTo("RE-000002");
        assertThat(storno.total()).isEqualTo(Money.of("-20.00", "EUR"));
        assertThat(original.status()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(original.cancelledBy()).isEqualTo(storno.id());
    }

    @Test
    void cancelCallsRequireWithTheOriginalInvoicesOrgUnit() {
        Invoice original = postedInvoice();
        when(repo.findById(original.id())).thenReturn(Optional.of(original));
        when(numberRanges.next(InvoiceService.INVOICE_NUMBER_RANGE)).thenReturn("RE-000002");

        service.cancel(original.id());

        verify(scopeEnforcer).require("sales.invoice:post", ORG_UNIT);
    }

    @Test
    void getInvoiceCallsRequireWithTheInvoicesOrgUnit() {
        Invoice invoice = postedInvoice();
        when(repo.findById(invoice.id())).thenReturn(Optional.of(invoice));

        service.getInvoice(invoice.id());

        verify(scopeEnforcer).require("sales.invoice:read", ORG_UNIT);
    }

    @Test
    void listInvoicesCallsRequireAnywhereAndReturnsAllWhenUnrestricted() {
        when(repo.findAll(any())).thenReturn(new PageImpl<>(List.of(postedInvoice())));
        var pageable = PageRequest.of(0, 50);

        service.listInvoices(pageable);

        verify(scopeEnforcer).requireAnywhere("sales.invoice:read");
        verify(repo).findAll(pageable);
    }

    @Test
    void listInvoicesFiltersToPermittedOrgUnitsWhenPresent() {
        Set<UUID> permitted = Set.of(ORG_UNIT);
        when(scopeEnforcer.permittedOrgUnits("sales.invoice:read")).thenReturn(Optional.of(permitted));
        when(repo.findAllInOrgUnits(any(), any())).thenReturn(new PageImpl<>(List.of()));
        var pageable = PageRequest.of(0, 50);

        service.listInvoices(pageable);

        verify(repo).findAllInOrgUnits(permitted, pageable);
    }

    @Test
    void cancellingANonPostedInvoiceIsRejected() {
        Invoice draft = Invoice.createDraft(ORG_UNIT, UUID.randomUUID(), Currency.getInstance("EUR"),
                List.of(new InvoiceLine("Widget", BigDecimal.ONE, Money.of("5.00", "EUR"))));
        when(repo.findById(draft.id())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.cancel(draft.id()))
                .isInstanceOf(SalesExceptions.InvalidInvoiceState.class);
    }

    @Test
    void getMissingInvoiceThrowsNotFound() {
        InvoiceId id = InvoiceId.newId();
        when(repo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getInvoice(id)).isInstanceOf(SalesExceptions.InvoiceNotFound.class);
    }
}
