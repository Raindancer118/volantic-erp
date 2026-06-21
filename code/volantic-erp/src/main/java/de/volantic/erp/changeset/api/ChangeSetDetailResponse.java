package de.volantic.erp.changeset.api;

import de.volantic.erp.changeset.domain.model.ChangeSet;
import de.volantic.erp.changeset.domain.model.ChangeSetMode;
import de.volantic.erp.changeset.domain.model.ChangeSetStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full view of a single change-set session (REST v1): the summary fields plus every recorded operation in
 * record order, so a session can be inspected before it is committed, discarded or reverted.
 */
public record ChangeSetDetailResponse(
        UUID id,
        ChangeSetMode mode,
        ChangeSetStatus status,
        String actor,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt,
        boolean revertible,
        List<RecordedOperationResponse> operations) {

    public static ChangeSetDetailResponse from(ChangeSet session) {
        List<RecordedOperationResponse> operations = session.operations().stream()
                .map(RecordedOperationResponse::from)
                .toList();
        return new ChangeSetDetailResponse(
                session.id().value(),
                session.mode(),
                session.status(),
                session.actor(),
                session.openedAt(),
                session.closedAt(),
                ChangeSetSummaryResponse.isRevertible(session),
                operations);
    }
}
