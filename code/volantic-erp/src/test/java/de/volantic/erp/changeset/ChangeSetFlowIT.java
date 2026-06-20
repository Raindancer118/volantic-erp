package de.volantic.erp.changeset;

import de.volantic.erp.changeset.application.BulkChange;
import de.volantic.erp.changeset.application.BulkCreate;
import de.volantic.erp.changeset.application.BulkDelete;
import de.volantic.erp.changeset.application.BulkPreview;
import de.volantic.erp.changeset.application.ChangeSetService;
import de.volantic.erp.changeset.domain.model.ChangeSetId;
import de.volantic.erp.changeset.domain.model.ChangeSetStatus;
import de.volantic.erp.crm.application.ContactService;
import de.volantic.erp.crm.application.CustomerService;
import de.volantic.erp.crm.domain.model.Contact;
import de.volantic.erp.crm.domain.model.ContactId;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.SecurityAdmin;
import de.volantic.erp.workflow.application.ApprovalService;
import de.volantic.erp.workflow.domain.model.ApprovalDecision;
import de.volantic.erp.workflow.domain.model.PendingApproval;
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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

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
    private static final String REVIEWER = "approval-reviewer-bob";

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

    @Autowired
    private ContactService contacts;

    @Autowired
    private ApprovalService approvals;

    @BeforeEach
    void seedRbacAndAuthenticate() {
        if (!rbacSeeded) {
            Set<String> requesterPermissions = Set.of(
                    "changeset.bulk:execute", "changeset.probemodus:activate", "changeset.rollback:revert",
                    "crm.customer:create", "crm.customer:read", "crm.customer:update",
                    "crm.contact:read", "crm.contact:write",
                    "workflow.approval:start", "workflow.approval:read");
            // The reviewer can only decide approvals — deliberately NOT crm.customer:update, so the apply
            // can only succeed if it runs as the requester (proves the listener impersonates correctly).
            Set<String> reviewerPermissions = Set.of("workflow.approval:read", "workflow.approval:decide");

            java.util.stream.Stream.concat(requesterPermissions.stream(), reviewerPermissions.stream())
                    .distinct().forEach(key -> securityAdmin.definePermission(key, key));
            securityAdmin.defineRole("changeset-admin", "Change-set admin", requesterPermissions);
            securityAdmin.defineRole("approval-reviewer", "Approval reviewer", reviewerPermissions);
            securityAdmin.provisionUser(ACTOR, "tester", "tester@volantic.de");
            securityAdmin.provisionUser(REVIEWER, "reviewer", "reviewer@volantic.de");
            securityAdmin.assignRole(ACTOR, "changeset-admin", AccessScope.GLOBAL);
            securityAdmin.assignRole(REVIEWER, "approval-reviewer", AccessScope.GLOBAL);
            rbacSeeded = true;
        }
        authenticateAs(ACTOR);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(String oidcSubject) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(oidcSubject, "n/a", AuthorityUtils.NO_AUTHORITIES));
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
    void bulkCreateTakesEffectAndIsReversedByDeleting() {
        CustomerId owner = newCustomer("C-OWNER-1", "Owner GmbH");
        Map<String, String> ownerRef = Map.of("ownerType", "CUSTOMER", "ownerId", owner.value().toString());

        ChangeSetId session = changeSets.beginLive();
        List<UUID> created = changeSets.createBulk(session, new BulkCreate("crm.contact", List.of(
                contactRecord(ownerRef, "Ann", "Meyer"),
                contactRecord(ownerRef, "Bob", "Schulz"))));

        assertThat(created).hasSize(2);
        assertThat(contacts.getContact(new ContactId(created.get(0))).firstName()).isEqualTo("Ann");

        changeSets.revert(session);

        // Both created contacts are taken back (deleted).
        for (UUID id : created) {
            assertThatThrownBy(() -> contacts.getContact(new ContactId(id))).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    void bulkDeleteTakesEffectAndIsReversedByRecreatingWithTheSameId() {
        PartnerRef owner = PartnerRef.of(PartnerType.CUSTOMER, newCustomer("C-OWNER-2", "Owner2 GmbH").value());
        Contact contact = contacts.createContact(owner, "Clara", "Nguyen", "clara@acme.de", null);
        ContactId id = contact.id();

        ChangeSetId session = changeSets.beginLive();
        changeSets.deleteBulk(session, BulkDelete.byIds("crm.contact", List.of(id.value())));
        assertThatThrownBy(() -> contacts.getContact(id)).isInstanceOf(RuntimeException.class);

        changeSets.revert(session);

        // The Rollback Engine re-created it with the same id and data (forward-only compensation).
        Contact restored = contacts.getContact(id);
        assertThat(restored.firstName()).isEqualTo("Clara");
        assertThat(restored.lastName()).isEqualTo("Nguyen");
        assertThat(restored.owner()).isEqualTo(owner);
    }

    private static Map<String, String> contactRecord(Map<String, String> ownerRef, String first, String last) {
        return Map.of("ownerType", ownerRef.get("ownerType"), "ownerId", ownerRef.get("ownerId"),
                "firstName", first, "lastName", last, "email", "", "phone", "");
    }

    @Test
    void probemodusViaFourEyesApprovalAppliesOnlyAfterAReviewerApproves() {
        CustomerId id = newCustomer("C-APPROVE-1", "PendingCo");

        ChangeSetId session = changeSets.beginProbemodus();
        changeSets.apply(session, new BulkChange(TYPE, List.of(id.value()), Map.of("name", "ApprovedCo")));
        assertThat(nameOf(id)).isEqualTo("PendingCo"); // buffered — nothing written yet

        String approvalInstance = changeSets.requestApproval(session);
        assertThat(nameOf(id)).isEqualTo("PendingCo"); // still not applied — awaiting sign-off

        decideAsReviewer(approvalInstance, ApprovalDecision.APPROVED);

        // The approval triggers the async listener, which applies the buffered change AS the requester
        // (the reviewer has no crm.customer:update) — so success proves the impersonation works.
        await().pollInSameThread().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(nameOf(id)).isEqualTo("ApprovedCo"));
    }

    @Test
    void rejectedApprovalDiscardsTheSessionWithoutApplying() {
        CustomerId id = newCustomer("C-APPROVE-2", "KeepCo");

        ChangeSetId session = changeSets.beginProbemodus();
        changeSets.apply(session, new BulkChange(TYPE, List.of(id.value()), Map.of("name", "Nope")));
        String approvalInstance = changeSets.requestApproval(session);

        decideAsReviewer(approvalInstance, ApprovalDecision.REJECTED);

        await().pollInSameThread().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(changeSets.getSession(session).status().name()).isEqualTo("DISCARDED"));
        assertThat(nameOf(id)).isEqualTo("KeepCo"); // never applied
    }

    /** Switches to the reviewer, decides the approval's pending task, then switches back to the requester. */
    private void decideAsReviewer(String approvalInstanceId, ApprovalDecision decision) {
        authenticateAs(REVIEWER);
        try {
            PendingApproval task = approvals.pendingApprovals().stream()
                    .filter(t -> t.instanceId().equals(approvalInstanceId))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no pending task for " + approvalInstanceId));
            approvals.decide(task.taskId(), decision, REVIEWER);
        } finally {
            authenticateAs(ACTOR);
        }
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
