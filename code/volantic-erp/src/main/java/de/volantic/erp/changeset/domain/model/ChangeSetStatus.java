package de.volantic.erp.changeset.domain.model;

/** Lifecycle of a {@link ChangeSet}. Terminal states are {@link #COMMITTED}, {@link #REVERTED}, {@link #DISCARDED}. */
public enum ChangeSetStatus {

    /** Recording operations; the only state in which new operations may be added. */
    OPEN,

    /** Operations are in effect — a LIVE session that was closed, or a Probemodus session "übertragen". */
    COMMITTED,

    /** A LIVE/committed session was taken back via the Rollback Engine (compensated forward). */
    REVERTED,

    /** A Probemodus session was thrown away before commit — nothing ever happened. */
    DISCARDED;

    public boolean isTerminal() {
        return this != OPEN;
    }
}
