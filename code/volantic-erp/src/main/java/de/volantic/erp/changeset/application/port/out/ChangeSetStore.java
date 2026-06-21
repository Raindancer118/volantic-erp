package de.volantic.erp.changeset.application.port.out;

import de.volantic.erp.changeset.domain.model.ChangeSet;
import de.volantic.erp.changeset.domain.model.ChangeSetId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/** Outbound port for persisting and loading {@link ChangeSet} aggregates. Implemented in infrastructure. */
public interface ChangeSetStore {

    /** Inserts a new session or updates an existing one (by its id). */
    void save(ChangeSet changeSet);

    /** Loads a session by id, or empty if unknown. */
    Optional<ChangeSet> findById(ChangeSetId id);

    /** Lists a single actor's sessions, newest first, for the session overview / Rollback Engine. */
    Page<ChangeSet> findByActor(String actor, Pageable pageable);
}
