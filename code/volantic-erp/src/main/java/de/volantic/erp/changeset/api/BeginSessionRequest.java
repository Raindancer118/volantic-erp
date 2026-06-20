package de.volantic.erp.changeset.api;

import de.volantic.erp.changeset.domain.model.ChangeSetMode;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for opening a change-set session (REST v1). {@code LIVE} starts the Rollback Engine path
 * (applied immediately, reversible); {@code DEFERRED} activates the Probemodus (nothing written until
 * commit). An unknown value fails deserialization and yields a 400.
 */
public record BeginSessionRequest(@NotNull ChangeSetMode mode) {
}
