package de.volantic.erp.workflow.domain.model;

/**
 * A started approval process instance: its engine-assigned {@code instanceId}, the {@link ApprovalSubject}
 * it concerns and its current {@link ApprovalStatus}.
 */
public record ApprovalInstance(String instanceId, ApprovalSubject subject, ApprovalStatus status) {

    public ApprovalInstance {
        if (instanceId == null || instanceId.isBlank()) {
            throw new IllegalArgumentException("instanceId must not be blank");
        }
        if (subject == null) {
            throw new IllegalArgumentException("subject must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }
}
