package de.volantic.erp.changeset.domain.model;

import de.volantic.erp.core.entitylink.EntityRef;
import de.volantic.erp.core.revision.ChangeOperation;

import java.time.OffsetDateTime;

/**
 * One operation recorded inside a {@link ChangeSet}. Both opaque state strings are produced/consumed by
 * the resource's {@code core.revision} handlers; the domain never interprets them, keeping the module
 * generic.
 *
 * @param target      the affected resource (module-qualified type + id)
 * @param operation   what happened (or, in Probemodus, what is intended to happen)
 * @param beforeState serialized state prior to the change, for compensation; {@code null} for CREATE
 * @param payload     the intended change (e.g. field changes) for a DEFERRED operation; may be {@code null}
 * @param recordedAt  when the operation was recorded into the session
 */
public record RecordedOperation(
        EntityRef target,
        ChangeOperation operation,
        String beforeState,
        String payload,
        OffsetDateTime recordedAt) {

    public RecordedOperation {
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        if (recordedAt == null) {
            throw new IllegalArgumentException("recordedAt must not be null");
        }
        if (operation == ChangeOperation.CREATE && beforeState != null) {
            throw new IllegalArgumentException("CREATE has no before-state");
        }
    }

    /**
     * Returns a copy with the given before-state. Used at commit time of a Probemodus session, where the
     * before-state is only known once the buffered change is actually applied (it was {@code null} while
     * the operation sat buffered), so the Rollback Engine can later compensate it.
     */
    public RecordedOperation withBeforeState(String newBeforeState) {
        return new RecordedOperation(target, operation, newBeforeState, payload, recordedAt);
    }
}
