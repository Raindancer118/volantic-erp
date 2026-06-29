package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.core.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * JPA representation of an organizational unit. Table {@code security.org_unit}; the id is assigned by
 * the domain. {@code parentId} is a plain UUID (the self-referential tree is navigated via edge queries,
 * not JPA associations) and is immutable, as is the {@code code} business key.
 */
@Entity
@Table(schema = "security", name = "org_unit")
class OrgUnitEntity extends AbstractEntity {

    @Column(name = "parent_id", updatable = false)
    private UUID parentId;

    @Column(name = "code", nullable = false, unique = true, updatable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    protected OrgUnitEntity() {
    }

    OrgUnitEntity(UUID id, UUID parentId, String code, String name) {
        super(id);
        this.parentId = parentId;
        this.code = code;
        this.name = name;
    }

    /** Applies the mutable fields from the domain aggregate (code and parent are immutable). */
    void apply(String name) {
        this.name = name;
    }

    /**
     * Builds a detached entity carrying the expected optimistic-lock {@code version}, so a
     * {@code save()} becomes a version-checked merge (lost-update protection) instead of a re-load.
     */
    static OrgUnitEntity forUpdate(UUID id, UUID parentId, String code, String name, long version) {
        OrgUnitEntity entity = new OrgUnitEntity(id, parentId, code, name);
        entity.markPersisted(version);
        return entity;
    }

    UUID parentId() {
        return parentId;
    }

    String code() {
        return code;
    }

    String name() {
        return name;
    }
}
