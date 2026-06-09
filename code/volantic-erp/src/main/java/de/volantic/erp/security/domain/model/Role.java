package de.volantic.erp.security.domain.model;

import java.util.Set;

/**
 * A role bundles {@link Permission}s; users are assigned roles (optionally scoped). Pure domain
 * aggregate without any persistence concern.
 */
public final class Role {

    private final String key;
    private final Set<Permission> permissions;

    public Role(String key, Set<Permission> permissions) {
        this.key = key;
        this.permissions = Set.copyOf(permissions);
    }

    public boolean grants(String permissionKey) {
        return permissions.stream().anyMatch(p -> p.key().equals(permissionKey));
    }

    public String key() {
        return key;
    }

    public Set<Permission> permissions() {
        return permissions; // already immutable (Set.copyOf)
    }
}
