package de.volantic.erp.workflow.domain.model;

/** Lifecycle state of an approval process instance. */
public enum ApprovalStatus {
    /** The instance is still running — a reviewer has not decided yet. */
    PENDING,
    APPROVED,
    REJECTED
}
