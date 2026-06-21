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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
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
 */
class InvoiceServiceTest {

    private final InvoiceRepository repo = mock(InvoiceRepository.class);
    private final NumberRanges numberRanges = mock(NumberRanges.class);
    private final AuditTrail audit = mock(AuditTrail.class);
    private InvoiceService service;

    @BeforeEach
    void setUp() {
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new InvoiceService(repo, numberRanges, audit);
    }

    private Invoice postedInvoice() {
        Invoice invoice = Invoice.createDraft(UUID.randomUUID(), Currency.getInstance("EUR"),
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
        Invoice draft = Invoice.createDraft(UUID.randomUUID(), Currency.getInstance("EUR"),
                List.of(new InvoiceLine("Widget", new BigDecimal("1"), Money.of("5.00", "EUR"))));
        when(repo.findById(draft.id())).thenReturn(Optional.of(draft));
        when(numberRanges.next(InvoiceService.INVOICE_NUMBER_RANGE)).thenReturn("RE-000007");

        Invoice posted = service.post(draft.id());

        assertThat(posted.status()).isEqualTo(InvoiceStatus.POSTED);
        assertThat(posted.documentNumber()).isEqualTo("RE-000007");
        verify(audit).record(any(), any(), any(), any());
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
    void cancellingANonPostedInvoiceIsRejected() {
        Invoice draft = Invoice.createDraft(UUID.randomUUID(), Currency.getInstance("EUR"),
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
