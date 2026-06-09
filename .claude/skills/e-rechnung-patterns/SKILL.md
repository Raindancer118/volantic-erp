---
name: e-rechnung-patterns
description: E-Rechnung implementation patterns for Volantic ERP — XRechnung, ZUGFeRD, EN16931, PEPPOL. Use when implementing invoice generation, e-invoice validation, or reviewing billing code. Mandatory from 01/2027 (>800k€) and 01/2028 (all).
---

# E-Rechnung Patterns Skill

Implementation patterns for XRechnung and ZUGFeRD in Volantic ERP.

**Legal deadlines:**
- **01.01.2027**: Versandpflicht für Unternehmen mit >800k € Jahresumsatz
- **01.01.2028**: Versandpflicht für alle Unternehmen (B2B)

## When to Use
- Implementing invoice generation in `verkauf` or `buchhaltung`
- Reviewing invoice-related code
- Adding ZUGFeRD PDF embedding
- Implementing PEPPOL access point

---

## Formats Overview

| Format | Standard | Typ | Einsatz |
|--------|----------|-----|---------|
| **XRechnung** | EN16931 | Reines XML | Öffentliche Auftraggeber (B2G), DE-Standard |
| **ZUGFeRD 2.x** | EN16931 | PDF/A-3 + XML | B2B, international, am weitesten verbreitet |
| **PEPPOL BIS** | EN16931 | XML | B2B international, PEPPOL-Netzwerk |

**Volantic ERP Strategie:** ZUGFeRD für B2B (PDF + eingebettetes XML), XRechnung für B2G.

---

## 1. Recommended Library: Mustang

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.mustangproject:library:2.14.0")
}
```

Mustang ist die führende Open-Source-Bibliothek für ZUGFeRD/XRechnung in Java.

---

## 2. ZUGFeRD Rechnungsgenerierung

### Domain Model
```java
// Volantic ERP Invoice DTO → ZUGFeRD
public record EInvoiceData(
    String rechnungsnummer,
    LocalDate rechnungsdatum,
    LocalDate faelligkeitsdatum,
    Adresse verkaefer,
    Adresse kaeufer,
    List<RechnungsPosition> positionen,
    BigDecimal nettobetrag,
    BigDecimal steuerbetrag,
    BigDecimal bruttobetrag,
    String waehrung,  // "EUR"
    String zahlungsreferenz
) {}
```

### ZUGFeRD XML generieren (Mustang)
```java
@Service
@RequiredArgsConstructor
public class ZugferdService {

    public byte[] generateZugferdPdf(EInvoiceData data, byte[] pdfContent) {
        var invoice = new Invoice();
        invoice.setNumber(data.rechnungsnummer());
        invoice.setIssueDate(data.rechnungsdatum());
        invoice.setDueDate(data.faelligkeitsdatum());
        invoice.setCurrency(data.waehrung());

        // Sender
        invoice.setSender(toTradeParty(data.verkaefer()));

        // Recipient
        invoice.setRecipient(toTradeParty(data.kaeufer()));

        // Line items
        data.positionen().forEach(pos -> invoice.addItem(toItem(pos)));

        // Embed XML into existing PDF
        var exporter = new ZUGFeRDExporterFromPDFA()
            .setProducer("Volantic ERP")
            .setCreator("Volantic ERP")
            .setZUGFeRDVersion(2)
            .setProfile(Profiles.EN16931);  // Comfort profile — use for B2B

        exporter.load(pdfContent);
        exporter.setTransaction(invoice);

        try (var baos = new ByteArrayOutputStream()) {
            exporter.export(baos);
            return baos.toByteArray();
        }
    }

    private TradeParty toTradeParty(Adresse adresse) {
        return new TradeParty(
            adresse.name(),
            adresse.strasse(),
            adresse.plz(),
            adresse.ort(),
            adresse.land()  // ISO 3166-1 alpha-2, e.g. "DE"
        ).addTaxID(adresse.steuernummer())
         .addVATID(adresse.ustIdNr());
    }
}
```

---

## 3. XRechnung (B2G — Öffentliche Auftraggeber)

```java
// XRechnung uses UBL or CII XML format — Mustang supports both
var exporter = new ZUGFeRDExporterFromPDFA()
    .setProfile(Profiles.XRECHNUNG);  // XRechnung 3.0

