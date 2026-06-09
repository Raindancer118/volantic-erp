package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.core.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/** JPA representation of a role with its permissions. Table {@code security.role}. */
@Entity
@Table(schema = "security", name = "role")
class RoleEntity extends AbstractEntity {

    @Column(name = "role_key", nullable = false, unique = true)
    private String key;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "security", name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<PermissionEntity> permissions = new HashSet<>();

    protected RoleEntity() {
    }

    RoleEntity(String key, String name) {
        this.key = key;
        this.name = name;
    }

    void addPermission(PermissionEntity permission) {
        permissions.add(permission);
    }

    String key() {
        return key;
    }

    Set<PermissionEntity> permissions() {
        return permissions;
    }
}
