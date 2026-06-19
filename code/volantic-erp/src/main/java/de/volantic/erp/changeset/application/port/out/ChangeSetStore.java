package de.volantic.erp.changeset.application.port.out;

import de.volantic.erp.changeset.domain.model.ChangeSet;
import de.volantic.erp.changeset.domain.model.ChangeSetId;

import java.util.Optional;

/** Outbound port for persisting and loading {@link ChangeSet} aggregates. Implemented in infrastructure. */
public interface ChangeSetStore {

    /** Inserts a new session or updates an existing one (by its id). */
    void save(ChangeSet changeSet);

    /** Loads a session by id, or empty if unknown. */
    Optional<ChangeSet> findById(ChangeSetId id);
}
