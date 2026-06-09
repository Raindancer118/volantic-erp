package de.volantic.erp.security;

import java.util.UUID;

/**
 * Daten-Scope einer Autorisierungsanfrage bzw. -vergabe (Instanz-/Bereichs-Ebene).
 *
 * <p>{@link #GLOBAL} steht für „uneingeschränkt". Ein konkreter Scope bindet eine Berechtigung an
 * eine Instanz/einen Bereich (z. B. {@code AccessScope.of("DEPT", abteilungsId)}). Eine global
 * vergebene Berechtigung deckt jeden angefragten Scope ab; eine scoped vergebene Berechtigung deckt
 * nur exakt ihren eigenen Scope ab — und insbesondere keine globale Anfrage.
 */
public record AccessScope(String type, UUID id) {

    public static final AccessScope GLOBAL = new AccessScope("GLOBAL", null);

    public static AccessScope of(String type, UUID id) {
        return new AccessScope(type, id);
    }

    public boolean isGlobal() {
        return id == null;
    }
}
