package de.volantic.erp.workflow.application.port.out;

import de.volantic.erp.workflow.domain.model.ApprovalDecision;
import de.volantic.erp.workflow.domain.model.ApprovalInstance;
import de.volantic.erp.workflow.domain.model.ApprovalSubject;
import de.volantic.erp.workflow.domain.model.PendingApproval;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port to the BPMN workflow engine for the generic approval process. The implementation
 * (infrastructure) adapts Flowable; the application depends only on this domain-shaped interface.
 */
public interface ApprovalEngine {

    /** Starts an approval process for {@code subject}, requested by {@code requestedBy}; returns the running instance. */
    ApprovalInstance start(ApprovalSubject subject, String requestedBy);

    /** All open review tasks across running approval instances. */
    List<PendingApproval> pendingApprovals();

    /** Records {@code decision} on the open task {@code taskId}, advancing (and ending) the instance. */
    void decide(String taskId, ApprovalDecision decision, String decidedBy);

    /** The current state of the instance, or empty if no such approval instance exists (running or historic). */
    Optional<ApprovalInstance> findInstance(String instanceId);
}
