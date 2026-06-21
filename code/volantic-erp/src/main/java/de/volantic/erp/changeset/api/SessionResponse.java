package de.volantic.erp.changeset.api;

import de.volantic.erp.changeset.domain.model.ChangeSetId;
import de.volantic.erp.changeset.domain.model.ChangeSetMode;

import java.util.UUID;

/** Response body for a freshly opened session (REST v1): the id to address it and the mode it runs in. */
public record SessionResponse(UUID id, ChangeSetMode mode) {

    public static SessionResponse of(ChangeSetId id, ChangeSetMode mode) {
        return new SessionResponse(id.value(), mode);
    }
}
