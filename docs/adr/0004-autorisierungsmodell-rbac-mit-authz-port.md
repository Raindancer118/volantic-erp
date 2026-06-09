# ADR-0004 — Autorisierungsmodell: RBAC-Basis hinter einem stabilen Authorization-Port

- **Status:** Akzeptiert
- **Datum:** 2026-06-09
- **Entscheider:** Tom (Volantic)
- **Betrifft:** §4a Masterplan, Modul `security` (IAM)
- **Hängt zusammen mit:** DB-Architektur §6 (security-Schema), ADR-0003 (enges, stabiles SPI/Port)

## Kontext

IAM ist Fachmodul #1 — alles baut darauf auf. Anforderung (Tom): **„extrem feingranulare Rechte"**,
mit dem Beispiel **„wer darf das Gehalt von Mitarbeiter X sehen?"**. Das ist bewusst mehr als
klassisches RBAC, denn es kombiniert drei Granularitäts-Ebenen:

- **Aktion/Ressource:** „darf Mitarbeiter lesen" (klassisches RBAC).
- **Feld:** „darf *das Gehaltsfeld* lesen" (feldgenau).
- **Instanz/Scope:** „…von *diesem* Mitarbeiter / dieser Abteilung" (datensatzgenau).

Authentifizierung kommt von **Authentik (OIDC)** — im ERP liegen keine Passwörter, nur der
OIDC-Subject-Bezug (DB-Architektur §6). Die **Autorisierung** ist Sache des ERP.

Das ist eine Fundament-Entscheidung: das Autorisierungsmodell nachträglich umzubauen ist teuer
(durchzieht jede Service-Methode). Gleichzeitig wäre eine voll ausgebaute Policy-Engine *jetzt* —
bevor HR/Accounting überhaupt existieren — Over-Engineering, das den MVP bremst (vgl. „SPI wird
geerntet, nicht erfunden", §3.3).

Optionen:
1. **Pure RBAC** (Rolle → Permission). Einfach, aber kann „Gehalt von Mitarbeiter X" nicht ausdrücken.
2. **Voll-ABAC / externe Policy-Engine** (Cedar, OPA) ab Tag 1. Maximal flexibel, aber schwer,
   betriebs-/latenzintensiv, und ohne reale HR/Accounting-Policies kennt niemand die echten Regeln.
3. **RBAC-Basis + feld-/instanzgenaue Prüfung hinter einem stabilen Port**, ABAC später dahinter.

## Entscheidung (vorgeschlagen)

**Option 3.** Im Einzelnen:

- **RBAC als Basis:** `User ← UserRole → Role ← RolePermission → Permission`. Permissions als
  `resource:action`-Strings (z. B. `hr.employee:read`, `hr.salary:read`, `sales.order:approve`).
  Sensible Felder bekommen eine **eigene Permission** (Gehalt = `hr.salary:read`, getrennt von
  `hr.employee:read`) — Feldgenauigkeit ohne neues Mechanik-Konzept.
- **Ein zentraler Authorization-Port** (`AuthorizationService` / Spring `PermissionEvaluator`), über den
  *jeder* Zugriff entschieden wird — Enforcement an der **Domänen-/Service-Grenze** (`@PreAuthorize` +
  Domänenchecks), nicht nur im UI. Das UI blendet zusätzlich aus, aber die Wahrheit liegt im Backend.
- **Instanz-/Scope-Ebene** über eine **Scope-Dimension** an der Permission-Zuordnung (z. B. Bindung an
  Organisationseinheit/Ownership), ausgewertet im selben Port. Start schlank: Org-Unit-/Owner-Scope;
  komplexere Regeln kommen, wenn HR sie real braucht.
- **ABAC/Policy-Engine später hinter demselben Port:** wächst eine echte Policy-Komplexität (HR,
  Accounting), wird eine Policy-Engine *hinter* dem `AuthorizationService` eingezogen — **ohne dass ein
  einziger Aufrufer sich ändert**. Der Port ist die stabile Naht (gleiche Disziplin wie das enge SPI).
- **Jede Autorisierungsentscheidung ist auditierbar** (GoBD/NIS2): Verknüpfung mit `audit.audit_log`,
  insb. Zugriffe/Verweigerungen auf sensible Felder.

## Konsequenzen

**Positiv**
- Grobes RBAC ist schnell lauffähig (IAM-Modul nicht blockiert), das Gehalts-Beispiel ist über eine
  eigene Feld-Permission + Org-Scope schon abbildbar.
- Der **Port ist die einzige Stelle**, an der die Autorisierung später wächst — kein Retrofit durch alle
  Services. Voll-ABAC bleibt möglich, ohne sich heute darauf festzulegen.
- Enforcement im Backend (nicht UI) erfüllt GoBD/NIS2.

**Negativ / Kosten**
- Das **Port-API muss von Anfang an gut sitzen** (es ist faktisch internes SPI — additiv erweiterbar,
  nie verengen). Verlangt dieselbe Vertragsdisziplin wie das externe SPI.
- Feld-Permissions können bei vielen sensiblen Feldern zahlreich werden — Namens-/Gruppierungs-
  Konvention nötig (`<modul>.<ressource>.<feld>:<aktion>`).

## Verworfene Alternativen

- **Pure RBAC:** kann Instanz-/Feld-Ebene („Gehalt von X") nicht ausdrücken — verfehlt die Kernanforderung.
- **Voll-ABAC / Policy-Engine ab Tag 1:** Over-Engineering vor Existenz realer HR/Accounting-Policies;
  Betriebs- und Latenzkosten gegen das sub-500-ms-Budget; widerspricht „geerntet, nicht erfunden".
- **Per-Record-ACLs überall:** feinste Granularität, aber Komplexitäts- und Performance-Last in jedem
  Query; nur dort einsetzen, wo wirklich nötig — nicht als Grundmodell.
