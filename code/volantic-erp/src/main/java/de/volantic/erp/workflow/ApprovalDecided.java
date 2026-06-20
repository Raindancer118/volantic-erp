package de.volantic.erp.workflow;

/**
 * Published when a reviewer decides an approval (ADR-0006 §7). String-typed and at the module root so any
 * module can listen without depending on the workflow module's internals. The requesting module matches on
 * {@code subjectType}/{@code subjectId} (the values it passed to {@link Approvals#requestApproval}).
 *
 * @param subjectType the business object's type, e.g. {@code "changeset.session"}
 * @param subjectId   the business object's id
 * @param approved    {@code true} if approved, {@code false} if rejected
 * @param decidedBy   the reviewer (OIDC subject) who decided
 */
public record ApprovalDecided(String subjectType, String subjectId, boolean approved, String decidedBy) {
}
