package de.volantic.erp.security.domain.model;

import java.util.Set;

/**
 * Eine Rolle bündelt {@link Permission}s; Nutzer erhalten Rollen (ggf. scoped) zugewiesen.
 * Reines Domänen-Aggregat ohne Persistenz-Belang.
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
        return permissions; // bereits unveränderlich (Set.copyOf)
    }
}
