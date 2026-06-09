package de.volantic.erp.security.domain.model;

/**
 * Status of a mirrored user. Authentication itself stays with Authentik (OIDC); this status controls
 * whether the user may exercise any permissions in the ERP at all.
 */
public enum UserStatus {
    ACTIVE,
    DISABLED
}
