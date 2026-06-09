/**
 * <strong>Audit</strong> — tamper-evident, hash-chained audit trail (GoBD/NIS2, DB architecture §4,
 * schema {@code audit}). Each entry's hash is computed over the previous entry's hash plus its own
 * content, so altering or removing any past entry breaks the chain and is detectable.
 *
 * <p>Other modules write through the exposed {@link de.volantic.erp.audit.AuditTrail} API; the internal
 * persistence and hashing are hidden behind the module boundary.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Audit Trail")
package de.volantic.erp.audit;
