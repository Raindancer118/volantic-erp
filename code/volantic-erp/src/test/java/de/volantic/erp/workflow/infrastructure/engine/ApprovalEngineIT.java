package de.volantic.erp.workflow.infrastructure.engine;

import de.volantic.erp.workflow.application.port.out.ApprovalEngine;
import de.volantic.erp.workflow.domain.model.ApprovalDecision;
import de.volantic.erp.workflow.domain.model.ApprovalInstance;
import de.volantic.erp.workflow.domain.model.ApprovalStatus;
import de.volantic.erp.workflow.domain.model.ApprovalSubject;
import de.volantic.erp.workflow.domain.model.PendingApproval;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Engine smoke/integration test for the Flowable-backed {@link ApprovalEngine}: boots the full context
 * (so Flowable creates its ACT_* schema and auto-deploys {@code approval.bpmn20.xml} alongside Flyway's
 * own schemas — proving there is no schema/ddl-validate conflict), then drives a complete approval
 * through both the approved and the rejected branch. Skipped without Docker; runs in CI.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ApprovalEngineIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ApprovalEngine engine;

    private PendingApproval pendingTaskFor(String instanceId) {
        return engine.pendingApprovals().stream()
                .filter(task -> task.instanceId().equals(instanceId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no pending task for instance " + instanceId));
    }

    @Test
    void approvedBranchEndsApproved() {
        ApprovalSubject subject = new ApprovalSubject("purchase-order", "PO-1001");
        ApprovalInstance started = engine.start(subject, "alice");

        assertThat(started.status()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(engine.findInstance(started.instanceId()))
                .get().extracting(ApprovalInstance::status).isEqualTo(ApprovalStatus.PENDING);

        PendingApproval task = pendingTaskFor(started.instanceId());
        assertThat(task.subject()).isEqualTo(subject);

        engine.decide(task.taskId(), ApprovalDecision.APPROVED, "bob");

        assertThat(engine.findInstance(started.instanceId()))
                .get()
                .satisfies(instance -> {
                    assertThat(instance.status()).isEqualTo(ApprovalStatus.APPROVED);
                    assertThat(instance.subject()).isEqualTo(subject);
                });
    }

    @Test
    void rejectedBranchEndsRejected() {
        ApprovalInstance started = engine.start(new ApprovalSubject("purchase-order", "PO-2002"), "alice");

        PendingApproval task = pendingTaskFor(started.instanceId());
        engine.decide(task.taskId(), ApprovalDecision.REJECTED, "bob");

        assertThat(engine.findInstance(started.instanceId()))
                .get().extracting(ApprovalInstance::status).isEqualTo(ApprovalStatus.REJECTED);
    }

    @Test
    void findInstanceIsEmptyForUnknownId() {
        assertThat(engine.findInstance("does-not-exist")).isEmpty();
    }
}
