# ADR-0007 — Organisationseinheiten als hierarchische Scope-Dimension

- **Status:** Akzeptiert
- **Datum:** 2026-06-22
- **Entscheider:** Tom (Volantic)
- **Betrifft:** Modul `security` (IAM), `crm`, `catalog`, `sales`
- **Hängt zusammen mit:** ADR-0004 (RBAC + Authorization-Port), DB-Architektur §6

## Kontext

ADR-0004 versprach drei Granularitätsebenen der Autorisierung: Aktion/Ressource (RBAC), Feld und
**Instanz/Scope** („…von *dieser* Abteilung", „Bindung an Organisationseinheit/Ownership"). Die ersten
beiden sind live; die Scope-Ebene war zwar **vollständig verdrahtet, aber ungenutzt**:
`SecurityAdmin.assignRole(sub, role, AccessScope)`, die Spalten `user_role.scope_type/scope_id`, der
Redis-Cache (`CachedAssignment`) und die Domänenlogik `RoleAssignment.covers()` tragen einen Scope bereits
verlustfrei durch. Aber **jeder** Service prüfte `@PreAuthorize("hasPermission(null, 'res:action')")` —
also nur den globalen Fall. Folge: ein scoped Grant war wirkungslos, kein Fachobjekt trug eine
Organisationseinheit, und „ein Vertriebler sieht nur die Kunden seiner Region" (least privilege) war nicht
ausdrückbar — alles brauchte globale Rechte.

Das nachträglich einzuziehen ist die teure, querschnittliche Änderung, vor der ADR-0004 warnte. Sie wird
hier **einmal** vollzogen.

## Entscheidung

Eine **Organisationseinheit (`OrgUnit`)** als erstklassige, **hierarchische** Scope-Dimension:

- **Wo:** `OrgUnit` lebt im Modul `security` (sie *ist* die Scope-Dimension). Fachmodule speichern nur eine
  nackte `org_unit_id` (UUID, **keine modulübergreifende FK** — wie `sales.customer_id`) und importieren den
  Typ `OrgUnit` nie. Kein neues Modul-Coupling außer einer UUID.
- **Hierarchie (downward):** Ein Grant auf eine Einheit deckt diese **und ihre Nachfahren**. Aufgelöst in
  der Authorization-Schicht (`AuthorizationServiceImpl`) über einen `OrgUnitHierarchy`-Port; die Domäne
  (`User`/`RoleAssignment`) bleibt baum-frei. Der Baum ist klein und ändert sich selten → er wird wie der
  User-Graph in Redis gecacht (`CachedOrgTree`, Eviction nach Commit, fail-open).
- **Port additiv erweitert** (nie verengt, ADR-0004): `permittedOrgUnits(sub, perm)` (→ `empty` = global/
  unrestricted, sonst die Nachfahren-expandierte Menge erlaubter Einheiten für Listenfilter) und
  `hasPermissionAnywhere(sub, perm)` (Gate für Listen). Der bestehende `isPermitted(sub, perm, scope)`
  löst für `ORG_UNIT`-Scopes nun Vorfahren auf.
- **`ScopeEnforcer`** (öffentlich) kapselt Port + SecurityContext, damit Fachservices Instanz-Checks und
  Listenfilter durchführen, ohne Kontext oder Baum selbst anzufassen (DRY). Funktioniert identisch für
  direkte Aufrufe, den Bulk-Pfad (Handler rufen die Services) und Vier-Augen (impersoniert den Antragsteller).
- **Drei Enforcement-Muster je Aggregat:**
  - *create*: Scope kommt aus dem Request → deklarativ `@PreAuthorize("hasPermission(#orgUnitId, 'ORG_UNIT', '…:create')")`.
  - *read/update/delete*: Einheit erst nach dem Laden bekannt → `scopeEnforcer.require('…:action', entity.orgUnitId())` (post-load).
  - *list/findIds*: `requireAnywhere('…:read')`, dann Filter auf `permittedOrgUnits(...)`.

