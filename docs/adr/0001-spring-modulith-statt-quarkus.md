# ADR-0001 — Spring Boot + Spring Modulith + jMolecules als Plattform-Stack

- **Status:** Akzeptiert
- **Datum:** 2026-06-09
- **Entscheider:** Tom (Volantic)
- **Betrifft:** §3.1, §3.2 Masterplan
- **Ersetzt:** frühere Annahme „Quarkus (Java 21)"

## Kontext

Volantic ERP wird ein **modularer Monolith** (ein Deployment, eine DB pro Mandant, strikte fachliche
Kapselung als DDD Bounded Contexts, Kommunikation über transaktionale in-JVM-Domain-Events). Diese
Architektur ist gesetzt: Microservices sind ausgeschlossen (Glass's Law — Komplexität ≈ n³·¹¹,
Orchestrierungs-Overhead für ein 3-Personen-Team nicht tragbar, verteilte Transaktionen unvereinbar
mit GoBD-Konsistenzanforderungen).

Offen war nur die Frage des konkreten JVM-Frameworks. Kandidaten:

1. **Quarkus** (ursprüngliche Annahme) — schneller Startup, geringer Memory-Footprint via GraalVM
   Native Image; attraktiv bei vielen Single-Tenant-Instanzen.
2. **Spring Boot + Spring Modulith + jMolecules** — größtes JVM-Ökosystem; Spring Modulith ist explizit
   für genau diese „modularer Monolith"-Architektur gebaut.

Anforderungen, die die Wahl treiben:
- **Modulgrenzen müssen technisch erzwingbar sein** (CI-Gate), sonst erodiert die Kapselung — der
  Kern unseres Update-Versprechens.
- **Transaktionale Domain-Events** (ACID, kein verlorenes Event bei Crash) als Standard-Mechanik.
- **Großes Ökosystem** für Drittentwickler/Integrationen (strategischer Hebel #4: offene APIs,
  Plug-&-Play-Integration).
- Tragbar für ein **kleines Team** (geringe Eigenbau-Last).

## Entscheidung

Wir bauen auf **Spring Boot + Spring Modulith + jMolecules**.

Begründung im Einzelnen:

- **Spring Modulith ist purpose-built für diese Architektur.** Es liefert ab Werk:
  - Verifikation der Modulgrenzen (`ApplicationModules.verify()`) — als Test/CI-Gate, ergänzt durch
    ArchUnit für feinere Regeln.
  - Dokumentation der Modulstruktur (generierte Modul-Canvas / PlantUML).
  - **Event Publication Registry** = transaktionaler Outbox-Mechanismus für die in-JVM-Domain-Events
    aus §3.1, ohne dass wir ihn selbst bauen. Genau die ACID-Garantie, die wir brauchen.
- **jMolecules** macht DDD-Konzepte (Aggregate, Entity, Value Object, Domain Event) als Annotationen
  explizit und maschinenprüfbar — koppelt sauber an Spring Modulith und ArchUnit.
- **Größtes Ökosystem** der JVM-Welt: Bibliotheken, Tooling, Einstellbarkeit von Entwicklern,
  Drittanbieter-Integrationen. Direkt zahlend auf Hebel #4.
- Quarkus' **Ressourcenvorteil wiegt das nicht auf:** Single-Tenant-Instanzen werden via IaC
  betrieben (Provisioning/Updates/Backup als Code); RAM/Startup sind dort kein Engpass, der den
  Verlust des Modulith-/Event-/Ökosystem-Vorsprungs rechtfertigt. Native-Image-Startupzeit ist für
  langlaufende ERP-Server irrelevant.

## Konsequenzen

**Positiv**
- `ApplicationModules.verify()` + ArchUnit als CI-Gate: Modulgrenzen-Verletzung bricht den Build.
- Event Publication Registry deckt die transaktionale Event-Zustellung ab — kein Eigenbau-Outbox.
- Breiter Talent-Pool und Bibliotheks-Ökosystem senken die Eigenbau-Last für ein kleines Team.

**Negativ / Kosten**
- Höherer Memory-Footprint pro Instanz als Quarkus-Native — bei Single-Tenant × vielen Kunden ein
  Betriebskostenfaktor. Gegenmaßnahme: JVM-Tuning, ggf. später Spring-AOT/GraalVM-Native für Spring
  Boot evaluieren (Tür bleibt offen, ist aber nicht Tag-1).
- Spring-Magie (Auto-Configuration, Proxies) kann Performance-Analyse im sub-500-ms-Budget (§9a)
  erschweren — verlangt Disziplin bei Profiling und bewusstes Vermeiden teurer Reflection-Pfade auf
  Hot-Paths.

**Folgeentscheidungen**
- Plugin-Mechanik: PF4J (separates ADR-Thema, siehe §3.3) — bewusst *nicht* Spring-eigene Mechanik,
  wegen ClassLoader-Isolation und Hot-Reload.
- Persistenz: Flyway je Modul, PostgreSQL (siehe `docs/02-Datenbank-Architektur.md`).

## Verworfene Alternativen

- **Quarkus:** schlanker im Betrieb, aber ohne Modulith-Äquivalent (Modulgrenzen-Verifikation,
  Event-Registry) — das müssten wir selbst bauen, plus kleineres Ökosystem. Der Betriebsvorteil ist
  durch IaC entschärft und wiegt den Architektur-Vorsprung nicht auf.
- **Klassisches Spring Boot ohne Modulith:** keine erzwungenen Modulgrenzen → Kapselung erodiert →
  Update-Versprechen (Clean Core) nicht haltbar.
- **Jakarta EE / MicroProfile (anderer Server):** kleineres Momentum, mehr Eigenbau für die
  Modulith-Disziplin.
