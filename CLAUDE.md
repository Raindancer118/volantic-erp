# Claude Code — Volantic ERP

> Spring Boot 3.5 · Spring Modulith · Gradle (Kotlin DSL) · Java 25 · PostgreSQL

## Stack & Build

```bash
./gradlew build          # vollständiger Build
./gradlew test           # alle Tests
./gradlew bootRun        # lokaler Dev-Server
./gradlew check          # Build + Tests + ArchUnit
```

## Modul-Struktur (Spring Modulith)

| Package | Fachmodul | Reihenfolge |
|---------|-----------|-------------|
| `iam` | Auth, RBAC, Rollen, Rechte (Authentik-OIDC) | M0 – zuerst |
| `stammdaten` | Kunden (CRM), Lieferanten, Artikel | M1 |
| `lager` | Bestände, Warenbewegungen, Lagerplätze | M3 |
| `verkauf` | Angebote, Aufträge, Rechnungen | M4 |
| `hr` | Personalakten, Urlaub, Arbeitszeit | M5 |
| `buchhaltung` | Debitoren, Kreditoren, DATEV-Export | M6 |

Querschnitt: `workflow`, `belege`, `reporting`, `audit`, `sdk` (PF4J-Plugins)

## Architektur-Regeln (ArchUnit — CI-Gate)

- Bounded Contexts kommunizieren **nur** über in-JVM Domain-Events (ACID), nie direkt
- Hexagonale Architektur: Domain kennt keine Infrastruktur
- Keine JPA-Entities in API-Responses (DTOs/Records verwenden)
- BOM-fähiges Datenmodell von Tag 1

## Datenbank

- PostgreSQL · **Flyway je Modul** (`db/migration/<modul>/`)
- Schema-Änderungen nur additiv (neue Spalten zuerst, alte erst N-2 droppen)
- Single-Tenant-Instanz als Default; Schema-per-Tenant als SaaS-Option

## Testing-Strategie

- **Testcontainers** für Integrationstests (echte Postgres-Instanz, kein H2-Mock)
- **ArchUnit** als CI-Gate für Modulgrenzen
- **Consumer-Driven Contract-Tests** für REST API + SPI (N-4..N)
- Ziel: 80 %+ Coverage auf Business-Logic
- Tools: JUnit 5, AssertJ, Mockito, Testcontainers

## API & Versionierung

- OpenAPI/Swagger (API-First)
- Versionierung: `/v1/...`, `/v2/...`
- **Additive-Only-Policy** — keine Breaking Changes
- Consumer-Driven Contract-Tests brechen den Build bei Vertragsverletzung

## Compliance (DACH — non-negotiable)

- **GoBD**: lückenlose Belegnummernkreise, unveränderlicher Audit-Trail
- **E-Rechnung**: XRechnung / ZUGFeRD (Pflicht ab 01/2027)
- **DSGVO**: Auskunft/Löschung/Export auch bei abgelaufener Lizenz
- **NIS2**: Zugriffsprotokolle, Härtung
- Kein direkter DB-Zugriff an laufenden Instanzen (API verwenden)

## Skills

Alle Skills liegen unter `.claude/skills/`. Einmal pro Session laden, dann per natürlicher Sprache aufrufen.

| Skill | Wann laden |
|-------|------------|
| `git-commit` | Vor jedem Commit |
| `test-quality` | Tests schreiben oder reviewen |
| `java-code-review` | Code-Review |
| `spring-boot-patterns` | Spring-Patterns prüfen/implementieren |
| `jpa-patterns` | JPA/Hibernate-Code |
| `api-contract-review` | REST-Endpoints ändern |
| `architecture-review` | Architektur-Entscheidungen |
| `security-audit` | Security-relevante Änderungen |
| `gradle-dependency-audit` | Dependency-Updates / Release-Vorbereitung |
| `performance-smell-detection` | Performance-kritische Pfade |
| `concurrency-review` | Multi-Threading / reaktiver Code |
| `logging-patterns` | Logging-Konfiguration / -Qualität |
| `clean-code` | Code-Qualität allgemein |
| `solid-principles` | Design-Qualität |
| `design-patterns` | Muster erkennen / einsetzen |
| `changelog-generator` | Release-Notes generieren |
| `issue-triage` | GitHub Issues priorisieren |
| `java-migration` | Java / Dependency Upgrades |

## Commit-Format (Conventional Commits)

```
<type>(<scope>): <subject>

<body>  — WARUM, nicht WAS

<footer> — Fixes #<issue>
```

Scopes: `iam`, `stammdaten`, `lager`, `verkauf`, `hr`, `buchhaltung`, `core`, `api`, `sdk`, `workflow`, `rbac`, `belege`, `db`, `build`, `ci`

## Nicht-funktionale Leitplanken (NFR — von Tag 1)

- **Sub-500 ms** für jede Standard-Interaktion (CI-Gate: Last-/Latenztest)
- **Drop-In-Update ≤ 40 min**, Rollback < 5 min (Blue/Green)
- **Optimistic UI** im Frontend (React + Vite + TS)
- **Redis-Cache** für Stammdaten / Sessions

## Ressourcen

- Masterplan: `docs/00-Masterplan.md`
- Datenbank-Architektur: `docs/02-Datenbank-Architektur.md`
- ADRs: `docs/adr/`
- Research: `docs/research/`
