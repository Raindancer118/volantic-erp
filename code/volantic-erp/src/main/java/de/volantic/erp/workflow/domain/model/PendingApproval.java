package de.volantic.erp.workflow.domain.model;

/**
 * An open review task waiting for a decision: the engine-assigned {@code taskId}, the owning process
 * {@code instanceId} and the {@link ApprovalSubject} under review.
 */
public record PendingApproval(String taskId, String instanceId, ApprovalSubject subject) {

    public PendingApproval {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        if (instanceId == null || instanceId.isBlank()) {
            throw new IllegalArgumentException("instanceId must not be blank");
        }
        if (subject == null) {
            throw new IllegalArgumentException("subject must not be null");
        }
    }
}
