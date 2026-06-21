package de.volantic.erp.changeset.domain.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A grouped, reversible edit session — the aggregate root behind the Rollback Engine and the Probemodus
 * (ADR-0006). Pure domain: it owns its identity, the recorded operations and the lifecycle invariants,
 * free of persistence and framework concerns. The actual writing/compensation is driven by the
 * application service through the {@code core.revision} handlers.
 *
 * <p>Lifecycle: a session is {@code OPEN} while recording. From there:
 * <ul>
 *   <li>{@link #commit()} closes it — a LIVE session (already applied) or a Probemodus session being
 *       "übertragen".</li>
 *   <li>{@link #revert()} takes a LIVE/committed session back via forward compensation.</li>
 *   <li>{@link #discard()} throws away a Probemodus session before anything was applied.</li>
 * </ul>
 */
public final class ChangeSet {

    private final ChangeSetId id;
    private final String actor;
    private final ChangeSetMode mode;
    private final OffsetDateTime openedAt;
    private final List<RecordedOperation> operations;
    private ChangeSetStatus status;
    private OffsetDateTime closedAt;

    private ChangeSet(ChangeSetId id, String actor, ChangeSetMode mode, ChangeSetStatus status,
                      OffsetDateTime openedAt, OffsetDateTime closedAt, List<RecordedOperation> operations) {
        this.id = id;
        this.actor = requireText(actor, "actor");
        this.mode = requireNonNull(mode, "mode");
        this.status = requireNonNull(status, "status");
        this.openedAt = requireNonNull(openedAt, "openedAt");
        this.closedAt = closedAt;
        this.operations = new ArrayList<>(operations);
    }

    /** Opens a fresh session for the given actor in the given mode. */
    public static ChangeSet open(String actor, ChangeSetMode mode) {
        return new ChangeSet(ChangeSetId.newId(), actor, mode, ChangeSetStatus.OPEN,
                OffsetDateTime.now(ZoneOffset.UTC), null, List.of());
    }

    /** Re-creates a persisted session (used by the persistence adapter). */
    public static ChangeSet reconstitute(ChangeSetId id, String actor, ChangeSetMode mode, ChangeSetStatus status,
                                          OffsetDateTime openedAt, OffsetDateTime closedAt,
                                          List<RecordedOperation> operations) {
        return new ChangeSet(requireNonNull(id, "id"), actor, mode, status, openedAt, closedAt, operations);
    }

    /**
     * Replaces the buffered operations with versions enriched by the before-state captured at commit
     * time. For a Probemodus session the before-state is unknown while operations sit buffered (nothing
     * is written yet); capturing it as the changes are applied lets the Rollback Engine compensate a
     * committed Probemodus session later. Only allowed while {@code OPEN} and the order/size must match.
     */
    public void replaceWithCaptured(List<RecordedOperation> capturedInOrder) {
        if (status != ChangeSetStatus.OPEN && status != ChangeSetStatus.AWAITING_APPROVAL) {
            throw new IllegalStateException("cannot capture before-states into a " + status + " change set");
        }
        if (capturedInOrder.size() != operations.size()) {
            throw new IllegalArgumentException("captured operations must match the recorded operations");
        }
        operations.clear();
        operations.addAll(capturedInOrder);
    }

    /** Records one operation. Only allowed while the session is {@code OPEN}. */
    public void record(RecordedOperation operation) {
        requireNonNull(operation, "operation");
        if (status != ChangeSetStatus.OPEN) {
            throw new IllegalStateException("cannot record into a " + status + " change set");
        }
        operations.add(operation);
    }

    /**
     * Closes the session as committed: a LIVE session stops recording, a Probemodus session is
     * "übertragen". The actual application of buffered operations is the service's job; this only
     * advances the lifecycle.
     */
    public void commit() {
        transitionFromOpen(ChangeSetStatus.COMMITTED);
    }

    /**
     * Takes the session back via the Rollback Engine. Allowed for a LIVE session (whether still
     * {@code OPEN} or already {@code COMMITTED}) — a Probemodus session that has not been committed has
     * nothing applied to revert and must be {@link #discard() discarded} instead.
     */
    public void revert() {
        if (status == ChangeSetStatus.OPEN && mode != ChangeSetMode.LIVE) {
            throw new IllegalStateException("an open Probemodus session is discarded, not reverted");
        }
        if (status != ChangeSetStatus.OPEN && status != ChangeSetStatus.COMMITTED) {
            throw new IllegalStateException("cannot revert a " + status + " change set");
        }
        this.status = ChangeSetStatus.REVERTED;
        this.closedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Throws away a Probemodus session before commit. Valid for a DEFERRED session that is still open or
     * awaiting approval (so a session can be withdrawn after it was submitted for sign-off).
     */
    public void discard() {
        if (mode != ChangeSetMode.DEFERRED) {
            throw new IllegalStateException("only a Probemodus (DEFERRED) session can be discarded");
        }
        if (status != ChangeSetStatus.OPEN && status != ChangeSetStatus.AWAITING_APPROVAL) {
            throw new IllegalStateException("change set is already " + status);
        }
        this.status = ChangeSetStatus.DISCARDED;
        this.closedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Submits a Probemodus session for four-eyes approval (ADR-0006 §7): no more operations may be added,
     * and it can only be applied once a reviewer approves it.
     */
    public void submitForApproval() {
        if (mode != ChangeSetMode.DEFERRED) {
            throw new IllegalStateException("only a Probemodus (DEFERRED) session can require approval");
        }
        if (status != ChangeSetStatus.OPEN) {
            throw new IllegalStateException("only an open session can be submitted for approval, not " + status);
        }
        this.status = ChangeSetStatus.AWAITING_APPROVAL;
    }

    /** Marks a session committed after approval (the service applies its buffered operations first). */
    public void approve() {
        if (status != ChangeSetStatus.AWAITING_APPROVAL) {
            throw new IllegalStateException("only a session awaiting approval can be approved, not " + status);
        }
        this.status = ChangeSetStatus.COMMITTED;
        this.closedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /** Discards a session after its approval was rejected — nothing is applied. */
    public void rejectApproval() {
        if (status != ChangeSetStatus.AWAITING_APPROVAL) {
            throw new IllegalStateException("only a session awaiting approval can be rejected, not " + status);
        }
        this.status = ChangeSetStatus.DISCARDED;
        this.closedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /** The recorded operations in the order they must be compensated: newest first. */
    public List<RecordedOperation> operationsForReversal() {
        List<RecordedOperation> reversed = new ArrayList<>(operations);
        Collections.reverse(reversed);
        return Collections.unmodifiableList(reversed);
    }

    /** The recorded operations in the order they were recorded. */
    public List<RecordedOperation> operations() {
        return Collections.unmodifiableList(operations);
    }

    public ChangeSetId id() {
        return id;
    }

    public String actor() {
        return actor;
    }

    public ChangeSetMode mode() {
        return mode;
    }

    public ChangeSetStatus status() {
        return status;
    }

    public OffsetDateTime openedAt() {
        return openedAt;
    }

    public OffsetDateTime closedAt() {
        return closedAt;
    }

    /** Identity equality: two sessions are the same iff they share an id, regardless of lifecycle state. */
    @Override
    public boolean equals(Object other) {
        return other instanceof ChangeSet that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    private void transitionFromOpen(ChangeSetStatus target) {
        if (status != ChangeSetStatus.OPEN) {
            throw new IllegalStateException("change set is already " + status);
        }
        this.status = target;
        this.closedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
