package de.volantic.erp.workflow;

/**
 * Public inbound port of the workflow module for other modules to route a business object through the
 * generic four-eyes approval process (ADR-0006 §7). Kept string-typed at the module root so callers never
 * touch the workflow module's internal types; the outcome is delivered asynchronously as an
 * {@link ApprovalDecided} event.
 */
public interface Approvals {

    /**
     * Starts an approval for the given subject (a free-form {@code type}+{@code id}); returns the approval
     * instance id. When a reviewer decides, an {@link ApprovalDecided} event is published for the subject.
     */
    String requestApproval(String subjectType, String subjectId, String requestedBy);
}
