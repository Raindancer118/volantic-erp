/**
 * <strong>Catalog</strong> — products/articles and bills of materials (DB architecture §6.2, schema
 * {@code catalog}). BOM-capable from day one: a {@code bom_line} component is itself a product, so
 * structures are multi-level. Hexagonal within the module; other modules use the exposed API/events.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Catalog")
package de.volantic.erp.catalog;
