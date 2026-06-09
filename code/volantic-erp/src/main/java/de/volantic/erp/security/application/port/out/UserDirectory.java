package de.volantic.erp.security.application.port.out;

import de.volantic.erp.security.domain.model.User;

import java.util.Optional;

/**
 * Outbound port: provides the mirrored {@link User} together with its full role/permission graph for
 * an OIDC subject. The implementation (infrastructure) is responsible for loading the graph in
 * <em>one</em> query (no N+1 on the authorization hot path) and mapping it onto the pure domain.
 */
public interface UserDirectory {

    Optional<User> findByOidcSubject(String oidcSubject);
}
