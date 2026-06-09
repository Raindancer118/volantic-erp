/**
 * <strong>Core / Shared Kernel</strong> — modulübergreifend genutzte Fundament-Bausteine.
 *
 * <p>Hier leben die plattformweiten Konzepte, die <em>alle</em> Fachmodule brauchen, ohne dass
 * dadurch eine fachliche Kopplung entsteht: Wertobjekte (Geld, Menge), Belegnummernkreise
 * (lückenlos, GoBD), der {@code entity_link}-Graph für das objektzentrierte 360°-Cockpit sowie
 * gemeinsame Basistypen (UUIDv7-IDs, Auditfelder).
 *
 * <p>Als {@link org.springframework.modulith.ApplicationModule.Type#OPEN OPEN}-Modul deklariert:
 * andere Module dürfen auf die hier exponierten Typen zugreifen, ohne dass Spring Modulith das als
 * Grenzverletzung wertet. Fachmodule (crm, catalog, …) dürfen das <em>nicht</em> untereinander.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        displayName = "Core / Shared Kernel")
package de.volantic.erp.core;
