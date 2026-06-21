# ADR-0006 — Massenänderungen, Probemodus & Rollback Engine über kompensierende Operationen

- **Status:** Vorgeschlagen
- **Datum:** 2026-06-19
- **Entscheider:** Tom (Volantic)
- **Betrifft:** modulübergreifend (alle Fachmodule), neues Modul `changeset`, `core`-SPI
- **Hängt zusammen mit:** Audit-Trail (#23, hash-verkettet/unveränderlich), Belegnummernkreise
  (`core.numberrange`), ADR-0004 (Enforcement an der Service-Grenze), `workflow` (Genehmigungen)

## Kontext

Tom will drei zusammenhängende Fähigkeiten:

1. **Massenänderungen** — in *jedem* Modul schnell den gleichen Wert auf viele Datensätze anwenden.
2. **Rollback Engine** — eine ganze Bearbeitungs-Session nachträglich zurücknehmen. Ausdrücklich **nicht**
   durch Löschen, sondern indem das System **automatisch Storno-/Gegenoperationen zu allem ausführt, was
   in der Session passiert ist** (forward-only Kompensation).
3. **Probemodus** — ein *explizit zu aktivierender* Modus, in dem Änderungen **gar nicht real ausgeführt
   werden**, bis am Ende „Übertragen" geklickt wird. Verwerfen = es ist nie etwas geschehen.

Spannungsfeld: Das ERP hat einen **manipulationssicheren, hash-verketteten Audit-Trail** (GoBD/NIS2)
und Belegnummernkreise mit **Unveränderbarkeit**. Stilles „Zurückrollen" (Löschen, DB-Restore,
Transaktions-Rollback einer Session) würde GoBD verletzen und die Audit-Kette zerstören. Aktueller
Stand: nur **Stammdaten** (`crm`, `catalog`); Belege (Verkauf, FiBu = M7) kommen später, müssen aber
von Beginn an antizipiert werden.

> **Namensgebung (festgelegt 2026-06-19).** Die Rücknahme-Funktion heißt nach außen
> **„Rollback Engine"**, der aufgeschobene Modus **„Probemodus"**. Intern/technisch bleibt der
> Kernbegriff `ChangeSet`.

## Entscheidung (vorgeschlagen)

### 1. Ein `ChangeSet` mit zwei Modi
Eine Bearbeitungs-Session ist ein `ChangeSet` (Aggregat im neuen Modul `changeset`), das die Operationen
eines Akteurs bündelt. Es hat einen **Modus**:

