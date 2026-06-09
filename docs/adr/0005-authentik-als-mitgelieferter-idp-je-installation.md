# ADR-0005 — Authentik als mitgelieferter IdP je Installation

- **Status:** Akzeptiert
- **Datum:** 2026-06-09
- **Entscheider:** Tom (Volantic)
- **Betrifft:** §4a Masterplan, Modul `security` (IAM), Deployment/Packaging
- **Hängt zusammen mit:** ADR-0004 (Autorisierungsmodell), CLAUDE.md (Authentik als Auth-Provider)

## Kontext

Volantic ERP wird als **self-hosted**-Produkt an den DACH-Mittelstand verkauft: der Kunde betreibt
die Anwendung auf eigener Infrastruktur. Authentifizierung läuft über **Authentik (OIDC)**; im ERP
liegen keine Passwörter, nur der OIDC-Subject-Bezug (ADR-0004, DB-Architektur §6).

Bisher zeigte die Konfiguration als Default auf `auth.volantic.de` — **unsere** zentrale
Authentik-Instanz. Für ein self-hosted-Produkt ist das falsch:

- **DSGVO/Datenhoheit:** Identitäten (Mitarbeiter des Kunden) dürfen nicht zwangsläufig über einen
  von Volantic betriebenen IdP laufen. Der Kunde will/soll seinen IdP selbst besitzen.
- **Verfügbarkeit/Kopplung:** Eine laufende Kundeninstallation darf nicht von einem zentralen,
  von Volantic gehosteten Dienst abhängen (Single Point of Failure, NFR Drop-In-Update/Rollback).
- **Betrieb:** Der Kunde soll das ERP als geschlossene Einheit hochfahren können, ohne vorher selbst
  einen IdP aufzusetzen und manuell OIDC-Clients zu konfigurieren.

Optionen:
1. **Zentraler Volantic-IdP** (`auth.volantic.de`) für alle Kunden. Einfachste Auslieferung, aber
   verletzt Datenhoheit und koppelt jede Installation an unsere Infrastruktur.
2. **Kunde stellt eigenen IdP** (bring-your-own), ERP nur env-konfiguriert. Maximale Flexibilität,
   aber hohe Einstiegshürde — der Kunde muss Authentik/Keycloak selbst betreiben und korrekt einrichten.
3. **Authentik je Installation mitgeliefert** (gebündelt), vorkonfiguriert, vom Kunden überschreibbar.

## Entscheidung

**Option 3.** Jede Installation **liefert ihren eigenen Authentik mit**:

- **Bundle:** Das Deployment ist eine Einheit (z. B. `docker-compose`) aus Authentik (Server +
  Worker), dessen eigener PostgreSQL + Redis, der ERP-PostgreSQL und der ERP-Anwendung. Der Kunde
  startet das Bundle als Ganzes.
- **Vorkonfiguration per Blueprint:** Authentik wird beim ersten Start über ein
  **Authentik-Blueprint** automatisch eingerichtet — Application + OIDC-Provider „volantic-erp"
  (Redirect-URIs, Scopes, Signaturschlüssel) entstehen ohne manuelles Klicken.
- **ERP bleibt reiner Resource Server**, vollständig env-getrieben: `OIDC_ISSUER_URI` und
  `OIDC_JWK_SET_URI` zeigen pro Installation auf den **mitgelieferten** Authentik (im Compose-Netz
  z. B. `http://authentik-server:9000/application/o/volantic-erp/`). Die Defaults im Code
  (`auth.volantic.de`) sind ausdrücklich nur unser **Dev-/Demo-Issuer**.
- **Bring-your-own bleibt möglich:** Will ein Kunde seinen bestehenden IdP nutzen, überschreibt er
  schlicht die OIDC-Env-Variablen und lässt den gebündelten Authentik weg — derselbe Code, keine
  Änderung am ERP.
- **Boot-Resilienz:** Über zusätzlich gesetztes `jwk-set-uri` baut das ERP einen lazy JWT-Decoder
  und bootet auch, wenn Authentik im selben Bundle minimal später bereit ist (kein Start-Deadlock).

## Konsequenzen

**Positiv**
- Datenhoheit beim Kunden; keine Laufzeitabhängigkeit einer Installation zu Volantic-Infrastruktur.
- „Hochfahren und loslegen" — vorkonfigurierter IdP, kein manuelles OIDC-Setup beim Kunden.
- Sauberer Pfad zu bring-your-own-IdP, ohne zweiten Code-Pfad.

**Negativ / Kosten**
- Volantic liefert und **pflegt** Authentik mit (Versions-/Sicherheitsupdates, Blueprint-Pflege).
- Größerer Deployment-Footprint (zusätzlich Authentik + dessen DB/Redis) — mehr RAM/Container je Kunde.
- Initiale Bootstrap-Sequenz (Authentik-Migrationen + Blueprint vor erstem ERP-Login) muss robust sein.

## Verworfene Alternativen

- **Zentraler Volantic-IdP:** verletzt Datenhoheit (DSGVO) und macht jede Kundeninstallation von
  unserer Infrastruktur abhängig — unvereinbar mit self-hosted.
- **Reines bring-your-own ohne Bündelung:** zu hohe Einstiegshürde; widerspricht dem Ziel, dass der
  Kunde das Produkt als geschlossene Einheit starten kann.

## Offen / Folgeschritte

- `docker-compose`-Bundle (Authentik + DBs + Redis + ERP) als eigenes Deliverable (separater Schritt).
- Authentik-Blueprint für die `volantic-erp`-Application/Provider versionieren und mit ausliefern.
- Update-/Backup-Strategie für den mitgelieferten Authentik in die Drop-In-Update-Prozedur einarbeiten.
