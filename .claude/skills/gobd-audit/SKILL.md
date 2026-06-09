---
name: gobd-audit
description: GoBD compliance audit for Volantic ERP — Belegnummernkreise, Unveränderbarkeit, Audit-Trail, Aufbewahrungsfristen. Use when implementing document numbering, audit logging, or reviewing accounting-related code for GoBD compliance.
---

# GoBD Audit Skill

GoBD (Grundsätze zur ordnungsmäßigen Führung und Aufbewahrung von Büchern, Aufzeichnungen und Unterlagen) compliance checklist for Volantic ERP.

**Scope:** Relevant für `buchhaltung`, `verkauf`, `belege`, `audit` Module.

## When to Use
- Implementing Belegnummernkreise (document numbering)
- Implementing Audit-Trail / Change-Log
- Reviewing accounting, invoice, or order code
- Before any release touching `buchhaltung` or `belege`

---

## 1. Belegnummernkreise (Document Numbering)

### Requirements
- **Lückenlos**: No gaps in sequence — every number issued must be accounted for (even cancelled documents)
- **Je Belegart/Mandant**: Separate sequence per document type and tenant
- **Unveränderbar**: Issued numbers cannot be reused or skipped

### Implementation Pattern
```java
@Entity
@Table(name = "belegnummernkreis",
       uniqueConstraints = @UniqueConstraint(columnNames = {"belegart", "jahr", "mandant_id"}))
public class Belegnummernkreis {

    @Id @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    private Belegart belegart;  // RECHNUNG, ANGEBOT, LIEFERSCHEIN, ...

    private int jahr;
    private String prefix;     // "RE-2025-"
    private long letzteNummer; // atomic counter

    @Version  // Optimistic Locking — prevents duplicate numbers under concurrency
    private long version;
}

@Service
@RequiredArgsConstructor
public class BelegnummerService {

    private final BelegnummernkreisRepository repository;

    @Transactional  // MUST be in same transaction as document creation
    public String naechsteNummer(Belegart belegart, int jahr, MandantId mandantId) {
        var kreis = repository.findByBelegartAndJahrAndMandantId(belegart, jahr, mandantId)
            .orElseThrow(() -> new BelegnummernkreisNichtGefunden(belegart, jahr));

        kreis.incrementiereNummer();
        repository.save(kreis);  // optimistic lock protects concurrent access

        return kreis.formatiereNummer();
    }
}
```

### Checklist
- [ ] Nummernkreis und Dokument werden in **derselben Transaktion** angelegt
- [ ] Optimistic Locking (`@Version`) oder pessimistisches Lock bei hoher Nebenläufigkeit
- [ ] Storniertes Dokument behält seine Nummer (Status = STORNIERT, nicht gelöscht)
- [ ] Nummernlücken werden protokolliert und begründet
- [ ] Jahreswechsel erzeugt neuen Nummernkreis (nicht fortlaufend über Jahre hinweg, sofern steuerlich nicht anders)

---

## 2. Unveränderbarkeit / Immutability

### Requirements
- Gebuchte/abgeschlossene Belege dürfen **nicht geändert** werden
- Korrekturen erfolgen über Stornobelege (Gegenbuchungen), nie durch UPDATE auf dem Original

### Pattern: Immutable Documents
```java
@Entity
public class Rechnung {

    @Enumerated(EnumType.STRING)
    private RechnungStatus status;  // ENTWURF → GEBUCHT → STORNIERT

    // ✅ Enforce immutability at domain level
    public void buchen() {
        if (this.status != RechnungStatus.ENTWURF) {
            throw new RechnungBereitsGebucht(this.rechnungsnummer);
        }
        this.status = RechnungStatus.GEBUCHT;
        // Publish event for audit trail
    }

    // ✅ Storno creates a new document, not an update
    public StornoRechnung stornieren(String grund) {
        if (this.status != RechnungStatus.GEBUCHT) {
            throw new RechnungNichtStornierbar(this.status);
        }
        this.status = RechnungStatus.STORNIERT;
        return new StornoRechnung(this, grund);
    }
}
```

### Checklist
- [ ] Keine UPDATE-Queries auf gebuchten Belegen (nur INSERT für Storni/Korrekturbuchungen)
- [ ] Status-Transitions explizit modelliert (State Machine oder `@DomainEvents`)
- [ ] DB-Level: `CHECK CONSTRAINT` oder Trigger schützen GEBUCHT-Zeilen vor UPDATE
- [ ] API: PUT/PATCH auf gebuchten Belegen gibt 409 Conflict mit sprechender Fehlermeldung

