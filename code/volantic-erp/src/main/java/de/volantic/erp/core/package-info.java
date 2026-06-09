/**
 * <strong>Core / Shared Kernel</strong> — foundation building blocks used across modules.
 *
 * <p>This is where the platform-wide concepts live that <em>every</em> business module needs without
 * introducing business coupling: value objects (money, quantity), document number ranges (gap-free,
 * GoBD), the {@code entity_link} graph for the object-centric 360° cockpit, and shared base types
 * (UUIDv7 ids, audit fields).
 *
 * <p>Declared as an {@link org.springframework.modulith.ApplicationModule.Type#OPEN OPEN} module:
 * other modules may access the types exposed here without Spring Modulith treating it as a boundary
 * violation. Business modules (crm, catalog, …) must <em>not</em> do this among each other.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        displayName = "Core / Shared Kernel")
package de.volantic.erp.core;
