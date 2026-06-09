/**
 * <strong>Security / RBAC</strong> — role-based access control and identity mirror.
 *
 * <p>Mirrors identities from the external OIDC provider (Authentik, bundled per installation) — there
 * are <em>no passwords</em> in the ERP database, only the OIDC subject reference. Manages roles,
 * permissions and their assignment (fine-grained, GoBD/NIS2-ready). Other modules query permissions
 * exclusively through the API exposed here.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Security / RBAC")
package de.volantic.erp.security;
