# Volantic ERP — Datenbank-Architektur (Start-Blueprint)

> Entworfen nach `datastorage`-Best-Practices (composable persistence, keine BLOBs in der DB,
> OLTP/OLAP physisch trennen, GoBD/DSGVO by design). Ziel: das **optimale Fundament zum Start**,
> ohne Tag-1-Overengineering. „Mehrere Datenbanken" ja — aber jede mit klarem Zweck.

---

## 1. Leitprinzipien

1. **Ein System pro Workload (Polyglot Persistence).** PostgreSQL ist die transaktionale Single
   Source of Truth — aber nicht der Mülleimer für alles. BLOBs, Suche, Cache, Analytik bekommen das
   jeweils richtige System.
2. **OLTP und OLAP physisch trennen.** HTAP gilt für Enterprise als gescheitert. Reporting läuft
   nicht auf der Buchungs-DB, sondern (später) auf einer Read-Projektion via CDC.
3. **BLOBs gehören in Objektspeicher**, nie in die DB (Skill-Regel: > 1 MB zwingend S3/MinIO).
4. **Modulgrenzen auch in der DB.** Jedes Spring-Modulith-Modul besitzt sein **PostgreSQL-Schema**
   und seine Flyway-Migrationen. Keine harten Fremdschlüssel über Modulgrenzen (lose Kopplung).
5. **Unveränderbarkeit, wo das Gesetz es verlangt** (GoBD): Buchungen append-only, Storno statt
   Update/Delete; manipulationssicherer Audit-Trail.

---

## 2. Polyglot-Persistenz: Welche Datenbanken, warum, wann

### Ab Tag 1 (M0–M2)

| Store | Zweck | Begründung |
|---|---|---|
| **PostgreSQL 16+** | Transaktionaler Kern, SoT (alle ERP-Daten) | Strukturierte Daten → relational, ACID, B-Tree, Normalisierung |
| **MinIO / S3** (vorhanden auf dorn) | Dokumente/Belege/Anhänge/CAD (PDF, Bilder, > 1 MB) | BLOBs raus aus der DB; Metadaten in PG, Bytes in S3 |
| **Redis** (optional ab M1) | Cache (cache-aside), Sessions, verteilte Locks, Rate-Limiting | Nur wo nötig; nicht als zweite Wahrheit |

### Hineinwachsen (klare Auslöser, NICHT Tag 1)

| Store | Auslöser | Zweck |
|---|---|---|
| **OpenSearch / Elasticsearch** | Universal-Search wird mit PG-FTS zu langsam/unscharf | Cross-Modul-Volltext, Relevanz-Ranking, Fuzzy |
| **ClickHouse** (o. DuckDB/columnar) | Reporting-Dashboards brauchen < 1 s auf Mio. Zeilen | OLAP, gespeist per **Debezium-CDC** aus PG-WAL |

**Start-Suche bewusst zuerst in PostgreSQL** (`tsvector` + `pg_trgm`) — spart Tag-1-Infra; die
Such-Schnittstelle wird so abstrahiert, dass ein Wechsel auf OpenSearch später nur ein Adapter ist.

### Mandanten (Tenancy)

- **Default: 1 PostgreSQL-Datenbank pro Kunde** (Single-Tenant-Instanz). Maximale Isolation,
  Datenhoheit/On-Prem-tauglich, schmerzfreie Updates. **Kein `tenant_id` in den Tabellen nötig** →
  schlankeres Schema.
- **Optionaler SaaS-Modus: Schema-per-Tenant** in einer DB (PG-Schema-Switch via JDBC ist effizient).
  Architektur über einen Tenant-Resolver offengehalten, aber nicht vorzeitig festgenagelt.

---

## 3. PostgreSQL — Konventionen (verbindlich)

- **Primärschlüssel: `UUIDv7`** (zeit-sortiert). Global eindeutig (gut für Events, Import/Merge,
  Plugin-Daten, verteilte Erzeugung) **und** indexlokal (kein Random-UUID-Index-Bloat). Erzeugung
  in der Java-App (UUIDv7-Lib) oder via `pg_uuidv7`/PG18-`uuidv7()`.
- **Fachliche Belegnummern** sind **separat** von der UUID (§5.1) — menschenlesbar, GoBD-lückenlos.
- **Zeit: immer `TIMESTAMPTZ`** (UTC), nie `timestamp` ohne Zone.
- **Geld: `NUMERIC(19,4)` + `currency_code` (ISO-4217)** — nie Float. Rundung kaufmännisch,
  Policy zentral. **Menge: `NUMERIC(19,6)` + `uom` (Einheit)**. (Das ist der Mengen-/Wertfluss.)
