package de.volantic.erp.core.entitylink.infrastructure.persistence;

import de.volantic.erp.core.AbstractEntity;
import de.volantic.erp.core.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA representation of a directed entity-link edge. Table {@code core.entity_link}. {@code created_at}
 * is an {@link OffsetDateTime} to match the {@code timestamptz} column under {@code ddl-auto=validate}.
 */
@Entity
@Table(schema = "core", name = "entity_link")
class EntityLinkEntity extends AbstractEntity {

    @Column(name = "from_type", nullable = false, updatable = false)
    private String fromType;

    @Column(name = "from_id", nullable = false, updatable = false)
    private UUID fromId;

    @Column(name = "to_type", nullable = false, updatable = false)
    private String toType;

    @Column(name = "to_id", nullable = false, updatable = false)
    private UUID toId;

    @Column(name = "link_type", nullable = false, updatable = false)
    private String linkType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected EntityLinkEntity() {
    }

    EntityLinkEntity(String fromType, UUID fromId, String toType, UUID toId, String linkType) {
        super(UuidV7.randomUuid());
        this.fromType = fromType;
        this.fromId = fromId;
        this.toType = toType;
        this.toId = toId;
        this.linkType = linkType;
        this.createdAt = OffsetDateTime.now();
    }

    String fromType() {
        return fromType;
    }

    UUID fromId() {
        return fromId;
    }

    String toType() {
        return toType;
    }

    UUID toId() {
        return toId;
    }

    String linkType() {
        return linkType;
    }
}
