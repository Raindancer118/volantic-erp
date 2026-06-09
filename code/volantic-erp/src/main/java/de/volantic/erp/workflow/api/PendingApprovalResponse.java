package de.volantic.erp.workflow.api;

import de.volantic.erp.workflow.domain.model.PendingApproval;

/** Response body representing an open review task (REST v1). */
public record PendingApprovalResponse(String taskId, String instanceId, String subjectType, String subjectId) {

    static PendingApprovalResponse from(PendingApproval pending) {
        return new PendingApprovalResponse(
                pending.taskId(),
                pending.instanceId(),
                pending.subject().type(),
                pending.subject().id());
    }
}
