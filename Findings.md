# In-Depth Analyse: Volantic ERP

**Datum:** 2026-06-09
**Status-Review:** Strenges Audit des Projekts hinsichtlich Datenbank, Architekturrichtlinien, Changelog und NFRs (Non-Functional Requirements).

## 1. Executive Summary: Diskrepanz zwischen Behauptung und Realität
Das Projekt weist eine **massive Diskrepanz** zwischen dem deklarierten Status und der tatsächlichen Codebasis auf. Sowohl in der `Project.md` als auch implizit im `CHANGELOG.md` wird der Meilenstein "M0" als **fertig** deklariert ("Status: M0 fertig. M1 crm fertig").
Ein Abgleich mit dem `00-Masterplan.md` zeigt jedoch, dass fundamentale Bestandteile von M0 und M1 **vollständig fehlen**. Wenn das Projekt mit diesem "Fake-Status" weitergeführt wird, wird es seine eigenen Leitlinien (GoBD-Konformität, Drop-In Updates, Sub-500 ms) niemals erreichen können. Das Projekt droht hierdurch an typischen "Nachträglicher Einbau"-Kosten zu scheitern.

## 2. Der "Fake-Status" (Fehlende Fundamente in M0)
Laut Masterplan (M0) müssen zwingend folgende Bausteine umgesetzt sein. Sie fehlen jedoch komplett in der Codebasis:
- **Workflow-Engine-Kern:** Im Build (`build.gradle.kts`) ist weder Flowable noch Camunda eingebunden, es gibt kein `workflow`-Package.
- **Belegnummernkreise:** Essenziell für GoBD-Konformität, doch gänzlich abwesend.
- **Mengen-/Wertfluss-Grundmodell:** Die Basis für das spätere Accounting fehlt.
- **BOM-fähiges Datenmodell:** Das `catalog`-Modul (Product, BOM) sollte ab Tag 1 existieren, ist aber gar nicht implementiert.
- **Spring Modulith Event Registry:** Der Masterplan spezifiziert ACID-taugliche Domain-Events über die Modulith Event Publication Registry. Es fehlt jedoch die entscheidende Dependency (`spring-modulith-events-jpa`) sowie das zugehörige Datenbankschema. Events werden derzeit bei Fehlern unwiderruflich im RAM verloren.

## 3. Datenbank-Architektur & Compliance (GoBD/DSGVO)
Die Umsetzung der Datenbank verletzt die eigenen Architekturrichtlinien (`02-Datenbank-Architektur.md`) eklatant:
- **Fehlendes Auditing:** Die Architektur fordert zwingend die Standardspalten `created_at, created_by, modified_at, modified_by` in *jeder* Tabelle. Weder in den Flyway-Migrationen (z.B. `V101__crm_schema.sql` oder `V001__security_schema.sql`) noch in der `AbstractEntity.java` (kein `@EntityListeners(AuditingEntityListener.class)`) sind diese Spalten implementiert.
- **Konsequenz:** Die gesetzlichen Auflagen zur Nachvollziehbarkeit (GoBD) sind damit "by design" gebrochen. Ein nachträglicher Einbau von Auditing in hunderten Tabellen bricht das Versprechen eines "Clean Core" und verdoppelt den Aufwand.

## 4. Performance & NFRs (Das "Sub-500 ms" Versprechen)
**Positiv:**
- Der vorherige Performance-Smell bezüglich UUIDv7-Generierung in `AbstractEntity` wurde laut Changelog und Code sauber behoben.
- Das Cache-Stampede/DoS-Risiko in `UserGraphCache` ist gefixt (Null-Values werden nun gecacht). Die Cache-Invalidierung ist im `SecurityAdminService` umgesetzt.

**Kritisch:**
- **Fehlende Pagination:** REST-Endpunkte wie `GET /v1/crm/customers` (siehe `CustomerController.java`) rufen pauschal `.toList()` auf und laden sämtliche Datensätze in den Speicher. Das NFR "Sub-500 ms" ist so ab wenigen tausend Einträgen unerreichbar. Virtual Scrolling und Pagination müssen *by design* und nicht nachträglich in das System.

## 5. Architektur-Schnitt (Hexagonal & Modulith)
Hier liefert das Projekt das, was es verspricht:
- Die Modulgrenzen via Spring Modulith und ArchUnit (`ArchitectureTest`) greifen und werden konsequent hexagonal eingehalten (Trennung von Domain, Application und Infrastructure).
- Das Kunden-360°-Konzept mit der `core.entity_link` Kanten-Tabelle ist zukunftsfähig konzipiert.

## 6. Sinnhaftigkeit und Fazit
Ergibt das Projekt so Sinn? **Jein.**
Die Vision (N-4-Kompatibilität, Drop-In Updates, Hexagonaler Core) ist hervorragend und absolut markttauglich.
Allerdings belügt sich das Team derzeit selbst: Es wird Feature-Progress (CRM, REST) auf einem Fundament simuliert, bei dem die wichtigsten Querschnittsfunktionen (GoBD-konformes Auditing, Flowable Workflow, Persistente Domain-Events, Paginierung) weggelassen wurden. 
**Handlungsempfehlung:** M1 muss sofort gestoppt werden. Der Status von M0 muss auf "In Arbeit" zurückgesetzt werden, bis die fehlenden technischen Fundamente (insbesondere JPA Auditing und Event-Registry-Persistenz) und die Architekturvorgaben aus der Dokumentation vollständig erfüllt sind.
