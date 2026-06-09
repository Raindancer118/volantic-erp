# Volantic ERP — Masterplan (v2, nach kritischer Review)

> Status: Überarbeitet nach kritischer Architektur-/Produkt-Review. Grundlage: Produktvision (Tom)
> + Web-Recherche + NotebookLM-Deep-Research (`docs/research/results/`) + Review-Ergebnisse.
> Quellen-Gewichtung beachtet (Studien/Fachartikel hoch, Vendor-Marketing mittel, Foren gering).

---

## 0. Rahmen, Team & Zeitfenster (NEU)

- **Team:** 3 Entwickler + 1 Sales (nicht mehr Solo). Damit verschiebt sich die Machbarkeit von
  „unmöglich" zu **„ambitioniert, aber tragfähig"**: ~**12–18 Monate bis zum vollen MVP**.
- **„Infinite development time" ist Sicherheitsnetz, nicht Planungsprämisse.** Intern planen, als
  müsse der **erste zahlende Kunde in 18 Monaten** stehen (wird es 24, ist das ok). Ohne internen
  Shipping-Druck baut man Kathedralen statt Produkte.
- **Markt-Zeitfenster:** E-Rechnungs-**Versandpflicht** 01/2027 (>800k € Umsatz) und 01/2028 (alle).
  Wer dann eine fertige Lösung hat, gewinnt — das ist die natürliche Deadline für das FiBu/E-Rechnung-Paket.
- **Sales-Agent ist Game-Changer:** entschärft das Vertrauens-/Markteintritts-Defizit eines
  No-Name-ERP deutlich.

## 1. Zielbild

Modernes, modulares ERP für den deutschen/DACH-Mittelstand, das die Etablierten (SAP, ORDAT,
Dynamics) nicht über Feature-Masse schlägt, sondern über **Architektur-Qualität, Bedien-Effizienz
und schmerzfreie Evolution**. Leitsatz: ERP-Projekte scheitern an **Pflege-Komplexität (~80 % TCO
nach Go-Live)** und **vernachlässigter UX**. Positionierung: einsteigerfreundlich, zeitsparend,
„mies effizient", deine Daten gehören dir.

## 2. Strategische Hebel (research-validiert)

1. **Update-Fähigkeit (Clean Core + Modulith)** — Anpassungen berühren nie den Kern.
2. **Objektzentrierte Usability** (360°-Cockpit statt Modul-Hopping) — **plus** tastatur-dichte
   Power-User-Masken (Dual-Mode).
3. **Integriertes In-App-Onboarding / E-Learning** statt Beratertage.
4. **Plug&Play-Integration** über REST + deklarative Adapter.
5. **Vertrauensarchitektur:** ISO 27001, DSGVO, E-Rechnung, GoBD, NIS2 out-of-the-box; German Cloud.

## 3. Architektur

