package de.volantic.erp.sales;

import de.volantic.erp.core.measure.Money;
import de.volantic.erp.sales.application.InvoiceService;
import de.volantic.erp.sales.domain.model.Invoice;
import de.volantic.erp.sales.domain.model.InvoiceId;
import de.volantic.erp.sales.domain.model.InvoiceLine;
import de.volantic.erp.sales.domain.model.InvoiceStatus;
import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.SecurityAdmin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end integration test of the sales invoice (Beleg) lifecycle against a real PostgreSQL
 * (Testcontainers), through the real {@link InvoiceService} with real authorization, gap-free numbering
 * ({@code core.numberrange}) and the audit trail. Proves the GoBD rules at runtime: posting draws
 * consecutive numbers, a posted invoice is immutable, and cancellation posts a negated storno into the
 * same range while leaving the original intact. Skipped without Docker; runs in CI.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class InvoiceFlowIT {

    private static final String ACTOR = "sales-clerk";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static boolean seeded;

    @Autowired
    private SecurityAdmin securityAdmin;

    @Autowired
    private InvoiceService invoices;

    @Autowired
    private de.volantic.erp.changeset.application.ChangeSetService changeSets;

    @BeforeEach
    void seedAndAuthenticate() {
        if (!seeded) {
            Set<String> permissions = Set.of("sales.invoice:read", "sales.invoice:write", "sales.invoice:post",
                    "changeset.bulk:execute", "changeset.rollback:revert");
            permissions.forEach(key -> securityAdmin.definePermission(key, key));
            securityAdmin.defineRole("sales-clerk-role", "Sales clerk", permissions);
            securityAdmin.provisionUser(ACTOR, "clerk", "clerk@volantic.de");
            securityAdmin.assignRole(ACTOR, "sales-clerk-role", AccessScope.GLOBAL);
            seeded = true;
        }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ACTOR, "n/a", AuthorityUtils.NO_AUTHORITIES));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private InvoiceId newDraft(String desc, String qty, String price) {
        Invoice draft = invoices.createDraft(UUID.randomUUID(), "EUR",
                List.of(new InvoiceLine(desc, new BigDecimal(qty), Money.of(price, "EUR"))));
        return draft.id();
    }

    @Test
    void postingDrawsConsecutiveGapFreeNumbers() {
        Invoice first = invoices.post(newDraft("A", "1", "10.00"));
        Invoice second = invoices.post(newDraft("B", "1", "20.00"));

        assertThat(first.documentNumber()).isNotNull().startsWith("RE-");
        assertThat(second.documentNumber()).isNotNull().startsWith("RE-");
        assertThat(first.documentNumber()).isNotEqualTo(second.documentNumber());
        // Consecutive: the numeric suffixes differ by exactly one.
        long firstNo = Long.parseLong(first.documentNumber().substring(3));
        long secondNo = Long.parseLong(second.documentNumber().substring(3));
        assertThat(secondNo - firstNo).isEqualTo(1);
    }

    @Test
    void aPostedInvoiceIsImmutableAndRoundTrips() {
        Invoice posted = invoices.post(newDraft("Widget", "2", "10.00"));

        Invoice reloaded = invoices.getInvoice(posted.id());
        assertThat(reloaded.status()).isEqualTo(InvoiceStatus.POSTED);
        assertThat(reloaded.total()).isEqualTo(Money.of("20.00", "EUR"));
        assertThat(reloaded.documentNumber()).isEqualTo(posted.documentNumber());

        // Posting again is rejected — a posted Beleg is frozen (GoBD).
        assertThatThrownBy(() -> invoices.post(posted.id())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void cancellingPostsANegatedStornoAndLeavesTheOriginalAsCancelled() {
        Invoice original = invoices.post(newDraft("Service", "1", "100.00"));

        Invoice storno = invoices.cancel(original.id());

        assertThat(storno.isStorno()).isTrue();
        assertThat(storno.stornoOf()).isEqualTo(original.id());
        assertThat(storno.status()).isEqualTo(InvoiceStatus.POSTED);
        assertThat(storno.documentNumber()).isNotEqualTo(original.documentNumber()); // own number, same range
        assertThat(storno.total()).isEqualTo(Money.of("-100.00", "EUR"));

        // The original is now CANCELLED, references the storno, and was never deleted (forward-only).
        Invoice reloadedOriginal = invoices.getInvoice(original.id());
        assertThat(reloadedOriginal.status()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(reloadedOriginal.cancelledBy()).isEqualTo(storno.id());
        assertThat(reloadedOriginal.total()).isEqualTo(Money.of("100.00", "EUR")); // unchanged
    }

    @Test
    void bulkPostingInvoicesViaAChangeSetIsReversibleByStorno() {
        // Mass-fakturierung: two drafts posted in one reversible LIVE session (ADR-0006 §2).
        InvoiceId a = newDraft("Order A", "1", "30.00");
        InvoiceId b = newDraft("Order B", "1", "40.00");

        var session = changeSets.beginLive();
        changeSets.postDocuments(session, new de.volantic.erp.changeset.application.BulkPostDocuments(
                "sales.invoice", List.of(a.value(), b.value())));

        assertThat(invoices.getInvoice(a).status()).isEqualTo(InvoiceStatus.POSTED);
        assertThat(invoices.getInvoice(b).status()).isEqualTo(InvoiceStatus.POSTED);

        // The Rollback Engine takes the whole batch back via storno — both originals end up CANCELLED,
        // each referencing its own storno, and neither is deleted (forward-only, GoBD-safe).
        changeSets.revert(session);

        assertThat(invoices.getInvoice(a).status()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(invoices.getInvoice(a).cancelledBy()).isNotNull();
        assertThat(invoices.getInvoice(b).status()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(invoices.getInvoice(b).cancelledBy()).isNotNull();
    }
}
