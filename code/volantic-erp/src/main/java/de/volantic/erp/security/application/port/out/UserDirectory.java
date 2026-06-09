package de.volantic.erp.security.application.port.out;

import de.volantic.erp.security.domain.model.User;

import java.util.Optional;

/**
 * Outbound-Port: liefert den gespiegelten {@link User} samt seinem vollständigen Rollen-/Rechte-Graph
 * zu einem OIDC-Subject. Die Implementierung (infrastructure) ist dafür verantwortlich, den Graph in
 * <em>einer</em> Abfrage zu laden (kein N+1 auf dem Autorisierungs-Hotpath) und auf die reine Domäne
 * zu mappen.
 */
public interface UserDirectory {

    Optional<User> findByOidcSubject(String oidcSubject);
}