// Required for XRechnung: Leitweg-ID (routing identifier for public sector)
invoice.setRecipient(new TradeParty("Bundesministerium", ...)
    .setLeitweg("991-54820-40"));  // Leitweg-ID from Auftraggeber
```

### XRechnung-Pflichtfelder (zusätzlich zu ZUGFeRD)
- [ ] Leitweg-ID des Empfängers (`BT-10`)
- [ ] Käufer-Referenz (`BT-10`)  
- [ ] Lieferantennummer beim Auftraggeber (`BT-46`)

---

## 4. EN16931 Validation

Alle ausgehenden Rechnungen **müssen** gegen EN16931 validiert werden.

```java
@Service
public class InvoiceValidationService {

    public ValidationResult validate(byte[] zugferdPdf) {
        var importer = new ZUGFeRDImporter(zugferdPdf);
        if (!importer.containsZUGFeRD()) {
            return ValidationResult.failure("Kein ZUGFeRD-XML im PDF gefunden");
        }

        // Mustang built-in validation
        var errors = importer.validate();
        if (!errors.isEmpty()) {
            return ValidationResult.failure(errors);
        }

        return ValidationResult.success();
    }
}
```

**Externer Validator (empfohlen für CI):**
```bash
# KoSIT Validator (offizielle DE-Referenzimplementierung)
./gradlew downloadKositValidator  # Custom task
java -jar validationtool.jar -s xrechnung-3.0.0/scenarios.xml invoice.xml
```

---

## 5. PEPPOL (optional, B2B international)

```kotlin
// build.gradle.kts
dependencies {
    implementation("network.oxalis:oxalis-api:6.x.x")
    // Or use a PEPPOL Access Point provider (Pagero, Storecove, etc.)
}
```

PEPPOL erfordert ein zertifiziertes Access Point — für Volantic ERP initial über einen Provider (SaaS), nicht selbst betrieben.

---

## 6. Checklist vor Release

### Technisch
- [ ] ZUGFeRD-XML ist valide gegen EN16931 (KoSIT-Validator in CI)
- [ ] PDF/A-3 Konformität geprüft (veraCrypt oder verapdf in CI)
- [ ] Alle Pflichtfelder gemäß EN16931 befüllt (BT-1 bis BT-139)
- [ ] Steuerberechnung korrekt (MwSt-Sätze 19%, 7%, 0%)
- [ ] Betragsfelder mit korrekter Präzision (2 Dezimalstellen, korrektes Runden)
- [ ] Datum-Formate: ISO 8601 (yyyy-MM-dd)
- [ ] Währungscode: ISO 4217 ("EUR")

### Rechtlich
- [ ] Verkäufer-Angaben vollständig: Name, Adresse, USt-IdNr., Steuernummer
- [ ] Käufer-Angaben vollständig (soweit bekannt)
- [ ] Rechnungsnummer eindeutig und lückenlos (→ gobd-audit)
- [ ] Leistungsdatum oder -zeitraum angegeben (BT-73/BT-74)
- [ ] Zahlungsziel und -kondition (BT-9, BT-20)

### Integration
- [ ] E-Rechnung als Anhang in Ausgangs-E-Mail (MIME type: `application/pdf`)
- [ ] Archivierung der ausgehenden Rechnung (GoBD: 10 Jahre)
- [ ] Eingehende E-Rechnungen (ZUGFeRD/XRechnung) können importiert werden

---

## Common Mistakes

| Fehler | Problem | Fix |
|--------|---------|-----|
| PDF ohne eingebettetes XML | Kein E-Rechnung-Standard | ZUGFeRD-Export mit Mustang |
| Falsche Profile | XRechnung ≠ EN16931 Comfort | `Profiles.XRECHNUNG` vs `Profiles.EN16931` |
| Fehlende Leitweg-ID bei B2G | Ablehnung durch PEPPOL/Pportal | Leitweg-ID vom Auftraggeber erfragen |
| Rundungsfehler | Validierungsfehler | BigDecimal, nie double für Beträge |
| Kein Validation-Gate in CI | Fehlerhafte Rechnungen in Produktion | KoSIT-Validator als CI-Schritt |
