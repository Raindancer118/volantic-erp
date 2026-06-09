/**
 * <strong>CRM / Master data</strong> — customers, contacts, suppliers, addresses (DB architecture §4,
 * schema {@code crm}). The first business module (M1): a thin vertical slice (customer) is built
 * end-to-end before the remaining entities follow.
 *
 * <p>Hexagonal within the module: a pure {@code domain} owns identity and invariants, {@code application}
 * holds the use cases, {@code infrastructure} adapts JPA/persistence, and {@code api} exposes REST v1.
 * Other modules query master data only through this module's public API or domain events — never its
 * internals.
 */
@org.springframework.modulith.ApplicationModule(displayName = "CRM / Master data")
package de.volantic.erp.crm;
