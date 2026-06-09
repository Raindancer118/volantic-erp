package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.core.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** JPA-Abbild einer Berechtigung. Tabelle {@code security.permission}. */
@Entity
@Table(schema = "security", name = "permission")
class PermissionEntity extends AbstractEntity {

    @Column(name = "permission_key", nullable = false, unique = true)
    private String key;

    @Column(name = "description")
    private String description;

    protected PermissionEntity() {
    }

    PermissionEntity(String key, String description) {
        this.key = key;
        this.description = description;
    }

    String key() {
        return key;
    }
}