- **Optimistic Locking:** Spalte `version BIGINT` (JPA `@Version`/jMolecules).
- **Standardspalten** je Tabelle: `id`, `created_at`, `created_by`, `modified_at`, `modified_by`,
  `version`. Stammdaten zusätzlich Historisierung (§5.2).
- **Naming:** snake_case, Schema = Modulname, Tabellen Singular (`crm.customer`).

---

## 4. Schema-Layout (= Modulgrenzen)

Eine DB pro Kunde, darin ein PG-Schema je Bounded Context:

```
core         number ranges, entity-link-graph (360°), config, uom, currency
security     RBAC, OIDC-User-Spiegel (Authentik als IdP)
crm          customer, contact, supplier, address  ← erstes Modul
catalog      product (Artikel), bom (Stücklisten) ab Tag 1
inventory    warehouse, stock, stock_movement
accounting   account (Kontenrahmen), journal_entry/line (doppelte Buchführung), tax
production   production_order (BA-Nummer), operation, routing   (später, Modell vorbereitet)
sales        quote, order, invoice (später)
documents    document-Metadaten (Bytes in MinIO/S3)
audit        manipulationssicherer Audit-Trail (hash-chained)
search       search_document (Universal-Search-Projektion)
flowable     ACT_*-Tabellen der eingebetteten Workflow-Engine (§7)
```

**Cross-Modul-Referenzen:** innerhalb eines Schemas FK normal; **über Modulgrenzen Referenz per
UUID ohne FK-Constraint**, Integrität über Domain-Events/Validierung (Spring-Modulith-Disziplin,
ArchUnit-geprüft).

---

## 5. Querschnitts-Fundamente (mit DDL-Skizzen)

### 5.1 Belegnummernkreise — lückenlos (GoBD)

PG-**Sequences sind ungeeignet** für Belegnummern (Lücken bei Rollback). Lückenlose, fortlaufende
Nummern (Rechnungen!) brauchen einen gesperrten Zähler:

```sql
CREATE TABLE core.number_range (
  id            uuid PRIMARY KEY,
  key           text NOT NULL,                 -- z.B. 'INVOICE', 'ORDER'
  fiscal_year   int  NOT NULL,
  prefix        text NOT NULL DEFAULT '',      -- z.B. 'RE-2026-'
  current_value bigint NOT NULL DEFAULT 0,
  format        text NOT NULL DEFAULT '{prefix}{value:06d}',
  reset_yearly  boolean NOT NULL DEFAULT true,
  UNIQUE (key, fiscal_year)
);
-- Vergabe in eigener, kurzer Transaktion: SELECT ... FOR UPDATE → +1 → commit.
-- Lückenlosigkeit nur für Belegarten, die es rechtlich verlangen.
```

### 5.2 Audit-Trail — manipulationssicher (GoBD)

Append-only, **hash-verkettet** (jede Zeile trägt Hash der vorherigen → tamper-evident):

```sql
CREATE TABLE audit.audit_log (
  id          uuid PRIMARY KEY,               -- UUIDv7 = chronologisch
  occurred_at timestamptz NOT NULL DEFAULT now(),
  actor       text NOT NULL,                  -- OIDC sub / 'system'
  module      text NOT NULL,
  entity_type text NOT NULL,
  entity_id   uuid NOT NULL,
  action      text NOT NULL,                  -- CREATE/UPDATE/DELETE/POST/...
  diff        jsonb,                          -- before/after (PII ggf. crypto-shredded, §8)
  prev_hash   bytea,
  row_hash    bytea NOT NULL                  -- = H(prev_hash || canonical(row))
);
-- Kein UPDATE/DELETE (per Rolle + ggf. BEFORE-Trigger erzwingen).
```

Buchungen (`accounting.journal_*`) sind ebenfalls append-only — Korrektur nur per **Storno-Buchung**.

### 5.3 Entity-Link-Graph — das 360°-Fundament

Generische, indizierte Kanten-Tabelle, die die modulübergreifenden Beziehungen für Cockpit/
Universal-Search trägt:

```sql
CREATE TABLE core.entity_link (
  id          uuid PRIMARY KEY,
  from_type   text NOT NULL,   -- 'crm.customer'
  from_id     uuid NOT NULL,
  to_type     text NOT NULL,   -- 'sales.order'
  to_id       uuid NOT NULL,
  link_type   text NOT NULL,   -- 'HAS_ORDER', 'HAS_DOCUMENT', ...
  created_at  timestamptz NOT NULL DEFAULT now(),
  UNIQUE (from_type, from_id, to_type, to_id, link_type)
);
CREATE INDEX ON core.entity_link (from_type, from_id);
CREATE INDEX ON core.entity_link (to_type, to_id);
-- Gepflegt über Domain-Events. Liefert: "zeig mir alles zu BA-Nummer X".
```

