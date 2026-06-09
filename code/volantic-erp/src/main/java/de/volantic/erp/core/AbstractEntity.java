package de.volantic.erp.core;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Common base of all JPA aggregates: an application-assigned {@link UuidV7} id, optimistic locking via
 * {@code version}, and GoBD audit/tracking columns ({@code created_at/by}, {@code modified_at/by})
 * populated automatically by Spring Data JPA auditing (DB architecture §3, by-design traceability).
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
 *
 * <p>The {@code created_at/modified_at} timestamps are filled by Hibernate ({@link CreationTimestamp}/
 * {@link UpdateTimestamp}), so they are always populated — including inside {@code @DataJpaTest} slices
 * that don't load the auditing config. The {@code created_by/modified_by} columns are filled by Spring
 * Data auditing (activated by {@code JpaAuditingConfig}) with the acting OIDC subject and are nullable
 * (left null for unauthenticated/system flows). Timestamps map to {@code timestamptz}.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    private OffsetDateTime modifiedAt;

    @LastModifiedBy
    @Column(name = "modified_by")
    private String modifiedBy;

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
        this(UuidV7.randomUuid());
    }

    /**
     * Business constructor for an externally assigned id (e.g. when the pure domain owns identity and
     * the JPA entity merely mirrors it). Marks the aggregate as new.
     */
    protected AbstractEntity(UUID id) {
        this.id = id;
        this.isNew = true;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getModifiedAt() {
        return modifiedAt;
    }

    public String getModifiedBy() {
        return modifiedBy;
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
