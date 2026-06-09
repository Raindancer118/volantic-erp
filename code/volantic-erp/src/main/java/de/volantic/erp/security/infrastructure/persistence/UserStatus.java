package de.volantic.erp.security.infrastructure.persistence;

/** Persistierter Status eines gespiegelten Nutzers. Authentifizierung liegt bei Authentik (OIDC). */
enum UserStatus {
    ACTIVE,
    DISABLED
}
