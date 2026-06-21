package de.volantic.erp.changeset.api;

import de.volantic.erp.changeset.domain.model.ChangeSet;
import de.volantic.erp.changeset.domain.model.ChangeSetMode;
import de.volantic.erp.changeset.domain.model.ChangeSetStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Summary view of a change-set session for the overview list (REST v1): enough to render a row and decide
 * which actions are offered. {@code revertible} tells a client whether the Rollback Engine can still take
 * this session back (a LIVE session that is open or committed) without having to encode the lifecycle
 * rules itself.
 */
public record ChangeSetSummaryResponse(
        UUID id,
        ChangeSetMode mode,
        ChangeSetStatus status,
        String actor,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt,
        int operationCount,
        boolean revertible) {

    public static ChangeSetSummaryResponse from(ChangeSet session) {
        return new ChangeSetSummaryResponse(
                session.id().value(),
                session.mode(),
                session.status(),
                session.actor(),
                session.openedAt(),
                session.closedAt(),
                session.operations().size(),
                isRevertible(session));
    }

    /** A LIVE session can be reverted while OPEN or after COMMITTED; a Probemodus session cannot (discard). */
    static boolean isRevertible(ChangeSet session) {
        return session.mode() == ChangeSetMode.LIVE
                && (session.status() == ChangeSetStatus.OPEN || session.status() == ChangeSetStatus.COMMITTED)
                || session.mode() == ChangeSetMode.DEFERRED && session.status() == ChangeSetStatus.COMMITTED;
    }
}
