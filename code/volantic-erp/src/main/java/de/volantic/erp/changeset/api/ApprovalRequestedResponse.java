package de.volantic.erp.changeset.api;

/**
 * Response to submitting a Probemodus session for four-eyes approval (REST v1): the workflow approval
 * instance id a reviewer will act on. Once approved, the session is applied and committed automatically.
 */
public record ApprovalRequestedResponse(String approvalInstanceId) {
}
