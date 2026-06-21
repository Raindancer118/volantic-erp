package de.volantic.erp.sales.domain.model;

import de.volantic.erp.core.measure.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure domain tests of the {@link Invoice} aggregate and its GoBD lifecycle invariants. */
class InvoiceTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    private static InvoiceLine line(String desc, String qty, String price) {
        return new InvoiceLine(desc, new BigDecimal(qty), Money.of(price, "EUR"));
    }

    private static Invoice draft() {
        return Invoice.createDraft(UUID.randomUUID(), EUR, List.of(line("Widget", "2", "10.00")));
    }

    @Test
    void draftHasNoNumberAndComputesTotal() {
        Invoice invoice = draft();

        assertThat(invoice.status()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(invoice.documentNumber()).isNull();
        assertThat(invoice.total()).isEqualTo(Money.of("20.00", "EUR"));
    }

    @Test
    void rejectsEmptyLinesAndForeignCurrencyLines() {
        assertThatThrownBy(() -> Invoice.createDraft(UUID.randomUUID(), EUR, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Invoice.createDraft(UUID.randomUUID(), EUR,
                List.of(new InvoiceLine("X", BigDecimal.ONE, Money.of("1.00", "USD")))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void postAssignsNumberAndFreezes() {
        Invoice invoice = draft();
        invoice.post("RE-000001");

        assertThat(invoice.status()).isEqualTo(InvoiceStatus.POSTED);
        assertThat(invoice.documentNumber()).isEqualTo("RE-000001");
        assertThat(invoice.issueDate()).isNotNull();
        assertThatThrownBy(() -> invoice.post("RE-000002")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void stornoNegatesLinesAndReferencesTheOriginal() {
        Invoice original = draft();
        original.post("RE-000001");

        Invoice storno = Invoice.storno(original);

        assertThat(storno.isStorno()).isTrue();
        assertThat(storno.stornoOf()).isEqualTo(original.id());
        assertThat(storno.status()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(storno.total()).isEqualTo(Money.of("-20.00", "EUR")); // negated
    }

    @Test
    void onlyAPostedInvoiceCanBeStornoedOrCancelled() {
        Invoice draft = draft();
        assertThatThrownBy(() -> Invoice.storno(draft)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> draft.cancel(InvoiceId.newId())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelMarksTheInvoiceCancelledByTheStorno() {
        Invoice original = draft();
        original.post("RE-000001");
        InvoiceId stornoId = InvoiceId.newId();

        original.cancel(stornoId);

        assertThat(original.status()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(original.cancelledBy()).isEqualTo(stornoId);
    }
}
