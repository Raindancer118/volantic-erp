package de.volantic.erp.security;

import java.util.Set;

/**
 * Zentraler Autorisierungs-Port (ADR-0004). <strong>Jede</strong> Zugriffsentscheidung im System
 * läuft über diese Schnittstelle — an der Domänen-/Service-Grenze, nicht nur im UI.
 *
 * <p>Heute dahinter: RBAC (Rolle → Permission) plus Feld- und Instanz-/Scope-Ebene. Wächst die
 * Policy-Komplexität (HR, Accounting), wird eine Policy-Engine hinter denselben Port gezogen, ohne
 * dass ein Aufrufer sich ändert. Die Schnittstelle darf additiv wachsen, aber nie verengt werden.
 *
 * <p>Berechtigungen sind {@code resource:action}-Strings, z. B. {@code "hr.salary:read"} oder
 * {@code "sales.order:approve"}. Sensible Felder bekommen eine eigene Permission.
 */
public interface AuthorizationService {

    /** Hält der Nutzer die Berechtigung global (uneingeschränkt)? */
    boolean isPermitted(String oidcSubject, String permission);

    /** Hält der Nutzer die Berechtigung im angefragten {@link AccessScope}? */
    boolean isPermitted(String oidcSubject, String permission, AccessScope scope);

    /** Alle Berechtigungs-Schlüssel des Nutzers (über alle Rollen, unabhängig vom Scope). */
    Set<String> permissionsOf(String oidcSubject);
}
