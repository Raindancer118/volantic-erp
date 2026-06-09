package de.volantic.erp.security.domain.model;

/**
 * Status eines gespiegelten Nutzers. Authentifizierung selbst liegt bei Authentik (OIDC); dieser
 * Status steuert, ob der Nutzer im ERP überhaupt Berechtigungen ausüben darf.
 */
public enum UserStatus {
    ACTIVE,
    DISABLED
}