**Welche Aggregate org-scoped sind:** `crm` Customer/Supplier (eigene `orgUnitId` aus dem Request),
`crm` Address/Contact (`orgUnitId` **denormalisiert vom Owner** geerbt — gleiche, einheitliche Feldprüfung
statt Owner-Lookup je Check), `sales` Invoice (**eigene** `orgUnitId` = ausstellende Einheit aus dem
Request; Storno erbt die der Originalrechnung).

**Bewusste Grenzen (explizit, nicht implizit):**
- **`catalog` Product bleibt unternehmensweit/global** — kein `orgUnitId`, Checks bleiben global. Der
  Artikel-Stammkatalog ist gemeinsame Referenzdaten; Differenzierung je Standort gehört zu separaten
  Konzepten (Preislisten, Lagerbestand je Lager), nicht zur Aufspaltung des Stammartikels. Unter
  Downward-Hierarchie würde ein root-/global gepflegter Artikel Sub-Unit-Nutzern sonst *verborgen* — der
  falsche Default. Bleibt zukunftssicher: Product-Scoping ist später hinter demselben Port nachrüstbar.
- **`orgUnitId` ist set-at-create und unveränderlich** (kein Drift; GoBD-sicher für Belege; nicht
  massen-änderbar). „Eine Entität in eine andere Einheit verschieben" ist ein bewusster späterer
  Admin-Use-Case (additiv) inkl. Kaskadierung auf Address/Contact.
- **`OrgUnit` ist zunächst flach umparkbar nicht** — Reparenting einer bestehenden Einheit (Cycle-
  Revalidierung über den ganzen Baum) ist Folgearbeit; Anlegen unter einem Parent + Umbenennen reichen
  für den MVP.
- Bestehende Datensätze werden per Migration auf eine **gesäte Default-Root-Einheit** zurückgesetzt
  (fixe id `00000000-0000-0000-0000-000000000001`), damit nichts unzugänglich wird; bestehende globale
  Grants bleiben global und damit voll wirksam.

## Konsequenzen

**Positiv**
- Least-privilege je Organisationseinheit ist endlich ausdrückbar; die Scope-Maschinerie aus ADR-0004 wird
  real genutzt statt nur verdrahtet.
- Der querschnittliche Umbau (Scope-Dimension in jeden Service) ist **einmalig** erledigt; Hierarchie-
  Verfeinerungen (Reparenting, Product-Scoping, weitere Aggregate) gehen später **rein im `security`-Modul
  bzw. additiv**, ohne erneut alle Services anzufassen.
- Enforcement im Backend (nicht UI), auditierbar — erfüllt GoBD/NIS2.

**Negativ / Kosten**
- `read/update/delete` müssen erst laden, dann prüfen (kein rein deklaratives `@PreAuthorize` mehr) — etwas
  mehr Code pro Methode, gekapselt im `ScopeEnforcer`.
- Org-Baum-Auflösung liegt auf dem Authz-Hot-Path; durch den gecachten Baum (in-memory Walk) unkritisch,
  aber eine zusätzliche Cache-Invalidierung mehr.
- Migration setzt Alt-Daten auf die Root-Einheit; eine echte fachliche Zuordnung muss ggf. nachgepflegt
  werden.

## Verworfene Alternativen

- **Invoice erbt die Einheit vom Kunden:** verlangte einen modulübergreifenden Lookup `sales → crm` und
  bräche die Modulith-Entkopplung (sales kennt Kunden nur per UUID). Eine Rechnung gehört ohnehin zur
  **ausstellenden** Einheit, nicht zur „Eigentümer-Einheit" des Kunden.
- **Per-Record-ACLs / volle ABAC jetzt:** Über-Engineering vor realen HR/Accounting-Policies (vgl.
  ADR-0004); Org-Unit-Scope deckt den Bedarf und bleibt hinter dem Port erweiterbar.
- **Product org-scoped:** siehe Grenzen oben — falscher Default für einen gemeinsamen Stammkatalog.
