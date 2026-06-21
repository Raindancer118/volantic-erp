package de.volantic.erp.changeset.api;

import de.volantic.erp.changeset.domain.model.RecordedOperation;
import de.volantic.erp.core.revision.ChangeOperation;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One recorded operation inside a session (REST v1 detail view). {@code beforeState} and {@code payload}
 * are the opaque serialized snapshots the resource's handler produced; they are surfaced so a session can
 * be inspected/audited, not interpreted by the api layer.
 *
 * @param targetType  module-qualified resource type, e.g. {@code crm.customer}
 * @param targetId    the affected resource
 * @param operation   what happened (or, in Probemodus, what is intended)
 * @param beforeState serialized state prior to the change (for compensation); {@code null} for CREATE
 * @param payload     the intended change for a buffered Probemodus operation; may be {@code null}
 * @param recordedAt  when the operation was recorded
 */
public record RecordedOperationResponse(
        String targetType,
        UUID targetId,
        ChangeOperation operation,
        String beforeState,
        String payload,
        OffsetDateTime recordedAt) {

    public static RecordedOperationResponse from(RecordedOperation operation) {
        return new RecordedOperationResponse(
                operation.target().type(),
                operation.target().id(),
                operation.operation(),
                operation.beforeState(),
                operation.payload(),
                operation.recordedAt());
    }
}