---

## 3. Audit-Trail

### Requirements
- Jede Datenänderung muss rückverfolgbar sein (Wer? Wann? Was? Warum?)
- Audit-Log ist selbst unveränderlich (append-only, keine Updates/Deletes)

### Implementation with Spring Data Envers or Custom
```java
// Option A: Hibernate Envers (empfohlen für einfache Fälle)
@Entity
@Audited
public class Artikel {
    // Envers trackt automatisch INSERT/UPDATE/DELETE in artikel_aud
}

// Option B: Custom Audit-Trail (für geschäftliche Ereignisse, empfohlen für GoBD)
@Entity
@Table(name = "audit_ereignis")
public class AuditEreignis {

    @Id @GeneratedValue
    private UUID id;

    private Instant zeitpunkt;
    private String nutzerkennung;
    private String modulName;
    private String ereignistyp;      // ANLAGE, AENDERUNG, STORNO, LOESCHUNG
    private String referenzTyp;      // "Rechnung", "Kunde", ...
    private String referenzId;
    private String altWert;          // JSON-serialisierter Vorher-Zustand
    private String neuWert;          // JSON-serialisierter Nachher-Zustand
    private String begruendung;      // optional, bei manuellen Korrekturen
}
```

### Checklist
- [ ] Audit-Tabellen haben **kein** DELETE/UPDATE-Recht für die App-DB-User
- [ ] Audit-Einträge enthalten: Zeitstempel (UTC), Nutzerkennung, IP/Session, Modul, Entität, Vorher/Nachher
- [ ] Strukturierte Daten (JSON) statt Freitext für maschinelle Auswertbarkeit
- [ ] Aufbewahrungsfrist: 10 Jahre (§147 AO) — Löschung nur auf expliziten Antrag mit Protokoll
- [ ] DSGVO vs. GoBD: steuerrelevante Daten (10 Jahre) schlagen DSGVO-Löschanfragen, muss in Datenschutzerklärung transparent sein

---

## 4. Aufbewahrungsfristen

| Dokumenttyp | Frist | Rechtsgrundlage |
|-------------|-------|-----------------|
| Handelsbücher, Inventare, Bilanzen | 10 Jahre | §257 HGB, §147 AO |
| Handelsbriefe, Buchungsbelege | 6 Jahre | §257 HGB |
| Rechnungen (Eingang + Ausgang) | 10 Jahre | §14b UStG |
| Sonstige geschäftliche Unterlagen | 6 Jahre | §257 HGB |

### Implementation Pattern
```java
@Entity
public class Dokument {
    private Instant erstelltAm;
    private DokumentTyp typ;

    public Instant loeschbarAb() {
        return switch (typ) {
            case RECHNUNG, BUCHUNGSBELEG, HANDELSBUCH -> erstelltAm.plus(10 * 365, DAYS);
            case HANDELSBRIEF, SONSTIGES -> erstelltAm.plus(6 * 365, DAYS);
        };
    }

    public boolean istLoeschbar() {
        return Instant.now().isAfter(loeschbarAb());
    }
}
```

---

## 5. Verfahrensdokumentation

GoBD §14 verlangt eine **Verfahrensdokumentation**. Für Volantic ERP bedeutet das:

- [ ] `docs/verfahrensdokumentation/` im Repo anlegen
- [ ] Beschreibung der Datenflüsse für steuerrelevante Prozesse
- [ ] Exportformat und -verfahren für Steuerprüfung (DATEV, GDPdU/GoBD-Export)
- [ ] Änderungen an der Verfahrensdokumentation versioniert (Git ist GoBD-kompatibel als Versionierungssystem)

---

## Common GoBD Mistakes

| Fehler | GoBD-Verstoß | Fix |
|--------|-------------|-----|
| Buchungsbeleg per UPDATE korrigieren | Verstoß §146 AO | Stornobeleg + Neubeleg |
| Audit-Log in selber Tabelle wie Nutzdaten | Manipulierbar | Separate, schreibgeschützte Tabelle |
| Nummernkreis-Lücken ohne Begründung | Verstoß | Status STORNIERT + Protokoll |
| DSGVO-Löschung auf steuerrelevanten Daten | Verstoß §147 AO | Anonymisierung statt Löschung |
| Belege nur in Anwendung, nicht exportierbar | Verstoß | GoBD-Export-Tool pflichtmäßig |
