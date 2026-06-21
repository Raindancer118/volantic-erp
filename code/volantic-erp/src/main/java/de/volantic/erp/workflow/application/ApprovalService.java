package de.volantic.erp.workflow.application;

import de.volantic.erp.workflow.ApprovalDecided;
import de.volantic.erp.workflow.Approvals;
import de.volantic.erp.workflow.application.port.out.ApprovalEngine;
import de.volantic.erp.workflow.domain.model.ApprovalDecision;
import de.volantic.erp.workflow.domain.model.ApprovalInstance;
import de.volantic.erp.workflow.domain.model.ApprovalSubject;
import de.volantic.erp.workflow.domain.model.PendingApproval;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Approval workflow use cases over the {@link ApprovalEngine} port, and the public {@link Approvals} port
 * for other modules. The engine manages its own transactions, so these methods stay free of
 * {@code @Transactional}. Authorization is enforced at the service boundary (ADR-0004) via
 * {@code workflow.approval:*} permissions. A decision publishes an {@link ApprovalDecided} event so the
 * requesting module can react (ADR-0006 §7).
 */
@Service
public class ApprovalService implements Approvals {

    private final ApprovalEngine engine;
    private final ApplicationEventPublisher events;

    ApprovalService(ApprovalEngine engine, ApplicationEventPublisher events) {
        this.engine = engine;
        this.events = events;
    }

    @PreAuthorize("hasPermission(null, 'workflow.approval:start')")
    public ApprovalInstance requestApproval(ApprovalSubject subject, String requestedBy) {
        return engine.start(subject, requestedBy);
    }

    @Override
    @PreAuthorize("hasPermission(null, 'workflow.approval:start')")
    public String requestApproval(String subjectType, String subjectId, String requestedBy) {
        return engine.start(new ApprovalSubject(subjectType, subjectId), requestedBy).instanceId();
    }

    @PreAuthorize("hasPermission(null, 'workflow.approval:read')")
    public List<PendingApproval> pendingApprovals() {
        return engine.pendingApprovals();
    }

    @PreAuthorize("hasPermission(null, 'workflow.approval:read')")
    public ApprovalInstance getInstance(String instanceId) {
        return engine.findInstance(instanceId)
                .orElseThrow(() -> new WorkflowExceptions.ApprovalInstanceNotFound(instanceId));
    }

    // @Transactional so the ApprovalDecided event is published inside a committing transaction — the
    // requesting module's @ApplicationModuleListener (a durable AFTER_COMMIT listener via the outbox)
    // only fires for events published within one. Flowable's task completion joins this same transaction.
    @Transactional
    @PreAuthorize("hasPermission(null, 'workflow.approval:decide')")
    public void decide(String taskId, ApprovalDecision decision, String decidedBy) {
        // Resolve the subject before completing the task (it is gone from the active list afterwards), so
        // the outcome event can be routed back to the requesting module.
        ApprovalSubject subject = engine.pendingApprovals().stream()
                .filter(task -> task.taskId().equals(taskId))
                .map(PendingApproval::subject)
                .findFirst()
                .orElse(null);

        engine.decide(taskId, decision, decidedBy);

        if (subject != null) {
            events.publishEvent(new ApprovalDecided(
                    subject.type(), subject.id(), decision == ApprovalDecision.APPROVED, decidedBy));
        }
    }
}