### 5.4 Universal-Search-Projektion

```sql
CREATE TABLE search.search_document (
  entity_type text NOT NULL,
  entity_id   uuid NOT NULL,
  tenant_key  text,                 -- für späteren Shared-Mode
  title       text NOT NULL,        -- "Müller GmbH"
  subtitle    text,                 -- "Kunde · 10001"
  body        tsvector NOT NULL,    -- gewichteter Volltext
  payload     jsonb,                -- Display-Felder fürs Suchergebnis
  PRIMARY KEY (entity_type, entity_id)
);
CREATE INDEX ON search.search_document USING GIN (body);
-- Per Domain-Event aktuell gehalten; Migration auf OpenSearch = Adapter-Tausch.
```

### 5.5 Event-Projektionen / Outbox

In-JVM-Domain-Events laufen ACID über die **Spring Modulith Event Publication Registry**
(`event_publication`-Tabelle = transaktionaler Outbox, mit Retry). Projektionen (Search, Entity-Link,
später CDC) hängen daran. Kein selbstgebautes Outbox-Rad.

---

## 6. Kern-Fachtabellen (Skizzen)

### 6.1 Security / RBAC (Authentik = IdP, keine Passwörter in der ERP-DB)

```sql
CREATE TABLE security.app_user (        -- Spiegel des OIDC-Subjekts
  id uuid PRIMARY KEY, oidc_sub text UNIQUE NOT NULL,
  email text NOT NULL, display_name text, active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE security.role (id uuid PRIMARY KEY, key text UNIQUE NOT NULL, name text NOT NULL);
CREATE TABLE security.permission (id uuid PRIMARY KEY, key text UNIQUE NOT NULL); -- 'crm.customer.read'
CREATE TABLE security.user_role (user_id uuid, role_id uuid, PRIMARY KEY (user_id, role_id));
CREATE TABLE security.role_permission (role_id uuid, permission_id uuid, PRIMARY KEY (role_id, permission_id));
```

### 6.2 Catalog: Artikel + Stücklisten (BOM ab Tag 1)

```sql
CREATE TABLE catalog.product (
  id uuid PRIMARY KEY, sku text UNIQUE NOT NULL, name text NOT NULL,
  type text NOT NULL,            -- MATERIAL / SEMIFINISHED / FINISHED / SERVICE
  base_uom text NOT NULL, version bigint NOT NULL DEFAULT 0, /* + Standardspalten */ );

CREATE TABLE catalog.bom (
  id uuid PRIMARY KEY, product_id uuid NOT NULL REFERENCES catalog.product(id),
  bom_version int NOT NULL, valid_from date, valid_to date,
  UNIQUE (product_id, bom_version));

CREATE TABLE catalog.bom_line (
  id uuid PRIMARY KEY, bom_id uuid NOT NULL REFERENCES catalog.bom(id),
  component_id uuid NOT NULL REFERENCES catalog.product(id),  -- selbst BOM-fähig → mehrstufig
  quantity numeric(19,6) NOT NULL, uom text NOT NULL, position int NOT NULL);
```

### 6.3 Accounting: Doppelte Buchführung + Mengen-/Wertfluss

```sql
CREATE TABLE accounting.account (              -- Kontenrahmen (SKR03/04 für DATEV)
  id uuid PRIMARY KEY, number text UNIQUE NOT NULL, name text NOT NULL, type text NOT NULL);

CREATE TABLE accounting.journal_entry (         -- Buchungssatz (Kopf), append-only
  id uuid PRIMARY KEY, doc_number text NOT NULL,           -- aus number_range
  posting_date date NOT NULL, fiscal_year int NOT NULL,
  reversed_by uuid REFERENCES accounting.journal_entry(id),-- Storno-Verweis
  created_at timestamptz NOT NULL DEFAULT now(), created_by text NOT NULL);

CREATE TABLE accounting.journal_line (          -- Soll/Haben + Menge UND Wert
  id uuid PRIMARY KEY, entry_id uuid NOT NULL REFERENCES accounting.journal_entry(id),
  account_id uuid NOT NULL REFERENCES accounting.account(id),
  debit numeric(19,4) NOT NULL DEFAULT 0, credit numeric(19,4) NOT NULL DEFAULT 0,
  currency_code char(3) NOT NULL DEFAULT 'EUR',
  quantity numeric(19,6), uom text,            -- Mengenfluss am Wertfluss
  tax_code text, cost_center text);
-- Lagerbewegung (inventory.stock_movement) erzeugt bei Bestandsführung die korrespondierende Buchung.
```

