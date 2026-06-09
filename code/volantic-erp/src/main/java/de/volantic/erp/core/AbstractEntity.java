package de.volantic.erp.core;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import org.springframework.data.domain.Persistable;

import java.util.Objects;
import java.util.UUID;

/**
 * Common base of all JPA aggregates: an application-assigned {@link UuidV7} id plus optimistic
 * locking via {@code version}.
 *
 * <p>Because the id is already set in the constructor (never {@code null}), Spring Data can no longer
 * detect a new aggregate by a {@code null} id. Therefore {@link Persistable} is implemented with a
 * transient {@code isNew} flag (the standard pattern for assigned ids): {@code true} until the first
 * persist/load, {@code false} afterwards — cleanly separating INSERT from UPDATE.
 */
@MappedSuperclass
public abstract class AbstractEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UuidV7.randomUuid(); // overwritten by Hibernate when loaded from the DB

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Transient
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AbstractEntity that)) {
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
