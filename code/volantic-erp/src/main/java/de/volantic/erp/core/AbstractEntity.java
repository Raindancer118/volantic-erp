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
 * Gemeinsame Basis aller JPA-Aggregate: anwendungsseitig vergebene {@link UuidV7}-ID plus
 * optimistisches Sperren über {@code version}.
 *
 * <p>Da die ID schon im Konstruktor gesetzt wird (nie {@code null}), kann Spring Data ein neues
 * Aggregat nicht mehr an einer {@code null}-ID erkennen. Deshalb wird {@link Persistable} mit einem
 * transienten {@code isNew}-Flag implementiert (Standardmuster für zugewiesene IDs): {@code true} bis
 * zum ersten Persist/Load, danach {@code false} — so wird sauber zwischen INSERT und UPDATE getrennt.
 */
@MappedSuperclass
public abstract class AbstractEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UuidV7.randomUuid(); // beim Laden aus der DB von Hibernate überschrieben

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
