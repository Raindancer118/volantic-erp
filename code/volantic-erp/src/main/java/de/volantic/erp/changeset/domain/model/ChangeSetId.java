package de.volantic.erp.changeset.domain.model;

import de.volantic.erp.core.UuidV7;

import java.util.UUID;

/** Identity of a {@link ChangeSet}. Time-ordered (UUIDv7) so sessions sort naturally by creation. */
public record ChangeSetId(UUID value) {

    public ChangeSetId {
        if (value == null) {
            throw new IllegalArgumentException("change set id must not be null");
        }
    }

    /** Mints a fresh, time-ordered identity for a new session. */
    public static ChangeSetId newId() {
        return new ChangeSetId(UuidV7.randomUuid());
    }
}