| Modus | Wann wird real geschrieben? | Rücknahme |
|---|---|---|
| `LIVE` | sofort bei jeder Aktion (regulärer Betrieb) | **Rollback Engine**: nachträglicher forward-Storno |
| `DEFERRED` (**Probemodus**) | **nie**, bis `COMMIT` („Übertragen") | `DISCARD` = nichts ist passiert |

`status`: `OPEN → COMMITTED | REVERTED | DISCARDED`. Probemodus muss **explizit aktiviert** werden
(eigene Permission, eigene Aktion); ohne aktiven Probemodus laufen Änderungen `LIVE`.

### 2. Forward-only Kompensation ist das einzige Undo-Modell (Rollback Engine)
Die Rollback-Engine-Rücknahme löscht nie und manipuliert den Audit-Trail nie. Jede Rücknahme ist eine
**neue, auditierte Operation**:

| Ursprüngliche Operation | Kompensation |
|---|---|
| CREATE | fachliche Rücknahme (Soft-Delete/Deaktivieren), solange kein Beleg referenziert |
| UPDATE | Rückbuchung auf den vorherigen Snapshot (neuer, regulärer UPDATE) |
| DELETE | Wiederanlage aus dem Snapshot |
| (später) gebuchter Beleg | **Stornobeleg** im selben Nummernkreis (`core.numberrange`) |

### 3. Probemodus = aufgeschobene Operationen, atomar übertragen
Im Probemodus wird **kein** realer Write ausgeführt; jede beabsichtigte Operation wird mitsamt ihrer
Nutzlast (Feldänderungen) im `ChangeSet` gepuffert und gegen den **aktuellen** Stand vorab validiert
(Dry-Run). „Übertragen" wendet alle gepufferten Operationen transaktional über die Domain-Services an
(echte, auditierte Writes); „Verwerfen" lässt das `ChangeSet` ohne jede Wirkung zurück.

### 4. Per-Aggregat-SPI über eine Registry (Idiom wie `EntityLinkRegistry`)
Das generische Modul kennt nur `core.entitylink.EntityRef` + generische Operationen, **nie**
modulspezifische Typen. Jedes Fachmodul liefert zwei kleine Handler als Spring-Beans (in `core`-SPI):

- `ReversibleResourceHandler` — Zustand erfassen (`capture`) und eine Operation kompensieren
  (`compensate`) — für die Rollback Engine und für `DELETE`-Wiederanlage.
- `BulkEditHandler` — welche Felder sind massen-änderbar, wie wird eine Feldänderung angewendet
  (inkl. Validierung) — für Massenänderung und für Probemodus-Übertragung.

So entsteht keine Kopplung der Fachmodule untereinander; das `changeset`-Modul sammelt die Handler-Beans.

### 5. Massenänderung = Dry-Run + Apply
Selektion (IDs oder Filter) + Feldänderungen → **Vorschau** (Diff je Satz + Validierungsfehler, ändert
nichts) → **Apply** transaktional über den Domain-Service. **Kein direkter DB-Zugriff** (CLAUDE.md):
dieselbe Schreiblogik wie Einzeländerungen, nur protokolliert. Läuft `LIVE`, ist der Batch per Rollback
Engine rücknehmbar; läuft Probemodus, wird er erst bei „Übertragen" wirksam.

### 6. Alles auditiert
(Massen-)Änderung, Übertragung und Kompensation gehen durch `AuditTrail`. Der Rollback-Revert erzeugt
eine zusammenhängende, nachvollziehbare Storno-Sequenz mit Verweis auf das Ursprungs-`ChangeSet`.

### 7. Berechtigung & Genehmigung
Eigene Permissions: `changeset.bulk:execute`, `changeset.probemodus:activate`,
`changeset.rollback:revert` — zusätzlich zu den jeweiligen `*:update`/`*:delete`-Permissions der
Ressource. Große Batches können optional über das `workflow`-Modul vier-Augen-genehmigt werden.

## Konsequenzen

**Positiv**
- GoBD/NIS2 bleiben intakt: forward-only, der Trail wird nie gelöscht/umgeschrieben. Probemodus schreibt
  bis „Übertragen" gar nichts — noch sauberer.
- Rollback Engine und Probemodus teilen sich eine Maschinerie (DRY), unterscheiden sich nur im Modus.
- Modulübergreifend ohne Kopplung — ein neues Modul liefert nur zwei kleine Handler.
- Wiederverwendung bestehender Bausteine: `EntityRef`, `AuditTrail`, `NumberRanges`, `workflow`.

**Negativ / Kosten**
- Snapshots/Nutzlasten kosten Speicher und müssen stabil serialisiert werden (Schema-Evolution beachten).
- Kompensation **gebuchter Belege** ist erst mit dem Beleg-Modell vollständig — jetzt existiert nur der
  Stammdaten-Pfad; der Storno-Pfad kommt als Handler-Variante nach.
- Große Batches gegen das sub-500-ms-Budget → ggf. asynchron mit Fortschritts-Rückmeldung.

## Verworfene Alternativen

- **Hartes Löschen / Transaktions-Rollback einer ganzen Session:** GoBD-widrig, zerstört die Audit-Kette.
- **DB-Snapshot & Restore pro Session:** nicht mandantenfein, überschreibt fremde Änderungen.
- **Direkter DB-Zugriff für Massenänderung:** umgeht Validierung/Hooks/Audit — in CLAUDE.md verboten.