### 6.4 Documents (Bytes in MinIO/S3)

```sql
CREATE TABLE documents.document (
  id uuid PRIMARY KEY,
  owner_type text NOT NULL, owner_id uuid NOT NULL,   -- polymorph → 360°
  filename text NOT NULL, mime_type text NOT NULL, size_bytes bigint NOT NULL,
  storage_key text NOT NULL,            -- S3/MinIO-Objektschlüssel
  sha256 bytea NOT NULL,                -- Integrität/Dedupe
  created_at timestamptz NOT NULL DEFAULT now(), created_by text NOT NULL);
```

---

## 7. Workflow-Engine — NICHT selbst bauen

Statt eine Prozess-Engine selbst zu schreiben: **Flowable (oder Camunda 7) eingebettet** in Spring
Boot, BPMN 2.0, eigene `ACT_*`-Tabellen in derselben PostgreSQL-DB (eigenes Schema `flowable`).
Bewährt, transaktional integriert, spart Monate. (Deckt Genehmigungen, Statusübergänge, Belegfluss.)

---

## 8. DSGVO ↔ GoBD (der echte Konflikt)

- **Crypto-Shredding für PII** (v.a. `crm.contact`, `audit.diff`): PII-Felder mit
  daten-subjekt-spezifischem Schlüssel verschlüsseln; „Löschen" = Schlüssel im KMS vernichten →
  alle Kopien kryptografisch unlesbar (skalierbar in append-only Strukturen).
- **ABER: GoBD/AO-Aufbewahrungspflicht (i.d.R. 10 Jahre) schlägt Art.-17-Löschung** für
  buchungsrelevante Belege. → Erasure nur für **nicht-aufbewahrungspflichtige** PII; aufbewahrungs-
  pflichtige Datensätze bleiben, werden aber zugriffsbeschränkt. Diese Unterscheidung gehört ins
  Datenmodell (Flag „retention_locked"/Rechtsgrund). Vorrang: **Datenminimierung** (Art. 5).

---

## 9. Reporting / OLAP (später, sauber getrennt)

Kein Reporting auf der OLTP-DB unter Last. Wenn Dashboards < 1 s auf großen Mengen brauchen:
**Debezium-CDC** vom PG-WAL → **ClickHouse** (spaltenorientiert). Bis dahin: read-only auf einer
PG-Replica oder materialisierte Sichten. HTAP wird bewusst vermieden.

---

## 10. Betrieb

- **Flyway je Modul** (`db/migration/<schema>/`), aggregiert beim Start; Versionswechsel automatisiert.
- **Backup 3-2-1** je Kunden-DB (PITR via WAL-Archiv + tägliches Basis-Backup); MinIO-Bucket
  versioniert. Für GoBD-Archiv ggf. **Object-Lock (WORM)**.
- **Verschlüsselung:** at-rest (DB + S3), CMEK/eigene Schlüssel für Datenhoheit.

---

## 11. Anti-Pattern-Checkliste (bewusst vermieden)

- 🔴 BLOBs in der DB → MinIO/S3 (§2, §6.4)
- 🔴 HTAP / Reporting auf OLTP → CDC-Trennung (§9)
- 🔴 PII unwiderruflich in append-only ohne Löschkonzept → Crypto-Shredding (§8)
- 🟠 Belegnummern aus Sequences (Lücken) → gesperrter Zähler (§5.1)
- 🟠 Float für Geld → NUMERIC (§3)
- 🟠 Random-UUIDv4-PK-Bloat → UUIDv7 (§3)
- 🟠 Harte FK über Modulgrenzen → Referenz per ID + Events (§4)
- 🟡 JSONB ohne „Shredding" → häufig gefilterte Felder als echte Spalten (Skill-Regel)
- 🟡 Selbstgebaute Workflow-/Outbox-Engine → Flowable + Modulith-Registry (§5.5, §7)

---

## 12. Start-Empfehlung (M0) in einem Satz

**Eine PostgreSQL-DB pro Kunde, modular in Schemas, mit den Querschnitts-Fundamenten
(Nummernkreise, hash-chained Audit, Entity-Link-Graph, Such-Projektion, Modulith-Event-Registry),
Belegen in MinIO, Flowable eingebettet** — Redis/OpenSearch/ClickHouse erst bei belegtem Bedarf.
