package de.volantic.erp.changeset.infrastructure.persistence;

import de.volantic.erp.changeset.domain.model.ChangeSetMode;
import de.volantic.erp.changeset.domain.model.ChangeSetStatus;
import de.volantic.erp.core.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA representation of a {@link de.volantic.erp.changeset.domain.model.ChangeSet}. Table
 * {@code changeset.change_set}. The recorded operations are stored as a JSON document in {@code operations}
 * (opaque to the database); {@code status}/{@code closed_at}/{@code operations} are the only mutable
 * columns — actor, mode and opened_at are fixed once the session is opened.
 */
@Entity
@Table(schema = "changeset", name = "change_set")
class ChangeSetEntity extends AbstractEntity {

    @Column(name = "actor", nullable = false, updatable = false)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, updatable = false, length = 20)
    private ChangeSetMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChangeSetStatus status;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "operations", nullable = false)
    private String operations;

    protected ChangeSetEntity() {
    }

    private ChangeSetEntity(UUID id, String actor, ChangeSetMode mode, OffsetDateTime openedAt) {
        super(id);
        this.actor = actor;
        this.mode = mode;
        this.openedAt = openedAt;
    }

    static ChangeSetEntity forNew(UUID id, String actor, ChangeSetMode mode, OffsetDateTime openedAt) {
        return new ChangeSetEntity(id, actor, mode, openedAt);
    }

    void setStatus(ChangeSetStatus status) {
        this.status = status;
    }

    void setClosedAt(OffsetDateTime closedAt) {
        this.closedAt = closedAt;
    }

    void setOperations(String operations) {
        this.operations = operations;
    }

    String getActor() {
        return actor;
    }

    ChangeSetMode getMode() {
        return mode;
    }

    ChangeSetStatus getStatus() {
        return status;
    }

    OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    OffsetDateTime getClosedAt() {
        return closedAt;
    }

    String getOperations() {
        return operations;
    }
}
