/**
 * <strong>Security / RBAC</strong> — rollenbasierte Zugriffssteuerung und Identitäts-Spiegel.
 *
 * <p>Spiegelt Identitäten aus dem externen OIDC-Provider (Authentik, {@code auth.volantic.de}) — es
 * liegen <em>keine Passwörter</em> in der ERP-DB, nur der OIDC-Subject-Bezug. Verwaltet Rollen,
 * Berechtigungen und deren Zuordnung (feingranular, GoBD/NIS2-tauglich). Andere Module fragen
 * Berechtigungen ausschließlich über die hier exponierte API ab.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Security / RBAC")
package de.volantic.erp.security;
