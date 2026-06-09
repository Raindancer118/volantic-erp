package de.volantic.erp.core;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

/**
 * Common base of all JPA aggregates: an application-assigned {@link UuidV7} id plus optimistic
 * locking via {@code version}.
 *
 * <p>The id is assigned up front for <em>new</em> aggregates (via {@link #AbstractEntity(boolean)}),
 * so keys are fixed before insert and the id is never {@code null} once observed. Crucially, the
 * Hibernate no-arg constructor does <strong>not</strong> generate an id — otherwise every database
 * load would waste a {@link java.security.SecureRandom} draw on a UUID that Hibernate immediately
 * overwrites with the row value (a needless cost on the read hot path / sub-500 ms NFR).
 *
 * <p>Because the id is non-null for new aggregates, Spring Data cannot detect "new" by a null id;
 * {@link Persistable} is therefore implemented with a transient {@code isNew} flag (the standard
 * pattern for assigned ids): {@code true} until the first persist/load, {@code false} afterwards.
 */
@MappedSuperclass
public abstract class AbstractEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Transient
    private boolean isNew;

    /** No-arg constructor for Hibernate only — the id is populated from the row right after instantiation. */
    protected AbstractEntity() {
    }

    /**
     * Business constructor: assigns a fresh {@link UuidV7} id and marks the aggregate as new. The
     * {@code newEntity} parameter only distinguishes this from the Hibernate no-arg constructor;
     * subclasses call {@code super(true)}.
     */
    protected AbstractEntity(boolean newEntity) {
        this.id = UuidV7.randomUuid();
        this.isNew = true;
    }

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
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
