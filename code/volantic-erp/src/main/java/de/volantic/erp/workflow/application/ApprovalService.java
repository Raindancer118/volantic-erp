package de.volantic.erp.workflow.application;

import de.volantic.erp.workflow.application.port.out.ApprovalEngine;
import de.volantic.erp.workflow.domain.model.ApprovalDecision;
import de.volantic.erp.workflow.domain.model.ApprovalInstance;
import de.volantic.erp.workflow.domain.model.ApprovalSubject;
import de.volantic.erp.workflow.domain.model.PendingApproval;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Approval workflow use cases over the {@link ApprovalEngine} port. The engine manages its own
 * transactions, so these methods stay free of {@code @Transactional}. Authorization is enforced at the
 * service boundary (ADR-0004) via {@code workflow.approval:*} permissions.
 */
@Service
public class ApprovalService {

    private final ApprovalEngine engine;

    ApprovalService(ApprovalEngine engine) {
        this.engine = engine;
    }

    @PreAuthorize("hasPermission(null, 'workflow.approval:start')")
    public ApprovalInstance requestApproval(ApprovalSubject subject, String requestedBy) {
        return engine.start(subject, requestedBy);
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

    @PreAuthorize("hasPermission(null, 'workflow.approval:decide')")
    public void decide(String taskId, ApprovalDecision decision, String decidedBy) {
        engine.decide(taskId, decision, decidedBy);
    }
}
