# ADR-0002 — N-4-Kompatibilität über Parallel-Adapter statt Transformer-Chain

- **Status:** Akzeptiert
- **Datum:** 2026-06-09
- **Entscheider:** Tom (Volantic)
- **Betrifft:** §3.5, §9a Masterplan

## Kontext

Kern-Verkaufsversprechen: **„Deine Integrationen und Plugins brechen nie."** Der Kunde läuft auf dem
**neuesten Kernel** (Support nur N-1), nutzt aber **bis zu 4 Jahre alte** Drittintegrationen (REST)
und Plugins (SPI) weiter. Wir garantieren dafür **Rückwärtskompatibilität über N-4 Releases**.

Zentrale Unterscheidung (gegen ständige Verwechslung):
- **Support-Fenster** = für welche Version fixen wir Bugs / nehmen den Anruf an → **N-1**.
- **Kompatibilitäts-Fenster** = wie alter Kundencode noch *funktioniert* → **N-4**.
Diese sind orthogonal: Der Kunde ist auf dem neuesten Kernel (Support-Pflicht) und seine alte
Integration läuft trotzdem (Kompatibilität).

Zwei Bauformen standen zur Wahl, um einen v(n-4)-Request auf den aktuellen Kern zu bringen:

1. **Transformer-Chain (linear):** Pro Version ein kleines Delta (n-4 → n-3 → … → n und zurück). Man
   schreibt nur ein Delta je Release. Aber: Ein alter Request durchläuft **bis zu 8 Transformationen**
   (4 hoch, 4 zurück), jede mit Serialisierung/Mapping.
2. **Parallel-Adapter (ein Hop):** Je unterstützter Alt-Version *ein* vollständiger Adapter direkt auf
   den aktuellen Kern. Ein Request trifft **genau einen** Adapter.

Harte Randbedingung: **Performance-Budget sub-500 ms** (§9a) gilt auch für alte Clients unter Last.
Eine 8-stufige Transformationskette pro Request frisst genau dieses Budget.

## Entscheidung

Wir nutzen **Parallel-Adapter: ein direkter Adapter pro unterstützter Alt-Version, ein Hop pro
Request.** Gilt für **beide** Kompatibilitäts-Oberflächen:

- **REST-API** (Drittintegrationen): bis zu 4 Voll-Adapter parallel, die die Verträge N-4…N-1 auf den
  aktuellen Kern übersetzen.
- **SPI** (in-JVM-Plugins): **enges, stabiles SPI** mit Additive-Only-Policy als das funktionale
  Äquivalent — ein einmal veröffentlichter Extension Point bleibt N-4 lang lauffähig (nie verengt, nur
  additiv erweitert).

Die Versionsauflösung ist **automatisch** und wird von der *Integration bzw. dem Plugin* diktiert
(nicht per Mandanten-Schalter): zur Laufzeit wird der passende Adapter gewählt, die anderen bleiben
inaktiv.

## Konsequenzen

**Positiv**
- **Latenz-optimal:** genau ein Übersetzungs-Hop statt bis zu acht — verträglich mit dem
  sub-500-ms-Budget, auch für 4 Jahre alte Clients unter Last.
- Jeder Adapter ist isoliert testbar und unabhängig deploybar; ein defekter Alt-Adapter beeinflusst die
  anderen nicht.
- Das Versprechen „neuester Kernel + alte Plugins/Integrationen laufen weiter" wird ohne Support alter
  Kernel-Versionen eingelöst.

**Negativ / Kosten (bewusst akzeptiert)**
- **Bis zu 4 Voll-Adapter parallel pflegen.** Jede Breaking-Change im Kern schlägt potenziell in *alle*
  relevanten Adapter gleichzeitig durch — höherer Pflegeaufwand pro Release als bei der Chain (die nur
  *ein* Delta je Version verlangt). Für ein 3-Personen-Team eine reale Last → braucht Governance.
- Risiko des „Adapter-Erwürgens", wenn die Disziplin fehlt.

**Notwendige Gegenmaßnahmen (sonst kippt die Entscheidung ins Negative)**
- **Additive-Only-Policy:** Verträge (REST *und* SPI) nur additiv ändern, nie brechend, nie verengen.
  Reduziert die Zahl echter Breaking-Changes — und damit die Adapter-Last — drastisch.
- **Consumer-Driven Contract-Tests als CI-Gate:** Release baut nicht, wenn ein Vertrag aus N-4…N bricht.
- **API-Steward:** ein benannter Dev verantwortet REST- *und* SPI-Vertragsdisziplin. Das ist der
  Unterschied zwischen „N-4 funktioniert" und „N-4 erwürgt uns."
- **N-4 als harte Obergrenze:** der älteste Adapter (n-4) wird bei jedem Major-Release abgekündigt und
  fällt raus — die Adapter-Zahl bleibt bei ≤4 konstant, wächst nicht unbegrenzt.

## Verworfene Alternativen

- **Transformer-Chain (linear):** weniger Pflegeaufwand pro Release (nur ein Delta je Version), aber bis
  zu 8 Transformationen je Request → unvereinbar mit dem sub-500-ms-Budget für alte Clients. Der
  Latenz-Preis wiegt schwerer als der Pflege-Vorteil, weil Performance ein Kern-Verkaufsargument ist.
- **„Perpetual"-Kompatibilität (unbegrenzt alt):** unbezahlbare, unbegrenzt wachsende Adapter-Matrix.
  N-4 ist die bewusste, vertraglich kommunizierte Grenze („alle 4 Jahre einmal die Integration auf die
  aktuelle API heben, dann wieder 4 Jahre Ruhe").
- **Mächtige Plugin-Engine statt engem SPI:** würde beliebige Kern-Umbauten erlauben, zerstört aber das
  40-Min-Drop-In-Update-Versprechen (jedes Update bräche potenziell Kundencode) → siehe enges SPI, §3.3.
