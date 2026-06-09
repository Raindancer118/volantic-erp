package de.volantic.erp.workflow.api;

import de.volantic.erp.workflow.domain.model.ApprovalInstance;

/** Response body representing an approval process instance and its status (REST v1). */
public record ApprovalInstanceResponse(String instanceId, String subjectType, String subjectId, String status) {

    static ApprovalInstanceResponse from(ApprovalInstance instance) {
        return new ApprovalInstanceResponse(
                instance.instanceId(),
                instance.subject().type(),
                instance.subject().id(),
                instance.status().name());
    }
}
