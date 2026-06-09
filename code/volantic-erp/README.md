# Volantic ERP — Anwendung

Modularer Monolith auf **Spring Boot + Spring Modulith + jMolecules** (ADR-0001), Build mit
**Gradle (Kotlin DSL)** (ADR-0003). Architektur-Doku: `../../docs/`.

## Struktur

```
src/main/java/de/volantic/erp/
├── VolanticErpApplication.java   # @Modulithic Einstiegspunkt
├── core/                         # Shared Kernel (OPEN-Modul): Geld/Menge, Belegnummern,
│                                 #   entity_link-Graph, Basistypen
└── security/                     # RBAC + OIDC-Identitätsspiegel (Authentik)
```

**Fachliche Module = Packages**, nicht Gradle-Subprojekte. Spring Modulith erkennt jedes direkte
Sub-Package von `de.volantic.erp` als Anwendungsmodul; die Grenzen werden über
`ModularityTests` (`ApplicationModules.verify()`) als CI-Gate erzwungen. Eigene Gradle-Subprojekte
gibt es erst für publizierte Artefakte — zuerst `volantic-erp-sdk` (das SPI) zu M3.

### Neues Fachmodul anlegen

1. Neues Package `de.volantic.erp.<modul>` mit `package-info.java` und
   `@org.springframework.modulith.ApplicationModule(displayName = "…")`.
2. Interne Implementierung in ein Sub-Package legen, das nicht Teil der exponierten API ist
   (Konvention: `internal`); nur die im Modul-Root liegenden Typen sind für andere Module sichtbar.
3. Cross-Modul-Kommunikation über Domain-Events (Spring Modulith Event Publication Registry) oder die
   exponierte API — niemals direkter Zugriff auf interne Typen.
4. Migrationen unter `src/main/resources/db/migration/<modul>/` ablegen und die Flyway-`locations` in
   `application.yml` ergänzen.

## Bauen & Prüfen

```bash
./gradlew build              # kompiliert + Modulgrenzen-Check + Tests
./gradlew test --tests ModularityTests   # nur den Architektur-Check (ohne DB)
```

Die Moduldokumentation (PlantUML/Canvas) landet nach dem Test unter `build/spring-modulith-docs/`.

## Laufen lassen

Braucht eine PostgreSQL-DB (Single-Tenant). Verbindung über Umgebungsvariablen
`DB_URL` / `DB_USER` / `DB_PASSWORD`, OIDC über `OIDC_ISSUER_URI`. Siehe `application.yml`.
