package de.volantic.erp.changeset;

import de.volantic.erp.changeset.application.BulkChange;
import de.volantic.erp.changeset.application.BulkPreview;
import de.volantic.erp.changeset.application.ChangeSetService;
import de.volantic.erp.changeset.domain.model.ChangeSetId;
import de.volantic.erp.changeset.domain.model.ChangeSetStatus;
import de.volantic.erp.crm.application.CustomerService;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
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

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test of the whole change-set machinery (ADR-0006) against a real PostgreSQL
 * (Testcontainers), driving the <em>real</em> {@link ChangeSetService} through the <em>real</em>
 * {@code @PreAuthorize} authorization and the <em>real</em> {@code core.revision} handler discovered for
 * {@code crm.customer}. This is the proof the feature works at runtime, not just that it compiles: it
 * seeds RBAC through the security admin API, authenticates as that user, then exercises
 *
 * <ul>
 *   <li>a LIVE session: apply a mass edit, then take it back with the Rollback Engine;</li>
 *   <li>the Probemodus: buffer (nothing written), then commit ("Übertragen") so it takes effect;</li>
 *   <li>discard: a buffered Probemodus session is thrown away with no effect.</li>
 * </ul>
 *
 * <p>Skipped without a Docker daemon; runs in CI.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ChangeSetFlowIT {

    private static final String TYPE = "crm.customer";
    private static final String ACTOR = "changeset-tester";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static boolean rbacSeeded;

    @Autowired
    private SecurityAdmin securityAdmin;

    @Autowired
    private CustomerService customers;

    @Autowired
    private ChangeSetService changeSets;

    @BeforeEach
    void seedRbacAndAuthenticate() {
        if (!rbacSeeded) {
            Set.of("changeset.bulk:execute", "changeset.probemodus:activate", "changeset.rollback:revert",
                    "crm.customer:create", "crm.customer:read", "crm.customer:update")
                    .forEach(key -> securityAdmin.definePermission(key, key));
            securityAdmin.defineRole("changeset-admin", "Change-set admin", Set.of(
                    "changeset.bulk:execute", "changeset.probemodus:activate", "changeset.rollback:revert",
                    "crm.customer:create", "crm.customer:read", "crm.customer:update"));
            securityAdmin.provisionUser(ACTOR, "tester", "tester@volantic.de");
            securityAdmin.assignRole(ACTOR, "changeset-admin", AccessScope.GLOBAL);
            rbacSeeded = true;
        }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ACTOR, "n/a", AuthorityUtils.NO_AUTHORITIES));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private CustomerId newCustomer(String number, String name) {
        return newCustomer(number, name, "info@acme.de");
    }

    private CustomerId newCustomer(String number, String name, String email) {
        return customers.createCustomer(number, name, email).id();
    }

    private String nameOf(CustomerId id) {
        return customers.getCustomer(id).name();
    }

    @Test
    void liveMassEditTakesEffectAndIsReversedByTheRollbackEngine() {
        CustomerId id = newCustomer("C-LIVE-1", "Acme");

        ChangeSetId session = changeSets.beginLive();
        changeSets.apply(session, new BulkChange(TYPE, List.of(id.value()), Map.of("name", "Acme Corp")));
        assertThat(nameOf(id)).isEqualTo("Acme Corp");

        changeSets.revert(session);

        assertThat(nameOf(id)).isEqualTo("Acme");
        Customer restored = customers.getCustomer(id);
        assertThat(restored.email()).isEqualTo("info@acme.de"); // untouched field preserved
        assertThat(changeSets.getSession(session).status()).isEqualTo(ChangeSetStatus.REVERTED);
    }

    @Test
    void probemodusWritesNothingUntilCommitted() {
        CustomerId id = newCustomer("C-PROBE-1", "Globex");

        ChangeSetId session = changeSets.beginProbemodus();

        BulkPreview preview = changeSets.preview(new BulkChange(TYPE, List.of(id.value()), Map.of("name", "Globex AG")));
        assertThat(preview.hasProblems()).isFalse();

        changeSets.apply(session, new BulkChange(TYPE, List.of(id.value()), Map.of("name", "Globex AG")));
        assertThat(nameOf(id)).isEqualTo("Globex"); // buffered: nothing written yet

        changeSets.commit(session);
        assertThat(nameOf(id)).isEqualTo("Globex AG"); // "Übertragen": now in effect

        changeSets.revert(session); // a committed Probemodus session is still revertible
        assertThat(nameOf(id)).isEqualTo("Globex");
    }

    @Test
    void discardingAProbemodusSessionHasNoEffect() {
        CustomerId id = newCustomer("C-PROBE-2", "Initech");

        ChangeSetId session = changeSets.beginProbemodus();
        changeSets.apply(session, new BulkChange(TYPE, List.of(id.value()), Map.of("name", "Initech GmbH")));

        changeSets.discard(session);

        assertThat(nameOf(id)).isEqualTo("Initech");
        assertThat(changeSets.getSession(session).status()).isEqualTo(ChangeSetStatus.DISCARDED);
    }

    @Test
    void filteredLiveMassEditChangesAllMatchesAndIsReversible() {
        // Two customers share a unique e-mail so a filter selects exactly them (ADR-0006 §5).
        String email = "filter-group@volantic.de";
        CustomerId first = newCustomer("C-FILTER-1", "Alpha", email);
        CustomerId second = newCustomer("C-FILTER-2", "Beta", email);

        ChangeSetId session = changeSets.beginLive();
        changeSets.apply(session, BulkChange.byFilter(TYPE, Map.of("email", email), Map.of("name", "Renamed")));

        assertThat(nameOf(first)).isEqualTo("Renamed");
        assertThat(nameOf(second)).isEqualTo("Renamed");

        changeSets.revert(session);

        assertThat(nameOf(first)).isEqualTo("Alpha");
        assertThat(nameOf(second)).isEqualTo("Beta");
    }

    @Test
    void previewFlagsMissingResources() {
        ChangeSetId ignored = changeSets.beginLive();
        assertThat(ignored).isNotNull();

        BulkPreview preview = changeSets.preview(
                new BulkChange(TYPE, List.of(java.util.UUID.randomUUID()), Map.of("name", "Ghost")));

        assertThat(preview.hasProblems()).isTrue();
        assertThat(preview.rows()).singleElement().satisfies(row -> assertThat(row.applicable()).isFalse());
    }
}
