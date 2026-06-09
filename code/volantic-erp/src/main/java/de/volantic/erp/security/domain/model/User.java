package de.volantic.erp.security.domain.model;

import de.volantic.erp.security.AccessScope;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Im ERP gespiegelter Nutzer als Domänen-Aggregat. Trägt <strong>kein Passwort</strong> — nur den
 * OIDC-Subject-Bezug zu Authentik; die Authentifizierung bleibt vollständig beim IdP. Die
 * Autorisierungs-Entscheidung ist hier domänenrein gekapselt (kein Spring, keine DB).
 */
public final class User {

    private final String oidcSubject;
    private final List<RoleAssignment> roleAssignments;

    public User(String oidcSubject, List<RoleAssignment> roleAssignments) {
        this.oidcSubject = oidcSubject;
        this.roleAssignments = List.copyOf(roleAssignments);
    }

    /** Hält der Nutzer die Berechtigung im angefragten {@link AccessScope}? */
    public boolean isPermitted(String permission, AccessScope scope) {
        return roleAssignments.stream().anyMatch(a -> a.grants(permission, scope));
    }

    /** Alle Berechtigungs-Schlüssel über alle Rollen, unabhängig vom Scope. */
    public Set<String> permissionKeys() {
        return roleAssignments.stream()
                .flatMap(a -> a.role().permissions().stream())
                .map(Permission::key)
                .collect(Collectors.toSet());
    }

    public String oidcSubject() {
        return oidcSubject;
    }
}
