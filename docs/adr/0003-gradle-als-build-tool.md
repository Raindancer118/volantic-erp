# ADR-0003 — Gradle (Kotlin DSL) als Build-Tool

- **Status:** Akzeptiert
- **Datum:** 2026-06-09
- **Entscheider:** Tom (Volantic)
- **Betrifft:** §3.2 Masterplan
- **Hängt zusammen mit:** ADR-0001 (Spring Modulith)

## Kontext

Der Stack steht auf Spring Boot + Spring Modulith + jMolecules (ADR-0001). Ein modularer Monolith
besteht aus **vielen fachlichen Modulen** (core, security, crm, catalog, inventory, accounting,
production, sales, documents, … plus `volantic-erp-sdk`). Dazu kommt eine harte Randbedingung aus §9a:
ein **schneller Dev-Feedback-Loop** ist nötig, um das sub-500-ms-Performance-Budget überhaupt
diszipliniert verfolgen zu können (häufiges Bauen + Profilen).

Zwei Build-Tools standen zur Wahl:

1. **Maven** — Konvention in der Spring-/Enterprise-Welt, deklaratives XML, größte Vertrautheit.
2. **Gradle** — inkrementelle Builds, Build-Cache, flexibleres Multi-Modul-Handling, Kotlin DSL.

## Entscheidung

Wir nutzen **Gradle mit Kotlin DSL** (`build.gradle.kts`).

Begründung:

- **Inkrementelle Builds + Build-Cache** halten Compile-/Test-Zyklen über die vielen Modulith-Module
  kurz — nur geänderte Module (und ihre Abhängigen) werden neu gebaut/getestet. Direkt zahlend auf den
  schnellen Dev-Feedback-Loop (§9a).
- **Multi-Modul-Handling** ist Gradles Stärke: saubere Modul-Abhängigkeitsgraphen, `api`/
  `implementation`-Sichtbarkeiten, die die Modulgrenzen-Disziplin (ArchUnit/Spring Modulith) auf
  Build-Ebene unterstützen.
- **Kotlin DSL** gibt typsichere, IDE-vervollständigte Build-Skripte statt String-getipptem XML —
  weniger Build-Fehler, bessere Wartbarkeit bei wachsender Modulzahl.
- **Version Catalogs** (`libs.versions.toml`) zentralisieren Dependency-Versionen über alle Module —
  wichtig für konsistente Spring-/Bibliotheks-Versionen im Monolith.

## Konsequenzen

**Positiv**
- Schnellere Builds bei vielen Modulen (inkrementell + Cache), perspektivisch Remote-Build-Cache in CI.
- Typsichere Build-Logik, zentralisierte Versionen via Version Catalog.
- Gradle ist von Spring Boot / Spring Modulith voll unterstützt (offizielle Plugins).

**Negativ / Kosten**
- **Steilere Lernkurve** als Maven; Gradle-Build-Logik kann bei Fehlkonfiguration „magisch" wirken.
  Gegenmaßnahme: Build-Konventionen in `buildSrc`/Convention-Plugins kapseln, nicht pro Modul kopieren.
- Etwas geringere Vertrautheit im Enterprise-/Spring-Mainstream als Maven (Talent-Pool) — durch die
  Kotlin-DSL-Lesbarkeit und gekapselte Conventions abgefedert.

**Folgekonventionen (präzisiert bei der Projekt-Einrichtung)**
- **Fachliche Module sind Spring-Modulith-*Packages*, keine Gradle-Subprojekte.** Spring Modulith
  erkennt Module an der Package-Struktur und erzwingt die Grenzen über `ApplicationModules.verify()`
  + ArchUnit (ADR-0001) — das ist das primäre Boundary-Werkzeug, nicht Gradle-Sichtbarkeit. Ein
  Gradle-Subprojekt je Fachmodul wäre für einen Modulith ein Anti-Pattern (verliert die automatische
  Modulerkennung, ohne echten Mehrwert).
- **Eigene Gradle-Subprojekte nur für publizierte Artefakte:** zuerst das `volantic-erp-sdk` (das SPI,
  gegen das Plugin-Entwickler kompilieren) — bewusst erst zu **M3** („SPI wird geerntet", §3.3). Erst
  *dann* lohnen `buildSrc`-Convention-Plugins und ein Multi-Projekt-Layout.
- **Solange ein Gradle-Modul:** Versionen vorerst zentral im `build.gradle.kts`; Umzug nach
  `gradle/libs.versions.toml` (Version Catalog), sobald das zweite Subprojekt (SDK) dazukommt.
- Flyway-Migrationen je Modul (siehe DB-Architektur) liegen modul-lokal unter
  `src/main/resources/db/migration/<modul>/`.

## Verworfene Alternativen

- **Maven:** vertrauter und konventioneller, aber langsamerer (nicht inkrementeller im selben Maße)
  Build und unflexibler bei wachsender Modulzahl. Der Build-Speed- und Multi-Modul-Vorteil von Gradle
  wiegt die geringere Vertrautheit auf, gerade weil der schnelle Feedback-Loop ein explizites Ziel ist.