### 3.1 Modularer Monolith (Modulith) + Clean Core
Ein Deployment, eine DB, strikte fachliche Kapselung (DDD Bounded Contexts), Kommunikation über
**in-JVM Domain-Events (ACID)**, Hexagonale Architektur, **ArchUnit als CI-Gate**. Microservices
ausgeschlossen (Glass's Law n³·¹¹, Orchestrations-Overhead auch für 3 Devs nicht tragbar).

### 3.2 ✅ ENTSCHIEDEN: Spring Boot + Spring Modulith + jMolecules
Die Stack-Frage ist **entschieden zugunsten Spring Boot + Spring Modulith + jMolecules** — auch mit
3 Devs. Begründung: Spring Modulith liefert Modulgrenzen-Verifikation, dokumentierte Modul-Strukturen
und die transaktionale in-JVM-Event-Mechanik (§3.1) „ab Werk"; größtes Ökosystem (relevant für
Drittentwickler/Integrationen, Hebel #4). Quarkus' Ressourcenvorteil wiegt das nicht auf, zumal
Single-Tenant-Instanzen via IaC betrieben werden. (ADR-0001)
**Build-Tool: Gradle** (Kotlin DSL) — inkrementelle Builds + Build-Cache halten den Dev-Feedback-Loop
über die vielen Modulith-Module kurz. (ADR-0003) **Java 25 LTS (Temurin)** als Sprach-/Runtime-Ebene
(via Gradle-Toolchain). Konkrete verifizierte Versionen: Spring Boot 3.5.8, Spring Modulith 1.4.1,
jMolecules (ddd 1.9.0 / events 1.10.0).

### 3.3 Erweiterbarkeit: Clean Core + PF4J (Hot-Reload)
Customizing **nur** über stabile APIs + **PF4J-Plugins** (isolierte JARs, `@Extension`), nie am Kern.
Faustregel: Individualentwicklung > 10–20 % = Risikosignal. Extension Points als kuratiertes SPI
(`volantic-erp-sdk`): Plugins steuern Cockpit-Karten, Reports, Workflows, Integrationen bei.
- **Enges SPI (ENTSCHIEDEN):** wenige, sehr stabile Extension Points statt mächtiger
  „Plugin-kann-alles-umbauen"-Engine. Das ist die zentrale Produktentscheidung: nur ein enges SPI hält
  das **40-Min-Drop-In-Update-Versprechen** (§9a) — eine Engine, die Kernverhalten umbiegt, macht uns
  in 5 Jahren zu ORDAT/SAP (nicht-updatebar). Standard-first: der Kunde passt sich in ~80 % an den
  Standard an; echte Sonderfälle über die kuratierten Extension Points.
- **SPI wird geerntet, nicht erfunden (ENTSCHIEDEN):** Extension Points werden **aus der ersten echten
  vertikalen Scheibe extrahiert** (Stammdaten + Kunden-360°), nicht vorab im Vakuum designt. Bei engem
  SPI darf man nur erweitern, nie verengen — ein voreilig erfundenes SPI ist ein 4-Jahre-Klotz. Daher:
  Fundament hart von Tag 1, **SDK spät und erfahrungsbasiert** (Roadmap M3, nach der CRM-Scheibe M1).
- **Hot-Reload ist Pflicht, nicht Komfort:** Kunden laden/aktualisieren Plugins zur Laufzeit, **ohne
  dass Volantic involviert ist und ohne Neustart** (PF4J `PluginManager` + Watch-Directory). Jedes
  Plugin in **eigenem ClassLoader** (sauberes Laden/Entladen, kein Leak); **Circuit-Breaker pro
  Plugin** (X Fehler in Y s → Auto-Deaktivierung + Admin-Notification); **graceful drain** laufender
  Transaktionen vor Reload. Plugin-API-Klassen kommen aus dem Core-ClassLoader (sonst ClassCast).
- **Isolation trotz Source-Available:** Da Lizenznehmer den Kern lesen können, läuft Plugin-Code in
  einer **Sandbox** mit deklarierten Permissions (Manifest) — schützt vor versehentlicher wie
  gezielter Kern-Beschädigung. (Kein Schutz *vor dem Kunden gegen sich selbst* nötig, aber
  Support-Scope sauber abgegrenzt: außerhalb der SPI = Kundensache.)

### 3.4 Persistenz & Mandantenfähigkeit
PostgreSQL als Single Source of Truth. **Single-Tenant pro Instanz** (1 Container + 1 DB je Kunde)
als Default — operativ vertretbar **dank IaC/Automatisierung** (Provisioning, Updates, Backup als
Code) — passt zu DACH-Datenhoheit/On-Prem. **Schema-per-Tenant** als optionaler SaaS-Modus über
Tenant-Hook offengehalten. **Flyway je Modul** (`db/migration/<module>/`).

### 3.5 Versions-Strategie: drei getrennte Oberflächen (NEU präzisiert)

Entscheidend ist die saubere Trennung von **Support-Fenster** (für welche Version fixen wir Bugs)
und **Kompatibilitäts-Fenster** (wie alter Kundencode *funktioniert* noch). Es gibt **drei
unabhängige Oberflächen**, die nie zusammengeworfen werden dürfen:

| Oberfläche | Was | Support | Kompatibilität |
|---|---|---|---|
| **Core/Kernel** | das laufende ERP-Binary | **N-1** (muss aktuell gehalten werden; wer älter ist, updatet erst, sonst kein Support) | — (Kunde ist immer auf neuestem Kernel) |
| **REST-API** | Drittintegrationen (CAD/Shop/DMS) | N-1 | **N-4 via Parallel-Adapter** |
| **SPI** | in-JVM-Plugins (`volantic-erp-sdk`) | N-1 | **N-4 via stabiles, enges SPI** |

**Der Trick / das Verkaufsversprechen:** Der Kunde läuft auf dem **neuesten Kernel** (Support-Pflicht),
nutzt aber gleichzeitig **4 Jahre alte Plugins und Integrationen weiter** — weil Adapter (REST) bzw.
das stabile SPI (Plugins) die alten Verträge bedienen. „Deine Integrationen brechen nie", ohne dass
wir veraltete Kernel supporten müssen.

- **Parallel-Adapter statt Transformer-Chain (ENTSCHIEDEN):** *ein* Hop pro Request, nicht acht — die
  REST-Version wird zur Laufzeit auf *einen* passenden Adapter aufgelöst, die anderen bleiben inaktiv.
  Latenz-optimal (sub-500 ms, §9a). Preis: bis zu **4 Voll-Adapter parallel pflegen** — jede
  Breaking-Change im Kern schlägt in alle relevanten Adapter durch. Bewusst akzeptiert. (ADR-0002)
- **Adapter/SPI-Versionsauflösung automatisch:** die *Drittintegration* bzw. das *Plugin* diktiert die
  Version (nicht der Mandant per Schalter); der passende Adapter wird automatisch gewählt.
- **Additive-Only-Policy:** Verträge nur additiv ändern (neue Felder/Endpunkte/Extension-Points), nie
  brechend, nie verengen. Das SPI darf wachsen, aber eine einmal veröffentlichte „Steckdose" trägst du
  4 Jahre.
- **Consumer-Driven Contract-Tests als CI-Gate:** Release baut nicht, wenn ein Vertrag aus N-4..N bricht.
- **API-Steward:** ein Dev ist benannter Verantwortlicher für REST- *und* SPI-Vertragsdisziplin.
  *Das ist der Unterschied zwischen „N-4 funktioniert" und „N-4 erwürgt euch."*

### 3.6 API-First & deklarative Integration
Standardisierte **REST-APIs** (OpenAPI/Swagger) als Primärschnittstelle. **Deklaratives
Integrations-Framework** (Config-/Low-Code-Adapter für CAD/Shop/DMS) als Differenzierer — Roadmap,
nicht M0.

## 4. Pflicht-Fachbausteine (Review: blinde Flecken — müssen in M0–M2)

Diese wurden im Plan v1 unterschätzt und sind **keine späteren Module, sondern Plattform-Fundament**:

- **Workflow-/Prozess-Engine** (Genehmigungen, Statusübergänge, Belegfluss).
- **RBAC** (rollenbasierte Rechte, feingranular, GoBD/NIS2-tauglich).
- **Belegnummernkreise** (lückenlos, GoBD-konform, je Mandant/Belegart konfigurierbar).
- **Reporting-Engine** (Auswertungen, Dashboards, Export).
- **Druckformulare / Dokumentengenerierung** (Angebote, Rechnungen, Lieferscheine als PDF).
- **Mengen-/Wertfluss** (das buchhalterische Rückgrat: jede Bewegung mengen- UND wertmäßig).
- **Stücklisten-(BOM-)Datenmodell ab Tag 1** — auch wenn PPS erst M6 kommt, muss das Datenmodell
  von Beginn an BOM-fähig sein (sonst teure Nachrüstung).

## 4a. Fachmodul-Reihenfolge & Konfigurations-Konvention (NEU — Tom)

Verbindliche Aufbau-Reihenfolge der Fachmodule (jeweils als Spring-Modulith-Package):

1. **IAM / Security** — Benutzer, Rollen, **extrem feingranulare Rechte** (RBAC + Feld-/Instanz-Ebene,
   z. B. „wer darf das Gehalt von Mitarbeiter X sehen?"). Zuerst, weil alles andere darauf aufbaut.
   Authentifizierung via Authentik-OIDC, Autorisierung im ERP. Mechanik: siehe **ADR-0004**.
2. **Stammdaten / Master Data** — zentral: Kunden (CRM), Lieferanten, Artikel/Produkte.
3. **Lager & Logistik / Inventory** — Bestände, Warenein-/ausgänge, Lagerplätze, Inventur.
4. **Verkauf & Einkauf / Sales & Purchasing** — Angebote, Aufträge, Lieferscheine, Eingangs-/Ausgangsrechnungen.
5. **HR / Human Resources** — Personalakten, Urlaubsverwaltung, Arbeitszeiterfassung. **(NEU im Scope.)**
   Treiber für die Feld-/Instanz-Autorisierung aus #1 (Gehaltsdaten).
6. **Buchhaltung / Accounting** — schwerstes Modul: Debitoren/Kreditoren, Kontenrahmen (SKR03/SKR04),
   DATEV-Export. Zuletzt (vgl. §5: DATEV-Bridge früh, eigene FiBu spät).

**Konfigurations-Konvention:** Settings und Config-Files durchgängig in **YAML** (`application.yml`
statt `.properties`); modul-/umgebungsspezifische Profile via `application-<profil>.yml`.

## 5. FiBu / Finanzbuchhaltung (NEU — Pflicht, nicht optional)

Ein ERP ohne FiBu ist keine ERP. Daher:
- **Ab M2: DATEV-Bridge** (Export/Schnittstelle zu DATEV) als Minimal-Finanzanbindung — pragmatischer
  Markteintritt, da DACH-Steuerberater DATEV erwarten.
- **Ab M7: eigene FiBu** (Konten, Buchungen, Perioden, USt) als Ausbaustufe.
- Eng verzahnt mit Mengen-/Wertfluss (§4), Belegnummernkreisen und E-Rechnung (§7).

## 6. UX-Konzept (research-geschärft)

- **Rollenbasiertes Launchpad** (Fiori-Stil): nur rollenrelevante Kacheln/Felder (Progressive
  Disclosure).
- **Objektzentriertes 360°-Cockpit + Universal-Search:** Kennung (BA-Nr./Kundennr./Artikelnr.) →
  alles modulübergreifend, kontextsensitive Sprünge statt Modul-Hopping (Vorbild Fiori Object Pages).
- **Dual-Mode — Pflicht:** geführtes Cockpit **und** datendichte, tastatur-optimierte
  Power-User-Erfassungsmasken (Shortcuts). Reine Klick-UIs frustrieren Vielerfasser.
- **Just-in-Time-Hilfe** + progressives Onboarding (Time-to-Proficiency als zentrale UX-Kennzahl).
- Erfolg gemessen über **Task-Completion-Time & Fehlerraten**. Skalierbares Design-System; kein
  generischer KI-Look (`avoidaistyles.md`). Client: **React + Vite + TS**, später optional Electron.

## 7. Compliance & Vertrauen (DACH, harter Hebel)

- **E-Rechnung** (XRechnung / ZUGFeRD) — Versandpflicht 01/2027 (>800k €) / 01/2028 (alle): das ist
  die Markt-Deadline.
- **GoBD** (Unveränderbarkeit, lückenlose Belegnummern, Audit-Trail).
- **NIS2** (Härtung, Logging, Zugriffssteuerung). **DSGVO** (Auskunft/Löschung/Portabilität; Export
  auch bei abgelaufener Lizenz). **ISO-27001** deutsche RZ als Option / On-Prem für Datenhoheit.

## 8. Geschäfts-/Lizenzmodell

**Closed Source, kommerzielle Lizenz — Quellcode bleibt zu (ENTSCHIEDEN).** Kein Source-Zugang für
Lizenznehmer: der Burggraben (Cockpit, Adapter-Strategie, SPI) bleibt geschützt, kein Fork-Risiko nach
Jahr 1. Der „kein-Lock-in"-Pitch ruht damit **ausschließlich auf der Datenhoheit-Garantie** (jederzeit
voller Export, s.u.), nicht auf Quelloffenheit. **Produkt-Lizenzschlüssel** (signierter Offline-Key +
optionaler Online-Refresh, ~30 Tage Offline-Toleranz). Monetarisierung: Lizenz + Hosting + Support +
E-Learning + Migration. EULA vor erstem zahlenden Kunden juristisch prüfen.
- **Seat-Lizenz als Soft-Limit, kein Hard-Block:** 253 Mitarbeiter auf 250er-Lizenz → grün mit
  freundlichem Hinweis; deutlich drüber → klare Upgrade-Aufforderung, **aber nie Aussperren**.
  Hard-Block nur bei komplett abgelaufener Lizenz. Seat-Definition (aktive User/30 Tage) im Vertrag
  und System konsistent. Der Mittelstand hasst Software, die bei User 251 zumacht — das ist Feature.
- **Datenhoheit-Garantie (hartes Verkaufsargument):** **Datenexport nie gesperrt**, auch bei
  abgelaufener Lizenz. Zusätzlich **lizenz-unabhängiges CLI-Export-Tool**, das direkt auf die DB des
  Kunden geht und vollständig (Stamm-, Bewegungsdaten, Historien) in Standardformate (CSV/JSON/XML)
  exportiert — funktioniert selbst wenn der App-Container gestoppt ist. „Euer ERP gehört euch."

## 9. Qualitäts-Disziplin

ArchUnit (Modulgrenzen), Contract-Tests N-4..N (CI-Gate), Tests für alle Funktionen
(Spring-Test/Testcontainers-Postgres), Start-Up-Tests, **ADRs**, OpenTelemetry, durchgängiger
Audit-Trail (GoBD/NIS2).

## 9a. Nicht-funktionale Leitplanken (aus Vision-Transkript, hart)

Diese NFRs sind keine späteren Tuning-Ziele, sondern **von Tag 1 in Architektur eingebaut** —
nachträglich sind sie kaum erreichbar:

- **Performance-Budget sub-500 ms:** Zielwert ~**0,5 s, harte Obergrenze 1 s** für jede
  Standard-Interaktion (Cockpit-Aufruf, Suche, Speichern). Konsequenzen by-design: **Optimistic UI**
  (Frontend zeigt Änderung sofort, API-Call im Hintergrund), **Pagination/Virtual-Scrolling**
  (nie mehr laden als sichtbar), **schlanke API-Responses** (nur View-relevante Felder),
  **DB-Indizes ab Schema-Entwurf** (nicht nachträglich), **Redis-Cache** für Stammdaten/Sessions.
  Performance-Budget als **CI-Gate** (Last-/Latenztest bricht Build bei Überschreitung).
- **Test-/Sandbox-System immer inklusive:** jede Kunden-Instanz bekommt **standardmäßig eine
  Testumgebung** (Datenstand-Klon, gefahrlos für Schulung/Plugin-Erprobung/Upgrade-Probe) — kein
  Aufpreis, ein Essential. Stützt Onboarding (§6) und Drop-In-Update-Sicherheit.
- **Drop-In-Updates ≤ 40 min, Rollback < 5 min:** Updates müssen zero-/near-zero-downtime laufen
  (Blue/Green bzw. zwei Container hinter Reverse-Proxy), **rückwärtskompatible Migrationen**
  (Flyway: neue Spalten zuerst, alte erst N-2 droppen), **automatische Smoke-Tests vor Traffic-Swap**,
  garantierter **Rollback unter 5 min**. Die 40-min-Grenze ist Vertragsversprechen, kein Richtwert.

## 10. Roadmap (überarbeitet)

- **M0 — Fundament:** Spring-Modulith-Gerüst, Auth (Authentik-OIDC), Persistence/Flyway,
  **RBAC**, **Belegnummernkreise**, **Workflow-Engine-Kern**, **Mengen-/Wertfluss-Grundmodell**,
  **BOM-fähiges Datenmodell**, Cockpit-/Search-Framework, API-Gateway-Grundgerüst, ArchUnit, ADRs.
- **M1 — Stammdaten/CRM (API v1) + Kunden-360°** + Datenexport + Contract-Test.
- **M2 — UX + Reporting + Druckformulare + DATEV-Bridge:** Design-Flow → Prototyp (Launchpad,
  Universal-Search, Cockpit, dichte Erfassungsmaske) → React-App; Reporting-/Druck-Engine; erste
  Finanzanbindung via DATEV.
- **M3 — Plugin-SDK (PF4J), enges SPI aus M1-Scheibe geerntet** + Beispiel-Extension. SPI wird
  *nach* der echten CRM-Scheibe extrahiert, nicht vorab erfunden (§3.3).
- **M4 — N-4-Kompat-Beweis:** API v2 + Transformer-Chain + Contract-Gate stabil.
- **M5 — Compliance-Paket:** E-Rechnung (XRechnung/ZUGFeRD), GoBD-Audit-Trail, DSGVO-Export.
- **M6 — Produktion/PPS:** **BA-Nummer-Cockpit** (Produktionsaufträge, Schritte, Stücklisten,
  Dokumente). PPS ist NICHT „nice to have".
- **M7 — Eigene FiBu + Upgrade-Wizard + Release-Pipeline** (Self-Service-Update).
- Danach: Verkauf, Lager-Ausbau, deklaratives Integrations-Framework, E-Learning, KI-Add-ons.

## 11. Governance (für spätere Kundenprojekte)
BPR statt „Lift-and-Shift", Datenbereinigung als priorisiertes Teilprojekt, Key-User/ERP-Champion,
Process-First-Demos mit Echtdaten, interdisziplinäre Entscheidungen.

## 11a. Datenmigration als Produkt (NEU — Go-to-Market-Kern)

Migration ist **Scheiternsfaktor #1** (eigene NotebookLM-Research) — daher kein „wir importieren das
schon", sondern **verifizierbare Migration mit Sicherheitsnetz**:
- **Definiertes Teilprojekt** mit Scope/Phasen/Abnahme — kein Häkchen. Für Kunde Null kostenlos, aber
  als **R&D zum Aufbau des wiederverwendbaren ETL-Toolkits** behandelt (Staging-Area, Mapping-Config).
- **Iterativ statt Big-Bang:** erst Stammdaten, dann offene Vorgänge, dann Historie — jede Stufe einzeln
  prüfbar.
- **Reconciliation-Reports** sind der Vertrauensbauer (Soll/Ist-Abgleich Quell- vs. Zielsystem) — der
  IT-Leiter glaubt Verifizierbarkeit, nicht Versprechen.
- **Parallelbetrieb** (Alt + Neu nebeneinander) statt Stichtags-Cutover — niemand wettet die Firma.
- Verkaufslogik: Migration + Gratis-Schulung überzeugen den **IT-Leiter**, sub-500-ms-Speed den
  **Geschäftsführer** — beide nötig.

## 12. Hauptrisiken (Stand v2)
1. **Shipping-Disziplin trotz „infinite time"** — größtes neues Risiko: Perfektionismus ohne
   Deadline. Gegenmaßnahme: internes 18-Monats-Ziel, jeder Meilenstein „fertig", keine Dauerpolitur.
2. **Fachliche Tiefe FiBu/E-Rechnung/GoBD** — fehlerintolerant, früh angehen.
3. **Mengen-/Wertfluss & Belegnummernkreise** korrekt von Anfang an (nachträglich kaum reparierbar).
4. **N-4-Governance** — ohne API-Steward + Additive-Only droht Adapter-Erwürgung.
5. **Dual-Mode-UX-Aufwand**.
6. **Performance-Budget (sub-500 ms) hält nicht unter Last/am Monatsende** — Gegenmaßnahme:
   Last-/Latenztest als CI-Gate ab M0, Optimistic UI + Indizes + Cache by-design, nicht nachgerüstet.
7. **Hot-Reload-Plugins reißen den Kern** (ClassLoader-Leak, hängende Transaktion) — Gegenmaßnahme:
   ClassLoader-Isolation + Circuit-Breaker + graceful drain als Plattform-Fundament (§3.3), nicht
   pro Plugin.
