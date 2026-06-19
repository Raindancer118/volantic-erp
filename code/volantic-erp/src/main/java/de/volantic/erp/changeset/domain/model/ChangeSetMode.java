package de.volantic.erp.changeset.domain.model;

/** Whether a {@link ChangeSet}'s operations take effect immediately or only on explicit commit. */
public enum ChangeSetMode {

    /** Changes are applied immediately; the session can be taken back later (Rollback Engine). */
    LIVE,

    /** Probemodus: nothing is written for real until the session is committed ("Übertragen"). */
    DEFERRED
}
