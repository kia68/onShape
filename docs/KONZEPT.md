# OnShape — Konzept- und Anforderungsdokument

**Fitness- und Ernährungs-Web-App (DE/EN)**
Version 1.0 · Stand: 5. August 2026 · Autor: Kia

---

## Inhaltsverzeichnis

1. [Executive Summary](#1-executive-summary)
2. [Wettbewerbsanalyse](#2-wettbewerbsanalyse)
3. [Marktlücken und Positionierung](#3-marktlücken-und-positionierung)
4. [Zielgruppen und Personas](#4-zielgruppen-und-personas)
5. [Funktionale Anforderungen](#5-funktionale-anforderungen)
6. [Nicht-funktionale Anforderungen](#6-nicht-funktionale-anforderungen)
7. [Algorithmen im Detail](#7-algorithmen-im-detail)
8. [Datenmodell](#8-datenmodell)
9. [API-Design](#9-api-design)
10. [Tech-Stack und Architektur](#10-tech-stack-und-architektur)
11. [Datenquellen, Lizenzen und Kosten](#11-datenquellen-lizenzen-und-kosten)
12. [Bewegungsvermittlung für Anfänger](#12-bewegungsvermittlung-für-anfänger)
13. [Internationalisierung DE/EN](#13-internationalisierung-deen)
14. [Recht und Compliance](#14-recht-und-compliance)
15. [Geschäftsmodell und Preise](#15-geschäftsmodell-und-preise)
16. [Roadmap](#16-roadmap)
17. [Risiken](#17-risiken)
18. [Quellen](#18-quellen)

---

## 1. Executive Summary

### Das Problem

Der Markt für Kalorien- und Trainings-Tracking ist gesättigt, aber schlecht bedient. Die Nutzer verteilen sich auf zwei Lager, die beide unvollständig sind:

- **Ernährungs-Apps** (MyFitnessPal, Yazio, Cal AI, Lifesum) können Kalorien zählen, aber Training ist bei ihnen ein Anhängsel. Kein echter Trainingsplan, keine Progression, keine Übungsanleitung.
- **Trainings-Apps** (Hevy, Strong, Fitbod, Jefit) können Sätze protokollieren, aber Ernährung ist bei ihnen ein Anhängsel oder gar nicht vorhanden.

Wer beides will, betreibt zwei Abos und zwei Apps, die nichts voneinander wissen. Das ist absurd, weil Ernährung und Training physiologisch **ein einziges System** sind: Das Kalorienziel hängt vom Trainingsvolumen ab, die Regeneration hängt von der Proteinzufuhr ab, das Trainingsziel bestimmt die Makroverteilung.

Zusätzlich haben beide Lager ein Anfängerproblem: **Keine der großen Apps bringt einem bei, wie man die Übungen tatsächlich ausführt.** Hevy und Strong sind reine Logger. Fitbod hat Videos, aber gibt keine Fehlerkorrektur. Der Anfänger, der zum ersten Mal ins Studio geht, steht mit einer Liste von Übungsnamen da, die ihm nichts sagt.

### Die Lösung

**OnShape** ist eine Web-App (PWA), die vier Dinge in einem geschlossenen Regelkreis verbindet:

1. **Ernährungstracking** mit einer verifizierten, mehrsprachigen Lebensmitteldatenbank — Barcode-Scanner **kostenlos**, nicht hinter der Paywall.
2. **Kaufberatung am Regal** — der Barcode-Scanner sagt nicht nur „350 kcal", sondern „Passt das zu deinem heutigen Ziel? Gibt es eine bessere Alternative im selben Regal?"
3. **Individuelle Trainingsplan-Generierung** nach Geschlecht, Alter, Erfahrung, Equipment und Ziel (Muskelaufbau / Abnehmen / Zunehmen / Kraft / Gesundheit), mit evidenzbasierter Volumensteuerung und automatischer Progression.
4. **Bewegungsschule** — jede Übung mit Video aus zwei Perspektiven, Textanleitung, Cue-Liste, häufigen Fehlern, und optional einer kamerabasierten Formanalyse im Browser, die Anfängern in Echtzeit sagt, ob die Bewegung stimmt.

### Die fünf Differenzierer gegenüber MyFitnessPal und Cal AI

| # | Differenzierer | Warum Konkurrenz das nicht hat |
|---|---|---|
| 1 | **Barcode-Scanner kostenlos** | MFP hat ihn 2022 hinter Premium (79,99 $/Jahr) gesperrt — Beschwerde Nr. 1 |
| 2 | **Verifizierte Datenbank statt User-Chaos** | MFP/Yazio sind crowdsourced; „Banane" liefert dutzende widersprüchliche Einträge |
| 3 | **Training + Ernährung im gleichen Regelkreis** | MFP kann keine Trainingspläne, Hevy/Fitbod können keine Ernährung |
| 4 | **Bewegungsschule mit Formfeedback** | Keine Mainstream-App korrigiert die Ausführung |
| 5 | **DE/EN nativ, DSGVO-first, EU-Hosting** | MFP und Cal AI sind US-Produkte mit schwacher DE-Datenbank |

### Zielmetriken (12 Monate nach Launch)

| Metrik | Ziel |
|---|---|
| D30-Retention | ≥ 25 % (Branchenschnitt Fitness-Apps: ~8–12 %) |
| Free→Paid-Conversion | ≥ 4 % |
| Logging-Zeit pro Mahlzeit | ≤ 15 Sekunden (MFP nach Redesign: bis 5 Min/Tag) |
| Datenbank-Trefferquote DE-Produkte | ≥ 90 % bei Barcode-Scan |
| Preis | 3,99 €/Monat bzw. 29,99 €/Jahr — deutlich unter MFP (79,99 $) |

---

## 2. Wettbewerbsanalyse

### 2.1 Übersicht

| App | Fokus | Preis (2026) | Größte Stärke | Größte Schwäche |
|---|---|---|---|---|
| **MyFitnessPal** | Ernährung | Free / 79,99 $ Premium / 99,99 $ Premium+ | Größte Datenbank, Marken-Bekanntheit, Integrationen | Barcode hinter Paywall, 5-Einträge-Limit im Free-Tier, Datenqualität, Redesign-Debakel |
| **Cal AI** | Ernährung (Foto-KI) | ~2,99 $/Woche bis ~29,99 $/Jahr, dynamische Preise | Foto-Logging ist schnell und macht Spaß | Kleine Datenbank, keine Mikronährstoffe, Genauigkeit bricht bei komplexen Gerichten ein, versteckte Preise |
| **Yazio** | Ernährung (DACH-Marktführer) | 36–84 €/Jahr | Deutsche Datenbank, Fasten-Tracker, Rezepte | Makro-Tracking hinter Premium, Dateninkonsistenzen |
| **Lifesum** | Ernährung + Pläne | ~50 €/Jahr | Schönes UI, Ernährungsprogramme | Free-Tier praktisch unbrauchbar (Demo-Charakter) |
| **Cronometer** | Mikronährstoffe | Free / ~50 $/Jahr | 80+ Nährstoffe, kuratierte USDA-Daten | Nerdig, schlechte DE-Abdeckung, kein Training |
| **FDDB** | Ernährung (DE) | Free / Premium | Kostenlose deutsche Referenzdatenbank | Veraltetes UI, schwach als aktiver Tracker |
| **Hevy** | Training | Free / 2,99 $/Mon., 23,99 $/Jahr, 74,99 $ Lifetime | Bestes Preis-Leistungs-Verhältnis, gutes Free-Tier, Social Feed | Reiner Logger, keine Ernährung, keine Anleitung |
| **Strong** | Training | Free / ~30 $/Jahr | Minimalistisch, extrem schnell | Nur Logging, keine Programmierung, keine Anleitung |
| **Fitbod** | Training (KI) | 15,99 $/Mon. bzw. 95,99 $/Jahr | Adaptive Planerstellung, Übungsvideos | Nur Kraft (kein Cardio-Bewusstsein), wechselnde Workouts erschweren Progressionsverfolgung, teuer |
| **Jefit** | Training | Free / ~70 $/Jahr | Große Übungsbibliothek | Überladenes UI |

### 2.2 MyFitnessPal — die Detailanalyse

**Stärken (was wir übernehmen müssen)**

- Größte Lebensmitteldatenbank am Markt. Die schiere Abdeckung ist der Grund, warum Nutzer trotz aller Beschwerden bleiben.
- Sehr breites Integrationsnetz (Garmin, Fitbit, Apple Health, Strava, Withings …).
- Rezept-Import per URL — unterschätzt und sehr beliebt.
- „Meal"-Konzept: Mehrere Lebensmittel als wiederverwendbare Einheit speichern.

**Schwächen (unsere Angriffspunkte)**

| Schwäche | Detail | Unsere Antwort |
|---|---|---|
| **Barcode-Paywall** | 2022 aus dem Free-Tier entfernt. Meistgenannte Beschwerde 2026. | Barcode für immer kostenlos. Wir machen es zum Marketing-Argument. |
| **5-Einträge-Limit** | Free-Nutzer können nur 5 Lebensmittel/Tag loggen. Nutzer, die jahrelang gratis trackten, stehen plötzlich vor der Paywall. | Unbegrenztes Logging im Free-Tier. |
| **Datenqualität** | Datenbank fast komplett user-generiert. Suche nach „Banane" liefert Dutzende Einträge mit unterschiedlichen Kalorienwerten. In Tests: 71,8 % Trefferquote bei Gerichten, ±14,4 % Abweichung bei Portionsgrößen. | Kuratierte Basis (BLS 4.0 + USDA) + verifizierte Marken-Layer + community Layer klar getrennt und farblich gekennzeichnet. |
| **Redesign 2025/26** | Nutzer berichten von 6–10 Taps für einen Workflow, der früher 2–3 brauchte. Tages-Logging von 90 Sekunden auf 5 Minuten. Kalorien pro Mahlzeit nicht mehr auf einen Blick sichtbar. Multi-Select und Copy-Meal entfernt. | **Speed-First-Design**: Quick-Add ≤ 3 Taps, Multi-Select, „Gestern kopieren", Tagesansicht mit Mahlzeit-Kalorien sofort sichtbar. |
| **Werbung** | Aggressive Banner + Vollbild-Interstitials zwischen Aktionen im Free-Tier. | **Keine Werbung. Niemals.** Rein abo-finanziert. |
| **Abrechnung/Support** | Berichte über unautorisierte Abbuchungen, Weiterbelastung nach Kündigung, Erstattungen abgelehnt, Telefon-Support tot. | Kündigung mit einem Klick in der App, transparente Rechnungshistorie, EU-Verbraucherrecht sauber umgesetzt. |

### 2.3 Cal AI — die Detailanalyse

**Stärken**

- Foto-Logging senkt die Einstiegshürde drastisch. Das ist der eigentliche Innovationssprung der letzten Jahre.
- Bei einfachen Einzelgerichten liegen die Schätzungen innerhalb von 10–15 % der verifizierten Werte; bei häufigen Lebensmitteln werden 90–95 % Trefferquote berichtet.
- Sehr gutes Onboarding-Funnel-Design.

**Schwächen**

| Schwäche | Detail | Unsere Antwort |
|---|---|---|
| **Fehlerakkumulation** | Bei 15 % Abweichung pro Mahlzeit × 3 Mahlzeiten kann der Tagesfehler 300+ kcal betragen. Für ein präzises Defizit unbrauchbar. | Foto-KI liefert **immer** ein Konfidenzintervall statt einer Scheinpräzision. Bei niedriger Konfidenz wird aktiv nachgefragt („Wie viel Reis? ½ / 1 / 1½ Tassen"). |
| **Blackbox bei komplexen Gerichten** | Cal AI blendet die Zutatenaufschlüsselung aus, wenn die KI unsicher ist. | Zutatenaufschlüsselung immer sichtbar und **editierbar**. Der Nutzer korrigiert einzelne Zutaten, nicht das Gesamtergebnis. |
| **Kleine Datenbank** | Deutlich kleiner als etablierte Konkurrenz — schwach bei Markenprodukten, Restaurantgerichten und regionalen Speisen. | Foto-KI ist bei uns ein *Eingabeweg* in eine große Datenbank, nicht ein Ersatz für sie. |
| **Keine Mikronährstoffe** | Nur Kalorien + Makros. Vitamine, Mineralstoffe, Ballaststoffe fehlen weitgehend. | Mikronährstoffe ab Tag 1 (BLS liefert sie ohnehin mit). |
| **Versteckte, dynamische Preise** | Preis erst nach Download und Onboarding-Quiz sichtbar; variiert nach Standort, Gerät und Quiz-Antworten. | Preise öffentlich auf der Website, ein Preis für alle. |
| **Kein Training** | Reines Ernährungsprodukt. | Training ist bei uns gleichwertig. |

### 2.4 Trainings-Apps — die Detailanalyse

**Hevy** — der Preis-Leistungs-König. Bestes Free-Tier, Lifetime-Option, Social Feed. Aber: reiner Logger. Er sagt dir nicht, *was* du trainieren sollst, und erklärt dir nicht, *wie*.

**Strong** — minimalistisch und schnell, genau deshalb beliebt. Aber keine Programmierung, keine Anleitung, kaum Analytics.

**Fitbod** — der einzige echte Algorithmus-Planer. Aber zwei harte Schwächen:
- **Nur Kraft.** Läufst du oder machst Konditionstraining, hat die App keinerlei Bewusstsein für diese Trainingsbelastung. Das ist bei einem Recovery-basierten Algorithmus ein Konstruktionsfehler.
- **Ständig wechselnde Workouts** erschweren das Verfolgen der progressiven Überlastung bei einzelnen Übungen — das genaue Gegenteil dessen, was Kraftaufbau braucht.
- Mit 95,99 $/Jahr die teuerste Option.

**Gemeinsame Schwäche aller Trainings-Apps:** Keine korrigiert die Bewegungsausführung in Echtzeit. Für Anfänger — die größte und am schnellsten wachsende Nutzergruppe — ist genau das der kritische Punkt.

---

## 3. Marktlücken und Positionierung

### 3.1 Die sechs Lücken

**Lücke 1 — Der Integrations-Graben.**
Niemand macht Ernährung *und* Training auf gleichem Qualitätsniveau. Das ist die größte und offensichtlichste Lücke. Und sie ist nicht nur ein UX-Thema: Wer beide Datenströme hat, kann Dinge berechnen, die einzeln unmöglich sind (adaptives TDEE aus Gewichtsverlauf × Trainingsvolumen, Protein-Bedarf aus tatsächlichem Trainingsvolumen, Deload-Empfehlung bei zu langem Defizit).

**Lücke 2 — Datenqualität vs. Datenmenge.**
MFP hat Menge ohne Qualität, Cronometer Qualität ohne Menge/Komfort. Der Sweet Spot — große, *kuratierte*, mehrsprachige Datenbank mit klarer Vertrauenskennzeichnung — ist unbesetzt.

**Lücke 3 — Anfänger-Onboarding im Kraftsport.**
Alle Trainings-Apps setzen voraus, dass man weiß, was ein „Romanian Deadlift" ist. Der Markt der absoluten Anfänger ist riesig und wird von niemandem ernsthaft bedient.

**Lücke 4 — Kaufentscheidung am Regal.**
Alle Barcode-Scanner beantworten „Was ist da drin?". Keiner beantwortet „**Soll ich das kaufen?**". Genau das ist aber die Frage, die der Nutzer im Supermarkt tatsächlich hat.

**Lücke 5 — Der DACH-Markt.**
MFP und Cal AI sind US-Produkte mit schwacher deutscher Produktabdeckung. Yazio ist Marktführer, hat aber Datenqualitätsprobleme und sperrt Makros hinter Premium. Der **BLS 4.0 ist seit kurzem lizenzfrei** — ein struktureller Vorteil, den bisher kaum jemand ausnutzt.

**Lücke 6 — Preis-Vertrauen.**
MFP verlangt 79,99 $/Jahr und hat Abrechnungs-Reputationsprobleme. Cal AI versteckt seine Preise. Ein transparenter, fairer, deutlich günstigerer Preis ist ein echtes Verkaufsargument.

### 3.2 Positionierung

> **OnShape ist die eine App für Training und Ernährung — mit einer Datenbank, der man vertrauen kann, einem Trainingsplan, der zu dir passt, und einem Coach, der dir zeigt, wie die Bewegung geht.**

**Positionierungs-Matrix**

```
                Ernährung stark
                       ▲
                       │
        MyFitnessPal   │   Cronometer
        Yazio, Cal AI  │
                       │
   Training  ──────────┼──────────►  Training
   schwach             │              stark
                       │        ★ OnShape
                       │
                       │   Hevy, Strong
                       │   Fitbod, Jefit
                       ▼
                Ernährung schwach
```

### 3.3 Nicht-Ziele (bewusste Abgrenzung)

Was OnShape ausdrücklich **nicht** ist:

- Kein Medizinprodukt. Keine Diagnose, keine Therapie, keine Behandlung von Krankheiten (siehe [§14](#14-recht-und-compliance)).
- Keine Essstörungs-Plattform. Kein Zugang unter 16 Jahren, keine BMI-Ziele unter 18,5, keine Gewichtsverlust-Raten über 1 % Körpergewicht/Woche, keine „Gamification" von Restriktion, kein öffentliches Kalorien-Leaderboard.
- Keine Social-Media-App. Community optional, nie zentral, keine Bilder-Feeds mit Körpervergleich.
- Kein Supplement-Shop. Keine Affiliate-Empfehlungen für Nahrungsergänzung.

---

## 4. Zielgruppen und Personas

### Persona 1 — Lisa, 26, absolute Anfängerin (Primärzielgruppe)

Will abnehmen, war noch nie im Fitnessstudio, hat Angst, sich zu blamieren. Hat MFP probiert und nach zwei Wochen aufgegeben, weil das Logging zu lange dauerte und der Barcode-Scanner Geld kostete.

**Braucht:** Extrem einfaches Logging, einen Plan der sagt „Montag machst du genau das", Videos die zeigen wie es geht, und das Gefühl, nichts falsch zu machen.
**Erfolgskriterium:** Sie geht nach 4 Wochen noch ins Studio.

### Persona 2 — Marco, 32, Fortgeschrittener (Sekundärzielgruppe)

Trainiert seit 3 Jahren, nutzt Hevy + MFP parallel, zahlt zwei Abos. Will einen sauberen Aufbau (Lean Bulk), kennt RIR und Periodisierung.

**Braucht:** Präzise Daten, eigene Programme importierbar, Volumen-Analytics pro Muskelgruppe, adaptives Kalorienziel, Export.
**Erfolgskriterium:** Er kündigt beide anderen Abos.

### Persona 3 — Sabine, 48, Wiedereinsteigerin

Nach Jahren Pause zurück, gesundheitsorientiert, will Kraft und Knochendichte erhalten, nicht „Bodybuilding". Perimenopause-Thematik.

**Braucht:** Altersgerechte Volumen- und Intensitätssteuerung, Gelenkschonung, Proteinfokus, kein Jugend-Marketing.
**Erfolgskriterium:** Sie fühlt sich angesprochen und nicht wie eine Randgruppe.

### Persona 4 — Tarek, 21, will zunehmen

Hardgainer, 68 kg bei 1,85 m, isst nach eigener Einschätzung „viel", nimmt nicht zu.

**Braucht:** Kalorienüberschuss-Steuerung mit realistischen Raten, kalorienreiche Lebensmittelvorschläge, Aufbau-Programm, sichtbaren Fortschritt bei Kraftwerten.
**Erfolgskriterium:** +4 kg in 6 Monaten bei kontrolliertem Körperfett.

---

## 5. Funktionale Anforderungen

Legende: **[MVP]** = Muss zum Launch · **[V1]** = 3–6 Monate danach · **[V2]** = später

### 5.1 Onboarding und Profil

| ID | Anforderung | Prio |
|---|---|---|
| FR-01 | Registrierung per E-Mail/Passwort, Apple, Google. Passkeys als Option. | MVP |
| FR-02 | Erfassung: Geschlecht (m/w/divers/keine Angabe), Geburtsdatum, Größe, Gewicht, Aktivitätslevel (Beruf + Alltag), Trainingserfahrung (nie / <6 Mon. / 6–24 Mon. / >2 Jahre). | MVP |
| FR-03 | Zielauswahl: Abnehmen · Muskelaufbau · Zunehmen · Kraft · Gesundheit/Erhaltung · Recomp. | MVP |
| FR-04 | Zielrate wählbar mit **medizinischen Grenzen**: Abnehmen 0,25–1,0 % KG/Woche, Zunehmen 0,125–0,5 % KG/Woche. Schnellere Raten sind blockiert, nicht nur gewarnt. | MVP |
| FR-05 | Equipment-Erfassung: Fitnessstudio (voll) · Home-Gym (Auswahlliste: Kurzhanteln, Langhantel, Bänder, Klimmzugstange, Kettlebell …) · Nur Körpergewicht. | MVP |
| FR-06 | Verfügbarkeit: Trainingstage/Woche (2–6), Minuten pro Einheit (20–120). | MVP |
| FR-07 | Gesundheits-Screening (PAR-Q+ Kurzform): Herzprobleme, Schwangerschaft, akute Verletzungen, Medikamente. Bei Treffern → Hinweis auf ärztliche Rücksprache, kein Ausschluss. | MVP |
| FR-08 | Einschränkungen/Verletzungen (Knie, Schulter, unterer Rücken, Handgelenk, Hüfte) → Übungsfilter im Plangenerator. | MVP |
| FR-09 | Ernährungspräferenzen: omnivor, vegetarisch, vegan, pescetarisch, halal, koscher; Allergene (14 EU-Allergene). | MVP |
| FR-10 | Onboarding ≤ 90 Sekunden. Fortschrittsbalken. Jederzeit abbrechbar mit Defaults. | MVP |
| FR-11 | Ergebnis-Screen: „Dein Tagesziel: 2.140 kcal · 165 g Protein · 60 g Fett · 235 g Kohlenhydrate" **mit Erklärung, wie es berechnet wurde** (aufklappbar). | MVP |

> **Designprinzip:** Jede berechnete Zahl in der App ist antippbar und erklärt sich selbst. Kein Blackbox-Algorithmus. Das ist gleichzeitig ein Vertrauens-Feature und eine Compliance-Anforderung aus dem EU AI Act.

### 5.2 Ernährungstracking

| ID | Anforderung | Prio |
|---|---|---|
| FR-20 | Tagesansicht mit Kalorien, Makros (P/F/K) und Rest-Budget **oberhalb der Falz**. Kalorien pro Mahlzeit direkt sichtbar. | MVP |
| FR-21 | Mahlzeiten-Slots: Frühstück, Mittag, Abend, Snacks — vom Nutzer umbenennbar und erweiterbar. | MVP |
| FR-22 | **Quick-Add ≤ 3 Taps**: Suche → Ergebnis antippen → Menge bestätigen. Portionsgröße vorbelegt mit der zuletzt genutzten Menge dieses Nutzers. | MVP |
| FR-23 | Multi-Select in der Suche: mehrere Lebensmittel gleichzeitig auswählen und in einem Zug loggen. | MVP |
| FR-24 | „Gestern kopieren" / „Letzte Woche Montag kopieren" / einzelne Mahlzeit kopieren. | MVP |
| FR-25 | Eigene Mahlzeiten („Meals") speichern: Mehrere Lebensmittel als eine benannte Einheit. | MVP |
| FR-26 | Eigene Rezepte: Zutaten + Portionsanzahl → Nährwerte pro Portion. Skalierbar. | MVP |
| FR-27 | Rezept-Import per URL (JSON-LD `Recipe` Schema parsen, Fallback auf LLM-Extraktion). | V1 |
| FR-28 | Mikronährstoff-Tracking: mindestens Ballaststoffe, Zucker, gesättigte Fette, Salz, Kalium, Kalzium, Eisen, Magnesium, Zink, Vitamin D, B12, C, Folat. | MVP |
| FR-29 | Wasser-Tracking mit Tagesziel. | MVP |
| FR-30 | Gewicht, Körpermaße (Taille, Hüfte, Brust, Arm, Oberschenkel), Körperfett (optional). | MVP |
| FR-31 | Offline-Logging: Einträge werden lokal gespeichert und synchronisieren bei Verbindung. | MVP |
| FR-32 | **Unbegrenztes Logging im Free-Tier.** Kein Eintragslimit. | MVP |
| FR-33 | Natürlichsprachige Eingabe: „2 Eier, eine Scheibe Vollkornbrot und einen Kaffee mit Milch" → strukturierte Einträge. | V1 |
| FR-34 | Nährwertetikett fotografieren → OCR → Eintrag. Für Produkte, die nicht in der DB sind. | V1 |

### 5.3 Barcode-Scanner und Kaufberatung

Das ist unser sichtbarstes Differenzierungsmerkmal. Es beantwortet die Frage, die MFP und Cal AI ignorieren: **„Lohnt sich das für mich?"**

| ID | Anforderung | Prio |
|---|---|---|
| FR-40 | Barcode-Scan (EAN-8/13, UPC-A/E) über die Kamera direkt im Browser via `BarcodeDetector` API, Fallback `zxing-wasm`. | MVP |
| FR-41 | **Kostenlos, unbegrenzt, im Free-Tier.** Kein Limit, keine Paywall, nie. | MVP |
| FR-42 | Scan-Ergebnis in < 1,5 Sekunden. Lokaler Cache für bereits gescannte Produkte. | MVP |
| FR-43 | **Fit-Score (0–100)**: Wie gut passt dieses Produkt zu *diesem* Nutzer heute? Details in [§7.6](#76-fit-score--lohnt-sich-der-kauf). | MVP |
| FR-44 | Ampel-Darstellung mit Klartext-Begründung: „Passt gut — 22 g Protein pro Portion, du liegst heute 60 g unter deinem Proteinziel." bzw. „Eher nicht — 34 g Zucker pro Portion, das wären 68 % deines Tages-Zuckerbudgets." | MVP |
| FR-45 | **Bessere Alternative**: Zeigt bis zu 3 Produkte derselben Kategorie mit besserem Fit-Score. Bevorzugt Produkte, die in deutschen Supermärkten verfügbar sind. | MVP |
| FR-46 | Preis-pro-Protein / Preis-pro-Kalorie, wo Preisdaten verfügbar sind — „Lohnt sich der Einkauf" auch ökonomisch. | V2 |
| FR-47 | Nutri-Score, NOVA-Klassifizierung und Zusatzstoffliste anzeigen (aus Open Food Facts vorhanden). | MVP |
| FR-48 | Allergen-Warnung basierend auf dem Nutzerprofil — prominent, rot, vor allem anderen. | MVP |
| FR-49 | Produkt nicht gefunden → Nutzer kann es mit Foto des Etiketts + OCR anlegen; Beitrag geht (mit Einwilligung) an Open Food Facts zurück. | MVP |
| FR-50 | Einkaufslisten-Modus: Mehrere Produkte hintereinander scannen, am Ende Gesamtübersicht („Dieser Einkauf deckt 3 Tage Protein"). | V1 |
| FR-51 | Regal-Vergleichsmodus: Zwei Produkte nacheinander scannen → direkter Seite-an-Seite-Vergleich. | V1 |

### 5.4 KI-Fotoerkennung

| ID | Anforderung | Prio |
|---|---|---|
| FR-60 | Mahlzeitenfoto → erkannte Zutaten mit geschätzter Menge. | V1 |
| FR-61 | **Immer Konfidenzintervall statt Scheinpräzision**: „540–680 kcal (mittlere Sicherheit)". Kein einzelner Wert. | V1 |
| FR-62 | Zutatenaufschlüsselung immer sichtbar und pro Zutat editierbar. Nie ausblenden. | V1 |
| FR-63 | Bei niedriger Konfidenz aktive Rückfrage mit visuellen Portionsgrößen („Wie viel Reis?" mit drei Fotos). | V1 |
| FR-64 | Referenzobjekt-Erkennung (Hand, Besteck, Standardteller) zur Größenkalibrierung. | V2 |
| FR-65 | Erkannte Zutaten werden gegen die eigene Datenbank gematcht, nicht frei halluziniert. | V1 |
| FR-66 | Bilder werden nach der Verarbeitung gelöscht, sofern der Nutzer sie nicht ausdrücklich speichert. | V1 |

### 5.5 Trainingsplan-Generierung

| ID | Anforderung | Prio |
|---|---|---|
| FR-70 | Automatische Plangenerierung aus Profil: Ziel, Erfahrung, Tage/Woche, Zeit/Einheit, Equipment, Verletzungen, Alter, Geschlecht. Algorithmus in [§7.4](#74-trainingsplan-generator). | MVP |
| FR-71 | Split-Auswahl automatisch: Ganzkörper (2–3 Tage), Oberkörper/Unterkörper (4 Tage), Push/Pull/Legs (5–6 Tage). Manuell überschreibbar. | MVP |
| FR-72 | Mesozyklus-Struktur: 4–6 Wochen Aufbau mit steigendem Volumen, dann 1 Woche Deload. | MVP |
| FR-73 | Jede Einheit mit Übungen, Sätzen, Wiederholungsbereich, Ziel-RIR und Pausenzeiten. | MVP |
| FR-74 | Übungstausch: Jede Übung gegen eine Alternative mit gleichem Zielmuskel und verfügbarem Equipment tauschbar. Grund abfragen („zu schwer" / „Gerät belegt" / „Schmerzen") → fließt ins Nutzermodell ein. | MVP |
| FR-75 | Manuelle Plan-Erstellung und -Bearbeitung für Fortgeschrittene. | MVP |
| FR-76 | Import etablierter Programme als Vorlagen (5/3/1, Starting Strength, PHUL, GZCLP, nSuns …) — Vorlagen sind Eigenimplementierungen der Struktur, keine Kopie geschützter Inhalte. | V1 |
| FR-77 | Volumen-Dashboard: Sätze pro Muskelgruppe pro Woche, mit Zielkorridor visualisiert. | MVP |
| FR-78 | Cardio-/Konditionstraining wird im Belastungsmodell mitgezählt (Fitbods großer Fehler). | MVP |
| FR-79 | Automatische Deload-Empfehlung bei: 3 Wochen stagnierender Leistung, wiederholt verfehltem RIR-Ziel, subjektiv hoher Erschöpfung, oder >8 Wochen im Kaloriendefizit. | V1 |

### 5.6 Trainings-Logging

| ID | Anforderung | Prio |
|---|---|---|
| FR-90 | Live-Workout-Modus: aktuelle Übung groß, letztes Mal danebengeschrieben, Satz eintragen in 2 Taps. | MVP |
| FR-91 | Automatisch vorbelegte Werte aus der letzten Einheit + Progressionsvorschlag. | MVP |
| FR-92 | Pausentimer mit Ton/Vibration, automatisch startend nach Satzeintrag, Dauer aus dem Plan. | MVP |
| FR-93 | RIR-/RPE-Erfassung pro Satz (optional, für Fortgeschrittene, im Anfängermodus ausgeblendet). | MVP |
| FR-94 | Warm-up-Satz-Rechner: aus dem Arbeitsgewicht automatisch 2–3 Aufwärmsätze. | V1 |
| FR-95 | Supersätze, Dropsätze, Cluster-Sätze. | V1 |
| FR-96 | Bildschirm bleibt an während des Workouts (Wake Lock API). Volle Offline-Fähigkeit. | MVP |
| FR-97 | 1RM-Schätzung (Epley + Brzycki, gemittelt) und Verlaufskurve pro Übung. | MVP |
| FR-98 | Persönliche Rekorde automatisch erkennen und feiern (Gewicht, Wiederholungen, geschätztes 1RM, Volumen). | MVP |

### 5.7 Bewegungsvermittlung (Kernfeature)

Siehe [§12](#12-bewegungsvermittlung-für-anfänger) für die inhaltliche Ausarbeitung.

| ID | Anforderung | Prio |
|---|---|---|
| FR-110 | Jede Übung hat: Video/Animation aus **zwei Perspektiven** (Front + Seite), Textanleitung in Schritten, 3–5 Cues, Liste häufiger Fehler mit Bild, Zielmuskulatur visualisiert. | MVP |
| FR-111 | Anfängermodus: Vor der ersten Ausführung einer Übung wird die Anleitung **automatisch** eingeblendet, nicht versteckt hinter einem Info-Icon. | MVP |
| FR-112 | „Erste-Mal-Checkliste": Kurzes Quiz nach der Anleitung („Wo sollten die Ellbogen zeigen?") — sichert, dass die Kernpunkte angekommen sind. | V1 |
| FR-113 | Alle Videos mit deutschen und englischen Untertiteln, ohne Ton verständlich (Studioumgebung ist laut). | MVP |
| FR-114 | Progressionsleiter pro Bewegungsmuster: Kniebeuge → Box Squat → Goblet Squat → Front Squat → Back Squat. Anfänger startet auf der passenden Stufe. | V1 |
| FR-115 | **Kamerabasierte Formanalyse** im Browser (MediaPipe Pose via WASM, rein clientseitig): Gelenkwinkel-Analyse, Wiederholungszählung, Live-Feedback bei definierten Fehlern (Knievalgus, Rundrücken, unvollständige Tiefe). | V2 |
| FR-116 | Formanalyse läuft **ausschließlich lokal**. Kein Videostream verlässt das Gerät. Muss explizit gestartet werden. | V2 |
| FR-117 | Selbstaufnahme-Modus: Nutzer filmt sich, App legt Gelenkspur über das Video, Nutzer sieht seine Bahn vs. Referenzbahn. | V2 |
| FR-118 | Studio-Guide: „Wie stelle ich die Bank ein", „Wie lege ich die Scheiben auf", „Wie frage ich nach einem Gerät" — die unausgesprochenen Hürden für Anfänger. | V1 |

### 5.8 Fortschritt und Auswertung

| ID | Anforderung | Prio |
|---|---|---|
| FR-130 | Gewichtsverlauf mit gleitendem 7-Tage-Mittel (nicht die Rohwerte prominent — Wassereinlagerung demotiviert). | MVP |
| FR-131 | Kalorien-/Makro-Verlauf, Wochendurchschnitte, Adhärenz-Quote. | MVP |
| FR-132 | Kraftverlauf pro Übung, geschätztes 1RM über Zeit. | MVP |
| FR-133 | Volumen pro Muskelgruppe pro Woche mit Zielkorridor. | MVP |
| FR-134 | **Adaptives TDEE**: Reale Kalorienverbrennung aus Gewichtsverlauf × Kalorienzufuhr rückgerechnet, nach 14 Tagen Daten. Details in [§7.1](#71-energiebedarf). | V1 |
| FR-135 | Wöchentlicher Bericht: was lief gut, was nicht, eine konkrete Empfehlung. | V1 |
| FR-136 | Fotos zur Fortschrittsdokumentation, lokal verschlüsselt, standardmäßig nicht in der Cloud. | V1 |
| FR-137 | Datenexport als CSV und JSON, vollständig, kostenlos, im Free-Tier (DSGVO Art. 20). | MVP |

### 5.9 Integrationen

| ID | Anforderung | Prio |
|---|---|---|
| FR-150 | Apple Health / Google Fit: Schritte, aktive Kalorien, Gewicht, Herzfrequenz (bidirektional). | V1 |
| FR-151 | Garmin, Fitbit, Withings, Polar. | V1 |
| FR-152 | Strava-Import für Ausdaueraktivitäten. | V2 |
| FR-153 | Import aus MyFitnessPal, Yazio, Hevy, Strong (CSV) — **Wechselhürde senken ist ein Akquisekanal**. | MVP |

---

## 6. Nicht-funktionale Anforderungen

| ID | Kategorie | Anforderung |
|---|---|---|
| NFR-01 | Performance | Largest Contentful Paint < 1,8 s auf 4G-Mittelklassegerät. Interaction to Next Paint < 200 ms. |
| NFR-02 | Performance | Lebensmittelsuche liefert erste Ergebnisse in < 150 ms (p95). |
| NFR-03 | Performance | Barcode-Scan bis Ergebnis < 1,5 s (p95). |
| NFR-04 | Offline | Vollständiges Logging (Essen + Training) offline. Sync bei Verbindung, konfliktfrei. |
| NFR-05 | Verfügbarkeit | 99,9 % monatlich. |
| NFR-06 | Barrierefreiheit | WCAG 2.2 AA. Tastaturbedienbar. Screenreader-getestet. Kontrast ≥ 4,5:1. Nicht farbcodierte Information allein. |
| NFR-07 | Mobile | Mobile-first, PWA-installierbar, Touch-Ziele ≥ 44 px, einhändig bedienbar (wichtige Aktionen im unteren Drittel). |
| NFR-08 | Sicherheit | TLS 1.3, Verschlüsselung ruhender Daten, Argon2id für Passwörter, Rate-Limiting, OWASP Top 10 abgedeckt. |
| NFR-09 | Datenschutz | Hosting ausschließlich in der EU (Frankfurt). Kein Drittland-Transfer ohne Rechtsgrundlage. |
| NFR-10 | Skalierung | 100.000 MAU ohne Architekturänderung. |
| NFR-11 | i18n | Vollständige DE/EN-Parität ab Tag 1. Alle Einheiten (kg/lb, cm/ft) und Datumsformate lokalisiert. |
| NFR-12 | Beobachtbarkeit | Strukturierte Logs, Traces, Fehler-Tracking, Produkt-Analytics — **selbst gehostet oder EU-basiert** (kein Google Analytics). |
| NFR-13 | Testabdeckung | Algorithmus-Kernmodule (Kalorien, Makros, Progression, Fit-Score) 100 % Unit-Test-Abdeckung mit Referenzwerten. |
| NFR-14 | Datenqualität | Jeder Lebensmitteleintrag hat eine Quellenangabe und ein Vertrauenslevel, sichtbar für den Nutzer. |

---

## 7. Algorithmen im Detail

> **Wissenschaftliche Grundlage.** Alle folgenden Formeln und Korridore sind mit Primärliteratur belegt (Quellen in [§18](#18-quellen)). Wo die Evidenz unsicher ist, ist das ausdrücklich vermerkt — die App soll keine Scheinpräzision vortäuschen.

### 7.1 Energiebedarf

#### Schritt 1 — Grundumsatz (BMR)

**Standardfall: Mifflin-St Jeor.** Der systematische Review von Frankenfield et al. (740 Zitationen) identifiziert Mifflin-St Jeor als die zuverlässigste der gängigen Gleichungen — sie schätzt den Ruheumsatz bei mehr Personen innerhalb von ±10 % des gemessenen Werts als Harris-Benedict, Owen oder WHO/FAO/UNU, und hat die schmalste Fehlerbreite [1]. Bei stark adipösen Personen bestätigt sich das in einer Kohorte von 4.247 Patienten [2].

```
Männer:  BMR = 10 × Gewicht(kg) + 6,25 × Größe(cm) − 5 × Alter + 5
Frauen:  BMR = 10 × Gewicht(kg) + 6,25 × Größe(cm) − 5 × Alter − 161
```

Für „divers"/„keine Angabe" wird der Mittelwert beider Formeln verwendet, mit Hinweis auf die geringere Genauigkeit.

**Wenn Körperfettanteil bekannt: Katch-McArdle.** Bei bekanntem KFA ist die Fettfreie-Masse-basierte Schätzung genauer, insbesondere bei sehr muskulösen oder sehr schlanken Personen:

```
FFM = Gewicht(kg) × (1 − KFA)
BMR = 370 + 21,6 × FFM
```

**Wichtige Einschränkung, die die App kommunizieren muss:** Alle Prädiktionsgleichungen haben erhebliche individuelle Fehler. Selbst Mifflin-St Jeor trifft im Einzelfall oft nur 43–64 % der Personen innerhalb von ±10 % [1][3][4]. Die Limits of Agreement liegen typischerweise bei ±300 kcal/Tag [5]. **Deshalb ist der Startwert nur eine Hypothese**, die durch das adaptive TDEE (Schritt 4) korrigiert wird. Die App sagt das dem Nutzer explizit.

#### Schritt 2 — Aktivitätsfaktor (TDEE)

Statt des groben klassischen PAL-Faktors verwenden wir eine zweiteilige Schätzung, weil das Trainingsvolumen ohnehin bekannt ist:

```
TDEE_basis = BMR × PAL_alltag
```

| Alltagsaktivität (ohne Sport) | PAL |
|---|---|
| Sitzend, wenig Bewegung (Bürojob, <5.000 Schritte) | 1,25 |
| Leicht aktiv (5.000–8.000 Schritte) | 1,40 |
| Aktiv (8.000–12.000 Schritte, stehender Beruf) | 1,55 |
| Sehr aktiv (>12.000 Schritte, körperliche Arbeit) | 1,75 |

Trainingskalorien werden **separat** addiert und aus dem tatsächlich geloggten Training berechnet:

```
kcal_training/Woche = Σ (MET_übung × 3,5 × Gewicht(kg) / 200 × Dauer_min)
TDEE = TDEE_basis + kcal_training/Woche / 7
```

Das ist präziser als ein pauschaler Faktor und macht das System reaktiv: Wer eine Woche nicht trainiert, bekommt automatisch ein niedrigeres Ziel.

> **Warum kein „Kalorien zurückverdienen"?** MFP schreibt Trainingskalorien dem Tagesbudget gut, was zu Doppelzählung und Überessen führt. Wir mitteln über die Woche, und die App zeigt das Training nicht als „Guthaben".

#### Schritt 3 — Zielanpassung

| Ziel | Anpassung | Grenzen |
|---|---|---|
| Abnehmen | −0,25 bis −1,0 % KG/Woche → Defizit = KG × %Rate × 7.700 / 7 | Nie unter BMR × 1,1. Nie unter 1.200 kcal (♀) / 1.500 kcal (♂). |
| Muskelaufbau (trainiert) | +5 bis +10 % über TDEE | Max. +0,25 % KG/Woche bei Fortgeschrittenen |
| Zunehmen (Anfänger/Untergewicht) | +10 bis +20 % über TDEE | Max. +0,5 % KG/Woche |
| Recomp | TDEE ±0, Protein hoch | Nur bei Anfängern oder Wiedereinsteigern realistisch — App sagt das ehrlich |
| Erhaltung | TDEE | — |

**Harte Sicherheitsgrenzen (nicht überschreibbar):**
- Zielgewicht darf nicht zu einem BMI < 18,5 führen.
- Kein Zugang für Nutzer unter 16 Jahren.
- Bei BMI < 17,5 oder erkanntem Muster restriktiven Verhaltens: Kalorienziel wird ausgeblendet und ein Hinweis auf professionelle Unterstützung angezeigt.

#### Schritt 4 — Adaptives TDEE (der eigentliche Vorteil)

Nach 14 Tagen mit Gewichts- und Kaloriendaten wird der reale Verbrauch zurückgerechnet:

```
Δ Gewicht (kg über n Tage, geglättet mit 7-Tage-Mittel)
TDEE_real = Ø Kalorienzufuhr − (Δ Gewicht × 7.700 / n)
```

Anschließend wird ein exponentiell gewichteter Mittelwert gebildet, um Rauschen zu dämpfen:

```
TDEE_adaptiv = 0,7 × TDEE_adaptiv_alt + 0,3 × TDEE_real
```

Bedingungen für die Anwendung: mindestens 14 Tage, mindestens 10 geloggte Gewichtsmessungen, Adhärenz beim Logging ≥ 80 %. Sonst wird weiter mit der Formelschätzung gearbeitet und dem Nutzer erklärt, warum.

Bei Frauen im gebärfähigen Alter wird die Zyklusphase optional erfasst und die Glättungsfenster auf 28 Tage erweitert, weil Wassereinlagerung sonst das Signal überlagert.

### 7.2 Makronährstoffverteilung

**Reihenfolge: Protein zuerst, dann Fett, Kohlenhydrate als Rest.**

#### Protein

Die ISSN-Position (902 Zitationen) setzt 1,4–2,0 g/kg Körpergewicht/Tag für die meisten trainierenden Personen an, und 2,3–3,1 g/kg **fettfreier Masse** zur Maximierung des Magermasseerhalts im Kaloriendefizit [6]. Der systematische Review von Helms et al. kommt für schlanke, kraftrainierte Athleten im Defizit auf 2,3–3,1 g/kg FFM, mit dem Bedarf steigend je stärker das Defizit und je schlanker die Person [7]. Eine aktuelle Meta-Regression (2025) bestätigt eine lineare Dosis-Wirkungs-Beziehung zwischen Proteinzufuhr und günstiger FFM-Veränderung, stärker ausgeprägt wenn relativ zur FFM ausgedrückt, bei Männern und bei niedrigerem Körperfettanteil [8].

Gegenposition, die wir fairerweise abbilden: Eine RCT von 2025 mit 21 College-Athleten fand über 6 Wochen bei 25 % Defizit **keinen** Unterschied zwischen 1,2, 1,6 und 2,2 g/kg und schließt, dass 1,2–1,7 g/kg für die meisten Athleten auch im Defizit ausreichen [9]. Die Studie ist klein — aber sie rechtfertigt, dass wir am unteren Rand des Korridors nicht dramatisieren.

**Unsere Umsetzung:**

| Situation | Protein |
|---|---|
| Erhaltung / Aufbau | 1,6–2,0 g/kg Körpergewicht |
| Defizit, normaler KFA | 1,8–2,2 g/kg Körpergewicht |
| Defizit, schlank (♂ <15 %, ♀ <25 % KFA) | 2,3–2,8 g/kg **FFM** |
| Adipositas (BMI > 30) | 1,4–1,8 g/kg **Zielgewicht**, nicht Istgewicht |
| Über 60 Jahre | Untergrenze +0,2 g/kg (Anabole Resistenz) |
| Vegan | Obergrenze des Korridors (Verdaulichkeit, Aminosäurenprofil) |

Eine Meta-Analyse über 74 RCTs bestätigt: Höhere Proteinzufuhr verbessert den Magermassezuwachs bei Personen im Krafttraining, wobei der Effekt bei unter 65-Jährigen ab ≥1,6 g/kg/Tag signifikant wird — bei über 65-Jährigen bereits ab 1,2–1,59 g/kg/Tag [18]. Das stützt unsere Untergrenze von 1,6 g/kg für Erhaltung und Aufbau.

Zusätzlich: Verteilungshinweis von 0,25 g/kg bzw. 20–40 g pro Mahlzeit alle 3–4 Stunden [6], als sanfter Coaching-Hinweis, nicht als hartes Ziel.

#### Fett

Untergrenze 0,6 g/kg Körpergewicht (Hormonproduktion, fettlösliche Vitamine), Standard 20–30 % der Gesamtkalorien. Bei sehr niedrigem Kalorienziel hat die Fett-Untergrenze Vorrang vor den Kohlenhydraten.

#### Kohlenhydrate

Rest der Kalorien. Untergrenze 2 g/kg bei intensivem Training (Glykogen für Trainingsqualität). Bei Konflikten wird der Nutzer gefragt, ob er das Defizit reduzieren oder das Training anpassen will.

#### Ballaststoffe

14 g pro 1.000 kcal (DGE/WHO-Orientierung), mindestens 25 g (♀) / 30 g (♂).

### 7.3 Nährstoff-Timing und Adhärenz

Die App arbeitet mit **Wochenbudgets, nicht Tagesbudgets** als primärer Metrik. Grund: Adhärenz ist der wichtigste Prädiktor für Erfolg, und tägliche Perfektion erzeugt Alles-oder-nichts-Denken. Der Nutzer sieht sein Tagesziel, aber die Erfolgsanzeige bezieht sich auf die Woche. Ein Tag über dem Ziel ist kein Scheitern, sondern ein Wert, der über die Woche ausgeglichen werden kann.

### 7.4 Trainingsplan-Generator

#### Schritt 1 — Wochenvolumen bestimmen

Die Evidenz zeigt eine klare Dosis-Wirkungs-Beziehung zwischen wöchentlichem Satzvolumen und Hypertrophie, allerdings mit abnehmendem Grenznutzen. Die Meta-Regression von Schoenfeld et al. fand pro zusätzlichem Satz eine Effektstärkenzunahme von 0,023, entsprechend +0,37 % Muskelzuwachs [10]. Die aktuellere Bayes'sche Meta-Regression von Pelland et al. (67 Studien, 2.058 Teilnehmer) bestätigt eine 100%ige posteriore Wahrscheinlichkeit, dass Zuwächse mit dem Volumen steigen — mit abnehmenden Erträgen, bei Kraft deutlich ausgeprägter als bei Hypertrophie [11]. Eine Meta-Analyse mit ausschließlich direkten, muskelspezifischen Messungen findet die Quadratwurzelfunktion als bestes Modell (marginale Steigung 0,24 % pro Satz) und **keinen klaren Plateaupunkt** [12].

Praktisch relevant: Baz-Valle et al. fanden keinen Unterschied zwischen moderatem (12–20 Sätze) und hohem (>20) Volumen für Quadrizeps und Bizeps und empfehlen **12–20 Sätze pro Muskelgruppe pro Woche** als Standardempfehlung für junge, trainierte Männer [13]. Ein Umbrella Review über 14 Meta-Analysen setzt mindestens 10 Sätze/Woche/Muskelgruppe als optimal an [14].

**Unsere Volumenkorridore:**

| Erfahrung | Sätze/Muskelgruppe/Woche (Start) | Aufbau über Mesozyklus |
|---|---|---|
| Anfänger (< 6 Monate) | 8–10 | +1 Satz pro Muskelgruppe alle 2 Wochen |
| Fortgeschritten (6–24 Monate) | 12–16 | +1 Satz pro Muskelgruppe pro Woche |
| Erfahren (> 2 Jahre) | 14–20 | +1–2 Sätze pro Woche bis zum Deload |
| Über 60 | 10–14, dafür 3× Frequenz | konservativer |

**Wichtige Nuance, die die meisten Apps ignorieren:** Direkte und indirekte Sätze müssen unterschiedlich gewichtet werden. Ein Bankdrücken-Satz ist ein direkter Satz für die Brust, aber nur ein *fraktionaler* Satz (0,5) für den Trizeps. Die Meta-Regressionen zeigen, dass die „fraktionale" Zählmethode die beste Vorhersagekraft hat [11][12]. **Wir implementieren das** — jeder Übung ist im Datenmodell eine Muskelbeteiligung mit Faktor 1,0 (primär) oder 0,5 (sekundär) zugeordnet. Das ist der Grund, warum unser Volumen-Dashboard genauer ist als das von Hevy oder Fitbod.

Zusätzlich: Bei Älteren mit geringer Trainingsantwort kann höheres Volumen die Nichtresponder-Rate senken [15] — der Generator erhöht bei ausbleibendem Fortschritt automatisch das Volumen statt die Intensität.

**Und eine Warnung, die die App im Volumen-Dashboard aussprechen muss:** Die Dosis-Wirkungs-Beziehung gilt für *Hypertrophie*, nicht für *Kraft*. Eine RCT mit 34 trainierten Männern fand über 8 Wochen bei 1, 3 und 5 Sätzen pro Übung **keine** Unterschiede in Kraft und Ausdauer — wohl aber signifikant größere Muskelzuwächse zugunsten der höheren Volumina [17]. Wer primär stärker werden will, braucht nicht mehr Volumen, sondern höhere Lasten. Der Generator gewichtet daher bei `goal = 'strength'` die Intensität und die Frequenz höher als das Volumen.

#### Schritt 2 — Frequenz und Split

Die Bayes'sche Netzwerk-Metaanalyse von Currier et al. (178 Studien für Kraft, 119 für Hypertrophie) fand: Höhere Lasten (>80 % 1RM) maximieren Kraftzuwächse; für Hypertrophie sind alle Lastbereiche vergleichbar, aber Mehrsatz-Protokolle führen. Das bestbewertete Protokoll für Kraft war hohe Last / mehrere Sätze / 3× wöchentlich, für Hypertrophie hohe Last / mehrere Sätze / 2× wöchentlich [16]. Für Frequenz gilt: Kraftzuwächse steigen mit der Frequenz (abnehmend), bei Hypertrophie ist der Effekt vernachlässigbar, solange das Volumen gleich ist [11].

**Split-Zuordnung:**

| Tage/Woche | Split | Frequenz pro Muskel |
|---|---|---|
| 2 | Ganzkörper A/B | 2× |
| 3 | Ganzkörper A/B/C | 3× |
| 4 | Oberkörper/Unterkörper | 2× |
| 5 | Push/Pull/Legs + Upper/Lower | ~2× |
| 6 | Push/Pull/Legs ×2 | 2× |

Anfänger bekommen **immer Ganzkörper**, unabhängig von der Tageszahl — mehr Übungswiederholung bedeutet schnelleres motorisches Lernen, und genau das ist bei Anfängern der limitierende Faktor, nicht das Volumen.

#### Schritt 3 — Übungsauswahl

Bewertungsfunktion pro Kandidatenübung:

```
Score = w1·Zielmuskel-Match
      + w2·Equipment-Verfügbarkeit
      + w3·Erfahrungs-Eignung
      + w4·Verletzungs-Sicherheit      (hartes Ausschlusskriterium bei Konflikt)
      + w5·Bewegungsmuster-Abdeckung
      + w6·Zeitbudget-Effizienz
      − w7·Redundanz zu bereits gewählten Übungen
      − w8·Nutzer-Ablehnungshistorie
```

**Bewegungsmuster-Abdeckung** ist die wichtigste Nebenbedingung: Jeder Plan muss die sechs Grundmuster enthalten — Kniebeugen, Hüftbeugen (Hinge), Drücken horizontal, Drücken vertikal, Ziehen horizontal, Ziehen vertikal — plus Rumpfstabilisation und einbeinige Arbeit. Das verhindert die typischen Lücken (Rückseite vernachlässigt, kein vertikales Ziehen).

**Anfänger-Regeln (hart):**
- Maximal 6 verschiedene Übungen pro Einheit, maximal 12 verschiedene im gesamten Programm. Motorisches Lernen braucht Wiederholung.
- Erst Maschinen und geführte Bewegungen, dann Kurzhantel, dann Langhantel. Freie Kniebeuge und Kreuzheben frühestens in Woche 3, und erst nach bestandenem Bewegungs-Check.
- Kein Übungswechsel in den ersten 4 Wochen (Gegenteil von Fitbods Ansatz — dort ist die ständige Variation für Anfänger schädlich).

#### Schritt 4 — Progression

**Anfänger: Lineare Progression.**
```
WENN alle Sätze im oberen Wiederholungsbereich geschafft
DANN Gewicht +2,5 kg (Oberkörper) / +5 kg (Unterkörper), Wiederholungen zurück auf unteren Bereich
```

**Fortgeschrittene: Doppelte Progression mit RIR-Steuerung.**
```
Zielbereich z.B. 8–12 Wdh bei RIR 2
WENN alle Sätze bei 12 Wdh UND RIR ≥ 2  → Gewicht +2,5–5 %
WENN Wdh < 8 bei RIR 0 in 2 Einheiten hintereinander → Gewicht −5 %, Technik-Check anbieten
SONST → gleiches Gewicht, eine Wiederholung mehr anstreben
```

**Mesozyklus-Volumenprogression:**
```
Woche 1: Startvolumen         RIR 3
Woche 2: +1 Satz/Muskelgruppe RIR 2
Woche 3: +1 Satz/Muskelgruppe RIR 2
Woche 4: +1 Satz/Muskelgruppe RIR 1
Woche 5: +1 Satz/Muskelgruppe RIR 0–1   (nur Erfahrene)
Woche 6: DELOAD — 50 % Volumen, 80 % Intensität, RIR 4
```

**Deload-Trigger (automatisch):** stagnierende oder fallende Leistung über 3 Wochen · RIR-Ziel wiederholt verfehlt · subjektive Erschöpfung hoch (Abfrage) · > 8 Wochen durchgehendes Kaloriendefizit · Schlafqualität selbst berichtet niedrig.

### 7.5 Ernährung × Training — der Regelkreis

Das ist die Verbindung, die keine der Konkurrenz-Apps hat:

| Signal | Reaktion |
|---|---|
| Trainingsvolumen steigt | Kalorien- und Kohlenhydratziel steigen |
| Trainingswoche ausgefallen | Kalorienziel sinkt automatisch |
| Kaloriendefizit > 8 Wochen | Deload wird empfohlen, Volumensteigerung ausgesetzt, Diätpause vorgeschlagen |
| Kraft fällt bei Diät | Warnung: Defizit zu aggressiv oder Protein zu niedrig — konkreter Vorschlag |
| Protein 3 Tage unter Ziel | Hinweis vor dem nächsten Training + konkrete Lebensmittelvorschläge |
| Gewichtsplateau > 3 Wochen | Adaptives TDEE wird neu berechnet, Ziel angepasst, Erklärung angezeigt |
| Aufbau, aber Gewicht steigt zu schnell | Überschuss reduzieren („zu viel davon wird Fett") |
| Schlaf/Erholung schlecht (Wearable) | Trainingsintensität für den Tag reduziert vorschlagen |

### 7.6 Fit-Score — „Lohnt sich der Kauf?"

Der Kern des Barcode-Features. Der Score ist **personalisiert und kontextabhängig** — dasselbe Produkt bekommt bei einem Nutzer im Aufbau eine andere Bewertung als bei einer Nutzerin im Defizit.

```
FitScore = 100 × (
    0,30 · Ziel_Passung
  + 0,25 · Nährstoffdichte
  + 0,20 · Makro_Beitrag
  + 0,15 · Verarbeitungsgrad
  + 0,10 · Sättigungsindex
) − Malus
```

**Komponenten:**

| Komponente | Berechnung |
|---|---|
| **Ziel_Passung** | Passt die Kaloriendichte zum Ziel? Defizit → niedrige Energiedichte belohnt; Aufbau → hohe Energiedichte belohnt. Skaliert am verbleibenden Tagesbudget. |
| **Nährstoffdichte** | Mikronährstoffe pro 100 kcal, normiert auf Referenzwerte. Belohnt Lebensmittel, die neben Kalorien auch Nährstoffe liefern. |
| **Makro_Beitrag** | Deckt das Produkt ein Makro ab, bei dem der Nutzer heute im Rückstand ist? Hoher Proteingehalt bei Proteindefizit → starker Bonus. |
| **Verarbeitungsgrad** | NOVA-Klassifikation aus Open Food Facts. NOVA 4 (hochverarbeitet) → Abzug, NOVA 1 → Bonus. |
| **Sättigungsindex** | Geschätzt aus Protein-, Ballaststoff- und Wassergehalt vs. Energiedichte. Wichtig im Defizit. |
| **Malus** | Allergen im Profil → Score wird auf 0 gesetzt und Warnung angezeigt. Über 90 % Tages-Zuckerbudget in einer Portion → −25. Trans-Fette → −20. Ernährungspräferenz verletzt (z. B. Gelatine bei Vegetarier) → 0. |

**Ausgabe an den Nutzer** — nie nur eine Zahl, immer ein Satz:

> **82 / 100 — Passt gut**
> 22 g Protein pro Portion. Du liegst heute 61 g unter deinem Proteinziel.
> 4,2 g Ballaststoffe. Wenig Zucker (2,1 g).
> ⚠ Enthält Milch — steht in deinem Profil nicht als Allergen.

> **34 / 100 — Eher nicht für heute**
> 34 g Zucker pro Portion = 68 % deines Tagesbudgets.
> NOVA 4 (hochverarbeitet), 11 Zusatzstoffe.
> **Bessere Alternative im selben Regal:** [Produkt X, 71/100] — ähnlicher Geschmack, 12 g weniger Zucker, 6 g mehr Protein.

**Wichtig für die Wellbeing-Guardrails:** Der Score bewertet **Passung zum heutigen Ziel**, nicht „gut" oder „schlecht" als moralische Kategorie. Die Sprache ist bewusst „passt heute nicht so gut" statt „ungesund". Kein Produkt wird verboten. Es gibt keinen Streak, der bei einem niedrigen Score bricht.

### 7.7 Alternativ-Empfehlung

```
1. Produktkategorie aus Open Food Facts Taxonomie bestimmen
2. Kandidaten: gleiche Kategorie, in DE/EU verfügbar, Nutzerpräferenzen erfüllt
3. Fit-Score für jeden Kandidaten mit dem aktuellen Nutzerkontext berechnen
4. Sortieren, Top 3 mit einem Score-Delta ≥ 15 anzeigen
5. Wenn kein Kandidat besser ist: das ehrlich sagen ("Das ist eine der besseren Optionen in dieser Kategorie")
```

---

## 8. Datenmodell

PostgreSQL. Zeitstempel durchgehend `timestamptz`. Row-Level Security aktiv auf allen Nutzertabellen.

### 8.1 Nutzer und Profil

```sql
CREATE TYPE sex_t          AS ENUM ('male','female','other','unspecified');
CREATE TYPE goal_t         AS ENUM ('lose','gain_muscle','gain_weight','strength','maintain','recomp');
CREATE TYPE experience_t   AS ENUM ('none','beginner','intermediate','advanced');
CREATE TYPE unit_system_t  AS ENUM ('metric','imperial');

CREATE TABLE users (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email           citext UNIQUE NOT NULL,
  password_hash   text,                       -- NULL bei OAuth/Passkey
  locale          text NOT NULL DEFAULT 'de', -- 'de' | 'en'
  unit_system     unit_system_t NOT NULL DEFAULT 'metric',
  timezone        text NOT NULL DEFAULT 'Europe/Berlin',
  created_at      timestamptz NOT NULL DEFAULT now(),
  deleted_at      timestamptz                 -- Soft Delete, Hard Delete nach 30 Tagen
);

CREATE TABLE profiles (
  user_id             uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  sex                 sex_t        NOT NULL,
  birth_date          date         NOT NULL,
  height_cm           numeric(5,1) NOT NULL CHECK (height_cm BETWEEN 100 AND 250),
  experience          experience_t NOT NULL,
  activity_pal        numeric(3,2) NOT NULL DEFAULT 1.40 CHECK (activity_pal BETWEEN 1.10 AND 2.00),
  goal                goal_t       NOT NULL,
  goal_rate_pct_week  numeric(4,3) NOT NULL DEFAULT 0.5,   -- % Körpergewicht/Woche
  target_weight_kg    numeric(5,2),
  body_fat_pct        numeric(4,1),
  dietary_prefs       text[]       NOT NULL DEFAULT '{}',  -- vegan, halal, ...
  allergens           text[]       NOT NULL DEFAULT '{}',  -- 14 EU-Allergene
  injuries            text[]       NOT NULL DEFAULT '{}',  -- knee, shoulder, low_back, ...
  equipment           text[]       NOT NULL DEFAULT '{}',
  training_days_week  smallint     NOT NULL DEFAULT 3 CHECK (training_days_week BETWEEN 1 AND 7),
  session_minutes     smallint     NOT NULL DEFAULT 60,
  updated_at          timestamptz  NOT NULL DEFAULT now()
);

-- Zielhistorie: nie überschreiben, immer neue Version.
-- Wichtig für Nachvollziehbarkeit (AI Act) und für die Fortschrittsanzeige.
CREATE TABLE nutrition_targets (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  valid_from      date NOT NULL,
  kcal            integer NOT NULL,
  protein_g       integer NOT NULL,
  fat_g           integer NOT NULL,
  carbs_g         integer NOT NULL,
  fiber_g         integer NOT NULL,
  water_ml        integer NOT NULL DEFAULT 2500,
  bmr_kcal        integer NOT NULL,
  tdee_kcal       integer NOT NULL,
  tdee_source     text NOT NULL,          -- 'mifflin' | 'katch' | 'adaptive'
  calculation     jsonb NOT NULL,         -- vollständige Herleitung, für "Wie kam das zustande?"
  created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON nutrition_targets (user_id, valid_from DESC);
```

### 8.2 Lebensmittel

```sql
CREATE TYPE food_source_t     AS ENUM ('bls','usda','off','brand_verified','user','ai_estimate');
CREATE TYPE trust_t           AS ENUM ('verified','community','estimated');

CREATE TABLE foods (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  source            food_source_t NOT NULL,
  source_id         text,                      -- BLS-Schlüssel, FDC-ID, OFF-Code
  trust             trust_t NOT NULL,
  barcode           text,                      -- EAN/UPC
  brand             text,
  name_de           text NOT NULL,
  name_en           text NOT NULL,
  category          text,                      -- OFF-Taxonomie
  nova_group        smallint CHECK (nova_group BETWEEN 1 AND 4),
  nutriscore        char(1),

  -- Alle Werte pro 100 g bzw. 100 ml
  kcal              numeric(7,2) NOT NULL,
  protein_g         numeric(6,2) NOT NULL,
  fat_g             numeric(6,2) NOT NULL,
  saturated_fat_g   numeric(6,2),
  trans_fat_g       numeric(6,2),
  carbs_g           numeric(6,2) NOT NULL,
  sugar_g           numeric(6,2),
  fiber_g           numeric(6,2),
  salt_g            numeric(6,3),
  micros            jsonb NOT NULL DEFAULT '{}',  -- {"vitamin_d_ug":1.2,"iron_mg":2.1,...}

  allergens         text[] NOT NULL DEFAULT '{}',
  additives         text[] NOT NULL DEFAULT '{}',
  is_liquid         boolean NOT NULL DEFAULT false,
  satiety_index     numeric(4,2),                 -- berechnet, siehe §7.6
  verified_at       timestamptz,
  verified_by       uuid REFERENCES users(id),
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ON foods (barcode) WHERE barcode IS NOT NULL AND trust = 'verified';
CREATE INDEX ON foods (source, source_id);
CREATE INDEX foods_search_de_idx ON foods
  USING GIN (to_tsvector('german', coalesce(brand,'') || ' ' || name_de));
CREATE INDEX foods_search_en_idx ON foods
  USING GIN (to_tsvector('english', coalesce(brand,'') || ' ' || name_en));
CREATE INDEX foods_trgm_idx ON foods USING GIN (name_de gin_trgm_ops);  -- Tippfehlertoleranz

-- Portionsgrößen: der eigentliche Genauigkeitshebel.
-- "1 Scheibe", "1 mittlere Banane", "1 Portion (laut Hersteller)"
CREATE TABLE food_servings (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  food_id      uuid NOT NULL REFERENCES foods(id) ON DELETE CASCADE,
  label_de     text NOT NULL,
  label_en     text NOT NULL,
  grams        numeric(7,2) NOT NULL,
  is_default   boolean NOT NULL DEFAULT false
);
```

### 8.3 Ernährungsprotokoll

```sql
CREATE TYPE meal_slot_t AS ENUM ('breakfast','lunch','dinner','snack','pre_workout','post_workout');
CREATE TYPE entry_method_t AS ENUM ('search','barcode','photo','voice','recipe','quick_add','copy');

-- Migrationsreihenfolge: recipes VOR food_entries anlegen (Fremdschlüssel).
-- Hier zur besseren Lesbarkeit umgekehrt dargestellt.

CREATE TABLE food_entries (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  food_id       uuid REFERENCES foods(id),
  recipe_id     uuid REFERENCES recipes(id),
  logged_date   date NOT NULL,                  -- lokales Datum des Nutzers
  slot          meal_slot_t NOT NULL,
  grams         numeric(8,2) NOT NULL,
  serving_id    uuid REFERENCES food_servings(id),
  method        entry_method_t NOT NULL,
  confidence    numeric(3,2),                   -- nur bei method='photo'
  -- Nährwerte denormalisiert gespeichert: historische Einträge dürfen sich
  -- nicht ändern, wenn die Quelldaten korrigiert werden.
  kcal          numeric(7,2) NOT NULL,
  protein_g     numeric(6,2) NOT NULL,
  fat_g         numeric(6,2) NOT NULL,
  carbs_g       numeric(6,2) NOT NULL,
  micros        jsonb NOT NULL DEFAULT '{}',
  client_id     text,                           -- Idempotenz für Offline-Sync
  created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON food_entries (user_id, logged_date);
CREATE UNIQUE INDEX ON food_entries (user_id, client_id) WHERE client_id IS NOT NULL;

CREATE TABLE recipes (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       uuid REFERENCES users(id) ON DELETE CASCADE,  -- NULL = kuratiert
  name_de       text NOT NULL,
  name_en       text,
  servings      numeric(4,1) NOT NULL DEFAULT 1,
  instructions  text,
  source_url    text,
  is_public     boolean NOT NULL DEFAULT false,
  created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE recipe_items (
  recipe_id  uuid NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
  food_id    uuid NOT NULL REFERENCES foods(id),
  grams      numeric(8,2) NOT NULL,
  sort_order smallint NOT NULL DEFAULT 0,
  PRIMARY KEY (recipe_id, food_id, sort_order)
);
```

### 8.4 Übungen und Bewegungsvermittlung

```sql
CREATE TYPE movement_pattern_t AS ENUM
  ('squat','hinge','push_horizontal','push_vertical','pull_horizontal',
   'pull_vertical','carry','core','isolation','cardio');
CREATE TYPE mechanic_t AS ENUM ('compound','isolation');
CREATE TYPE difficulty_t AS ENUM ('beginner','intermediate','advanced');

CREATE TABLE exercises (
  id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  slug               text UNIQUE NOT NULL,
  name_de            text NOT NULL,
  name_en            text NOT NULL,
  aliases            text[] NOT NULL DEFAULT '{}',   -- "Bankdrücken", "Bench Press", "Flachbank"
  pattern            movement_pattern_t NOT NULL,
  mechanic           mechanic_t NOT NULL,
  equipment          text[] NOT NULL,
  difficulty         difficulty_t NOT NULL,
  unilateral         boolean NOT NULL DEFAULT false,
  met_value          numeric(4,2),                   -- für Kalorienschätzung
  contraindications  text[] NOT NULL DEFAULT '{}',   -- 'knee','shoulder','low_back'
  -- Bewegungsvermittlung
  video_front_url    text,
  video_side_url     text,
  thumbnail_url      text,
  setup_steps_de     text[] NOT NULL DEFAULT '{}',
  setup_steps_en     text[] NOT NULL DEFAULT '{}',
  execution_steps_de text[] NOT NULL DEFAULT '{}',
  execution_steps_en text[] NOT NULL DEFAULT '{}',
  cues_de            text[] NOT NULL DEFAULT '{}',   -- "Brust raus", "Knie nach außen drücken"
  cues_en            text[] NOT NULL DEFAULT '{}',
  breathing_de       text,
  breathing_en       text,
  tempo              text,                            -- "2-0-1-0"
  -- Progressionsleiter
  regression_of      uuid REFERENCES exercises(id),
  progression_to     uuid REFERENCES exercises(id),
  created_at         timestamptz NOT NULL DEFAULT now()
);

-- Muskelbeteiligung mit Faktor: DER Schlüssel für korrekte Volumenzählung (§7.4)
CREATE TABLE exercise_muscles (
  exercise_id  uuid NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
  muscle       text NOT NULL,           -- 'chest','front_delt','triceps','quads',...
  factor       numeric(2,1) NOT NULL CHECK (factor IN (0.5, 1.0)),  -- indirekt | direkt
  PRIMARY KEY (exercise_id, muscle)
);

CREATE TABLE exercise_mistakes (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  exercise_id    uuid NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
  title_de       text NOT NULL,
  title_en       text NOT NULL,
  why_bad_de     text NOT NULL,
  why_bad_en     text NOT NULL,
  fix_de         text NOT NULL,
  fix_en         text NOT NULL,
  image_url      text,
  severity       smallint NOT NULL DEFAULT 2 CHECK (severity BETWEEN 1 AND 3)
);

-- Regelwerk für die kamerabasierte Formanalyse (V2)
CREATE TABLE exercise_form_rules (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  exercise_id  uuid NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
  rule_key     text NOT NULL,          -- 'knee_valgus','depth','back_round','bar_path'
  joints       text[] NOT NULL,        -- MediaPipe Landmark-Namen
  metric       text NOT NULL,          -- 'angle' | 'distance_ratio' | 'vertical_deviation'
  min_value    numeric(6,2),
  max_value    numeric(6,2),
  phase        text,                   -- 'eccentric' | 'bottom' | 'concentric'
  feedback_de  text NOT NULL,
  feedback_en  text NOT NULL,
  severity     smallint NOT NULL DEFAULT 2
);
```

### 8.5 Trainingsplanung und -protokoll

```sql
CREATE TABLE programs (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id        uuid REFERENCES users(id) ON DELETE CASCADE,  -- NULL = Vorlage
  name_de        text NOT NULL,
  name_en        text,
  goal           goal_t NOT NULL,
  days_per_week  smallint NOT NULL,
  weeks          smallint NOT NULL DEFAULT 6,
  split_type     text NOT NULL,          -- 'full_body','upper_lower','ppl'
  generated_by   text NOT NULL,          -- 'algorithm_v1' | 'manual' | 'template'
  generation_ctx jsonb,                  -- Eingaben des Generators, für Nachvollziehbarkeit
  is_active      boolean NOT NULL DEFAULT true,
  created_at     timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE program_days (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  program_id   uuid NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
  week_number  smallint NOT NULL,
  day_index    smallint NOT NULL,
  name_de      text NOT NULL,
  name_en      text,
  is_deload    boolean NOT NULL DEFAULT false
);

CREATE TABLE program_items (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  program_day_id uuid NOT NULL REFERENCES program_days(id) ON DELETE CASCADE,
  exercise_id    uuid NOT NULL REFERENCES exercises(id),
  sort_order     smallint NOT NULL,
  sets           smallint NOT NULL,
  rep_min        smallint NOT NULL,
  rep_max        smallint NOT NULL,
  target_rir     smallint,
  rest_seconds   smallint NOT NULL DEFAULT 120,
  superset_group smallint,
  notes_de       text,
  notes_en       text
);

CREATE TABLE workout_sessions (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id        uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  program_day_id uuid REFERENCES program_days(id),
  started_at     timestamptz NOT NULL,
  finished_at    timestamptz,
  perceived_effort smallint CHECK (perceived_effort BETWEEN 1 AND 10),
  notes          text,
  client_id      text,
  UNIQUE (user_id, client_id)
);

CREATE TABLE workout_sets (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  session_id    uuid NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
  exercise_id   uuid NOT NULL REFERENCES exercises(id),
  set_index     smallint NOT NULL,
  weight_kg     numeric(6,2),
  reps          smallint,
  duration_sec  integer,          -- für Cardio/Isometrie
  distance_m    numeric(8,2),
  rir           smallint,
  is_warmup     boolean NOT NULL DEFAULT false,
  completed     boolean NOT NULL DEFAULT true,
  form_score    numeric(4,1),     -- aus der Kameraanalyse, wenn genutzt
  logged_at     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON workout_sets (session_id);
CREATE INDEX ON workout_sets (exercise_id, logged_at DESC);

-- Materialisierte Sicht für das Volumen-Dashboard.
-- Beachte die Gewichtung mit em.factor (direkte vs. fraktionale Sätze, §7.4)
CREATE MATERIALIZED VIEW weekly_muscle_volume AS
SELECT
  ws.user_id,
  date_trunc('week', wse.logged_at)::date AS week,
  em.muscle,
  SUM(em.factor)                                        AS weighted_sets,
  SUM(wse.weight_kg * wse.reps * em.factor)             AS tonnage
FROM workout_sets wse
JOIN workout_sessions ws  ON ws.id = wse.session_id
JOIN exercise_muscles em  ON em.exercise_id = wse.exercise_id
WHERE wse.completed AND NOT wse.is_warmup
GROUP BY 1,2,3;
```

### 8.6 Messwerte und adaptives Modell

```sql
CREATE TABLE body_measurements (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id        uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  measured_on    date NOT NULL,
  weight_kg      numeric(5,2),
  body_fat_pct   numeric(4,1),
  waist_cm       numeric(5,1),
  hip_cm         numeric(5,1),
  chest_cm       numeric(5,1),
  arm_cm         numeric(5,1),
  thigh_cm       numeric(5,1),
  source         text NOT NULL DEFAULT 'manual',  -- 'manual','apple_health','garmin',...
  UNIQUE (user_id, measured_on, source)
);

CREATE TABLE tdee_estimates (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id        uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  computed_on    date NOT NULL,
  window_days    smallint NOT NULL,
  avg_intake     numeric(7,1) NOT NULL,
  weight_delta_kg numeric(5,3) NOT NULL,
  tdee_observed  numeric(7,1) NOT NULL,
  tdee_smoothed  numeric(7,1) NOT NULL,
  log_adherence  numeric(4,3) NOT NULL,
  applied        boolean NOT NULL DEFAULT false,
  UNIQUE (user_id, computed_on)
);
```

### 8.7 Scans und Empfehlungen

```sql
CREATE TABLE barcode_scans (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  barcode       text NOT NULL,
  food_id       uuid REFERENCES foods(id),
  found         boolean NOT NULL,
  fit_score     smallint,
  score_breakdown jsonb,                -- Erklärbarkeit: welche Komponente wie viel beitrug
  logged        boolean NOT NULL DEFAULT false,
  scanned_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON barcode_scans (barcode);   -- treibt "häufig gescannt, aber nicht gefunden"
```

---

## 9. API-Design

**Stil:** Einheitlich REST unter `/api/v1` (Spring Boot, OpenAPI/Swagger-dokumentiert). Das Next.js-Frontend konsumiert dieselbe REST-Schicht über TanStack Query (ein aus der OpenAPI-Spec generierter TS-Client ersetzt die ursprünglich geplante tRPC-Typsicherheit); dieselbe Schicht bedient auch Integrationen und eine spätere native App.

**Konventionen:** JSON, `Accept-Language` steuert die Sprache aller Textfelder, Cursor-Pagination, RFC 9457 Problem Details für Fehler, Idempotenz über `Idempotency-Key`-Header bei allen Schreibvorgängen.

### 9.1 Auth

```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
POST   /api/v1/auth/passkey/challenge
POST   /api/v1/auth/passkey/verify
```

### 9.2 Profil und Ziele

```
GET    /api/v1/me
PATCH  /api/v1/me/profile
GET    /api/v1/me/targets?date=2026-08-05
POST   /api/v1/me/targets/recalculate     → 202, Job-ID
GET    /api/v1/me/targets/explain?date=…  → Herleitung als strukturierte Erklärung
GET    /api/v1/me/export?format=json|csv  → DSGVO Art. 20
DELETE /api/v1/me                          → DSGVO Art. 17, 30 Tage Karenz
```

Beispielantwort `GET /me/targets/explain`:

```json
{
  "date": "2026-08-05",
  "result": { "kcal": 2140, "protein_g": 165, "fat_g": 60, "carbs_g": 235 },
  "_check": "165×4 + 60×9 + 235×4 = 660 + 540 + 940 = 2140 kcal ✓",
  "steps": [
    { "key": "bmr", "method": "mifflin_st_jeor", "value": 1763,
      "formula": "10×78 + 6.25×182 − 5×32 + 5 = 1762.5",
      "note_de": "Grundumsatz — was dein Körper in völliger Ruhe verbraucht.",
      "confidence": "±10 % bei etwa der Hälfte aller Personen" },
    { "key": "pal", "value": 1.40, "note_de": "Bürojob, 6.000 Schritte im Schnitt." },
    { "key": "tdee_basis", "value": 2468, "formula": "1763 × 1.40" },
    { "key": "training", "value": 186,
      "note_de": "Aus deinen 4 Einheiten letzte Woche, auf 7 Tage verteilt." },
    { "key": "tdee_formula", "value": 2654 },
    { "key": "tdee_adaptive", "value": 2569,
      "note_de": "Aus deinem tatsächlichen Gewichtsverlauf der letzten 21 Tage zurückgerechnet. Wir nutzen diesen Wert, weil er auf deinen echten Daten beruht.",
      "applied": true },
    { "key": "goal_adjustment", "value": -429,
      "formula": "78 kg × 0.5 % × 7700 kcal / 7 Tage",
      "note_de": "Ziel: 0,5 % Körpergewicht pro Woche abnehmen." }
  ],
  "guardrails": [
    { "key": "min_kcal", "passed": true, "threshold": 1939, "rule": "BMR × 1.1" },
    { "key": "min_fat",  "passed": true, "threshold": 47,   "rule": "0.6 g/kg" }
  ]
}
```

### 9.3 Lebensmittel

```
GET    /api/v1/foods/search?q=…&locale=de&limit=20&trust=verified
GET    /api/v1/foods/:id
GET    /api/v1/foods/barcode/:code
POST   /api/v1/foods                       → eigenes Lebensmittel anlegen
POST   /api/v1/foods/from-label            → OCR eines Nährwertetiketts
POST   /api/v1/foods/from-photo            → KI-Schätzung einer Mahlzeit
GET    /api/v1/foods/:id/alternatives      → bessere Alternativen (§7.7)
```

`GET /foods/barcode/:code` liefert Produkt **und** personalisierte Bewertung in einem Aufruf:

```json
{
  "food": {
    "id": "…", "name": "Skyr Natur", "brand": "Arla",
    "per_100g": { "kcal": 63, "protein_g": 11.0, "fat_g": 0.2, "carbs_g": 4.0, "sugar_g": 4.0 },
    "nova_group": 1, "nutriscore": "A",
    "allergens": ["milk"],
    "trust": "verified", "source": "brand_verified"
  },
  "fit": {
    "score": 88,
    "verdict": "good",
    "headline_de": "Passt sehr gut zu deinem heutigen Ziel",
    "reasons_de": [
      "22 g Protein pro Portion (200 g) — du liegst heute 61 g unter deinem Proteinziel",
      "Nur 126 kcal pro Portion bei hohem Sättigungswert",
      "NOVA 1 — unverarbeitet"
    ],
    "warnings_de": ["Enthält Milch"],
    "breakdown": {
      "goal_fit": 0.92, "nutrient_density": 0.81, "macro_contribution": 0.95,
      "processing": 1.0, "satiety": 0.88, "penalty": 0
    }
  },
  "alternatives": []
}
```

### 9.4 Tagebuch

```
GET    /api/v1/diary/:date
POST   /api/v1/diary/entries
POST   /api/v1/diary/entries/batch          → Multi-Select-Logging
PATCH  /api/v1/diary/entries/:id
DELETE /api/v1/diary/entries/:id
POST   /api/v1/diary/copy                   → { from: date, to: date, slots: [...] }
GET    /api/v1/diary/summary?from=…&to=…
```

### 9.5 Training

```
GET    /api/v1/exercises?pattern=…&equipment=…&difficulty=…&locale=de
GET    /api/v1/exercises/:slug              → inkl. Cues, Fehler, Videos
GET    /api/v1/exercises/:slug/alternatives?equipment=…&injuries=…

POST   /api/v1/programs/generate            → Plan aus Profil erzeugen
GET    /api/v1/programs/active
PATCH  /api/v1/programs/:id
POST   /api/v1/programs/:id/swap-exercise   → { itemId, reason }

POST   /api/v1/workouts                     → Session starten
POST   /api/v1/workouts/:id/sets            → Satz loggen (idempotent)
PATCH  /api/v1/workouts/:id/finish
GET    /api/v1/workouts?from=…&to=…
GET    /api/v1/analytics/volume?weeks=12    → gewichtete Sätze pro Muskelgruppe
GET    /api/v1/analytics/strength/:exercise → e1RM-Verlauf
```

### 9.6 Sync (offline-first)

```
POST   /api/v1/sync/push    → { changes: [{ table, op, clientId, payload, updatedAt }] }
GET    /api/v1/sync/pull?since=<cursor>
```

Konfliktstrategie: **Feld-Level Last-Write-Wins** mit Server-Zeitstempel als Tiebreaker. Für die typischen Schreibmuster dieser App (ein Nutzer, ein Gerät zur Zeit, additive Einträge) reicht das aus — CRDTs wären hier überdimensioniert und würden das Datenmodell unnötig verkomplizieren. Löschungen als Tombstones, 90 Tage vorgehalten.

---

## 10. Tech-Stack und Architektur

### 10.1 Empfohlener Stack

| Schicht | Wahl | Begründung |
|---|---|---|
| Backend | **Kotlin + Spring Boot 3**, Gradle | Typsicherer, etablierter JVM-Stack; Team-Entscheidung, ersetzt das ursprünglich skizzierte Node/TS-Backend |
| Frontend | **Next.js 15 (App Router) + React 19, TypeScript** | Server Components reduzieren die JS-Last spürbar, ein Deployment-Ziel für Web + PWA, starkes Ökosystem |
| Styling | **Tailwind CSS + shadcn/ui** | Schnell, konsistent, barrierefreie Primitives (Radix) |
| State (Server) | **TanStack Query** | Cache, Optimistic Updates, Offline-Mutation-Queue |
| State (lokal) | **Zustand** | Klein, kein Boilerplate |
| API | **REST** (Spring Boot, OpenAPI/Swagger-dokumentiert) | Einheitliche Schicht für Frontend, Integrationen und spätere native App; TS-Client für das Frontend aus der OpenAPI-Spec generiert (ersetzt tRPC, das ein TS-Backend vorausgesetzt hätte) |
| Datenbank | **PostgreSQL 17** | Volltextsuche, `pg_trgm`, JSONB, `pgvector` für spätere semantische Suche |
| ORM / Migrationen | **Spring Data JPA** (oder Exposed) + **Flyway** | Etablierter Kotlin/Spring-Standard, Migrationen versioniert und nachvollziehbar |
| Lokale DB | **IndexedDB via Dexie.js** | Praktischer Standard für Offline-PWA; OPFS/SQLite-WASM ist die Ausbaustufe, aber Safari-Support ist noch heikel |
| Sync | Eigene Queue über TanStack Query + Dexie | CRDT-Bibliotheken sind für additive Einträge Overkill; Feld-Level-LWW deckt die Konflikte ab |
| Auth | **Spring Security** + OAuth2-Client (Apple/Google) + WebAuthn4j (Passkeys) | Serverseitig im Kotlin-Backend, selbst gehostet, kein Nutzerdaten-Abfluss zu Dritten |
| Objektspeicher | **Hetzner Object Storage** (S3-kompatibel), EU | Videos, Bilder — deutlich günstiger als AWS |
| Hosting | **Hetzner Cloud (Falkenstein/Nürnberg)** + Docker + Coolify | DSGVO-Positionierung ist Teil des Produktversprechens |
| CDN | **BunnyCDN** (EU-Perimeter konfigurierbar) | Videoauslieferung, günstig |
| Pose Estimation | **MediaPipe Pose (Tasks Vision, WASM)** | Läuft vollständig im Browser, kein Serverkontakt — die einzige datenschutzkonforme Option für Videoanalyse |
| Barcode | `BarcodeDetector` API, Fallback `zxing-wasm` | Nativ wo verfügbar, sonst WASM |
| KI (Foto/NLP) | Claude oder GPT via EU-Endpunkt, mit striktem Tool-Schema | Ergebnis muss gegen die eigene DB gematcht werden, nicht frei generiert |
| E-Mail | **Postmark EU** oder **Brevo** | Transaktional, DSGVO-konform |
| Fehler/Analytics | **Sentry (self-hosted)** + **Plausible/Umami (self-hosted)** | Kein Google Analytics, keine Drittland-Übermittlung |
| Zahlungen | **Stripe** mit EU-Entität | Standard, gute SCA-Unterstützung |
| Tests | **Kotest/JUnit5 + MockK** (Backend), Vitest + Playwright (Frontend), Testcontainers (DB-Integration) | Unit/Integration/E2E über beide Schichten |

### 10.2 Warum PWA und nicht native App

- **Ein Codebase, zwei Plattformen.** Bei einem kleinen Team ist das entscheidend.
- **Keine App-Store-Provision** (15–30 %) auf Abos. Bei 29,99 €/Jahr sind das 4,50–9 € pro Nutzer und Jahr — direkt in der Marge.
- **Kein Review-Prozess**, sofortiges Deployment.
- Alle benötigten Web-APIs sind 2026 verfügbar: Kamera (`getUserMedia`), Barcode (`BarcodeDetector`), Wake Lock, Web Push (auch iOS seit 16.4), IndexedDB, WASM für MediaPipe.

**Grenzen, die man kennen muss:** Apple Health und Google Fit lassen sich aus dem Browser nicht direkt anbinden. Dafür braucht es entweder einen dünnen nativen Wrapper (Capacitor) oder den Umweg über Drittanbieter-APIs (Garmin Connect, Fitbit, Withings haben Web-APIs). **Empfehlung:** PWA zuerst, Capacitor-Wrapper in V1 nachschieben, sobald Health-Sync ein echter Konversionstreiber wird.

### 10.3 Architekturskizze

```
┌────────────────────────────────────────────────────────────┐
│  Browser (PWA)                                             │
│                                                            │
│  React 19 / Next.js App Router                             │
│  ├── Service Worker (App-Shell, Offline-Routing)           │
│  ├── Dexie / IndexedDB (Log-Queue, Food-Cache, Programme)  │
│  ├── MediaPipe Pose WASM  ← Video verlässt das Gerät nie   │
│  └── BarcodeDetector / zxing-wasm                          │
└──────────────────┬─────────────────────────────────────────┘
                   │ HTTPS, REST (OpenAPI)
┌──────────────────▼─────────────────────────────────────────┐
│  Spring Boot Server (Kotlin, Hetzner Falkenstein)           │
│  ├── Auth / Session                                        │
│  ├── Domain Services                                       │
│  │   ├── NutritionTargetService  (BMR, TDEE, Makros)       │
│  │   ├── ProgramGenerator        (Volumen, Split, Auswahl) │
│  │   ├── ProgressionEngine       (Doppelte Progression)    │
│  │   ├── FitScoreService         (Kaufberatung)            │
│  │   └── AdaptiveTdeeService     (Nightly Job)             │
│  ├── Sync-Endpunkte                                        │
│  └── Ingestion-Jobs (BLS / USDA / OFF Import + Merge)      │
└──────┬───────────────────┬──────────────────┬──────────────┘
       │                   │                  │
┌──────▼──────┐   ┌────────▼────────┐  ┌──────▼──────────┐
│ PostgreSQL  │   │ Object Storage  │  │ Externe APIs    │
│ + pg_trgm   │   │ (Videos/Bilder) │  │ OFF, USDA,      │
│ + pgvector  │   │ Hetzner S3      │  │ LLM (EU),       │
│             │   │ → BunnyCDN      │  │ Garmin/Fitbit   │
└─────────────┘   └─────────────────┘  └─────────────────┘
```

### 10.4 Datenbank-Aufbau-Pipeline

Der entscheidende Aufwand liegt nicht im Code, sondern im **Datenbank-Aufbau**. Das ist der eigentliche Burggraben.

```
1. BLS 4.0 importieren        → ~7.000 kuratierte Grundnahrungsmittel, DE, Laborwerte
2. USDA FoodData Central      → ~250.000 Einträge, EN, Foundation + Branded
3. Open Food Facts (DE + EU)  → ~2,5 Mio. Produkte mit Barcode, Nutri-Score, NOVA
4. Deduplizieren:
   - Barcode ist der stärkste Schlüssel
   - Fuzzy-Matching auf (Marke, Name) mit trigram similarity
   - Bei Konflikt gewinnt die höhere Vertrauensstufe
5. Plausibilitätsprüfung (automatisch aussortieren):
   - Atwater-Check: |kcal − (4·P + 9·F + 4·K)| > 15 %  → flag
   - Summe Makros pro 100 g > 100 g                     → flag
   - Nährwerte = 0 bei essbarem Produkt                 → flag
   - Ausreißer > 3 σ innerhalb der Produktkategorie     → flag
6. Übersetzungslayer: fehlende DE/EN-Namen per LLM ergänzen, Top-5.000 manuell prüfen
7. Portionsgrößen ergänzen (der größte Genauigkeitshebel überhaupt)
8. Vertrauensstufe setzen: verified | community | estimated
9. Nightly Delta-Import, wöchentliche Re-Validierung
```

**Ein Hinweis zum Rechtlichen dieser Pipeline:** Open Food Facts steht unter der **ODbL**. Die Lizenz erlaubt kommerzielle Nutzung, verlangt aber Namensnennung **und Share-Alike** — wenn OFF-Daten mit anderen Datenbanken zu einer neuen Datenbank kombiniert werden, muss die resultierende Datenbank ebenfalls als Open Data veröffentlicht werden. Produktbilder stehen unter CC-BY-SA.

**Konsequenz für die Architektur:** OFF-Daten dürfen **nicht** in dieselbe Tabelle wie proprietäre Daten gemerged werden. Wir halten sie in einer logisch getrennten Partition (`source = 'off'`) und führen sie erst zur Laufzeit in der Antwort zusammen. Der ODbL-pflichtige Teil wird als eigener Datensatz veröffentlicht (samt unserer Korrekturen — was ohnehin gute Community-Politik ist). **Das ist eine Frage, die vor dem Launch anwaltlich geprüft werden muss.**

---

## 11. Datenquellen, Lizenzen und Kosten

### 11.1 Lebensmitteldaten

| Quelle | Umfang | Lizenz | Kosten | Rolle bei uns |
|---|---|---|---|---|
| **BLS 4.0** (Max Rubner-Institut) | ~7.000 deutsche Lebensmittel, Laborwerte, umfangreiche Mikronährstoffe | Seit v4.0 **lizenzfrei** — die früheren Lizenzgebühren sind entfallen | **0 €** | **Kuratierte DE-Basis.** Der strategische Vorteil im DACH-Markt. Prüfen: exakte Nutzungsbedingungen von blsdb.de vor Produktivnutzung. |
| **USDA FoodData Central** | ~250.000 Einträge (Foundation, SR Legacy, Branded), monatliche Updates | Public Domain (US-Regierungswerk) | **0 €** (API-Key nötig) | EN-Basis, Referenzwerte |
| **Open Food Facts** | 2,5 Mio.+ Produkte mit Barcode, Nutri-Score, NOVA, Zusatzstoffe, Bilder | **ODbL** (Attribution + Share-Alike), Bilder CC-BY-SA | **0 €** | **Barcode-Abdeckung.** Getrennte Partition wegen Share-Alike. |
| **Nutritionix** | 1,9 Mio. Items, NLP, Restaurantdaten | Kommerziell | Enterprise ab ~1.850 $/Monat | Zu teuer für den Start; ggf. später für Restaurantdaten |
| **Edamam** | 615.000+ UPCs, 10.000 generische Lebensmittel | Kommerziell | ab ~299 $/Monat (Analysis-API), bis 999 $/Monat | Optional als Lückenfüller |
| **FatSecret Platform** | Groß, gute EU-Abdeckung | Kommerziell | Auf Anfrage | Ernstzunehmende Option, wenn OFF-Qualität nicht reicht |
| **Eigene verifizierte Marken-Layer** | Top-2.000 DE-Produkte, manuell vom Etikett erfasst | Eigen | ~2 Personenmonate | **Der eigentliche Burggraben.** Deckt 80 % der realen Scans ab. |

> **Strategische Empfehlung:** Start mit BLS + USDA + OFF (Gesamtkosten 0 €) plus einer manuell gepflegten Verified-Layer für die 2.000 meistgekauften deutschen Produkte. Das schlägt MFPs Datenqualität sofort, kostet nur Arbeitszeit, und die kommerziellen APIs bleiben als Option, falls Lücken auftauchen. Die `barcode_scans`-Tabelle mit `found = false` sagt uns datengetrieben, welche Produkte als Nächstes erfasst werden müssen.

### 11.2 Übungsdaten

| Quelle | Umfang | Lizenz | Kosten | Bewertung |
|---|---|---|---|---|
| **wger** | Open-Source-Übungsdatenbank, REST-API, selbst hostbar | Open Source (AGPL/CC) | 0 € | Gute Metadaten-Basis. **Keine animierten GIFs**, teils statische Bilder. Lizenz pro Asset prüfen. |
| **ExerciseDB** | 1.300–11.000+ Übungen mit GIFs, Videos, Anleitungen | Uneinheitlich je nach Fork | 0 € bis kostenpflichtig | Endpunkte laut Anbieter „nur zur Exploration, nicht für Produktion empfohlen" — harte Ratelimits, instabil. **Nicht als Produktivabhängigkeit geeignet.** |
| **WorkoutX** | 1.400+ Übungen mit gehosteten GIFs | Kommerziell, Free-Tier 500 Anfragen/Monat | Gestaffelt | Brauchbar als Übergangslösung |
| **Eigene Produktion** | 120–150 Übungen, Video Front + Seite, 4K, Studio | Eigen, volle Rechte | ~8.000–15.000 € (2 Drehtage, Model, Schnitt, Studio) | **Empfehlung.** Siehe unten. |

> **Strategische Empfehlung: Videos selbst produzieren.** Drei Gründe: (1) Die Lizenzlage bei den GIF-Anbietern ist unklar bis riskant, und Bewegungsvermittlung ist unser Kernversprechen — darauf kann keine wackelige Abhängigkeit gebaut werden. (2) Zwei-Perspektiven-Aufnahmen mit konsistenter Kameraposition gibt es nirgends von der Stange, sie sind aber didaktisch entscheidend. (3) Für die Formanalyse (V2) brauchen wir Referenz-Gelenkbahnen aus denselben Videos. **120 Übungen decken über 95 % aller realen Trainingspläne ab** — das ist an zwei Drehtagen machbar. Für den MVP genügen 60 Übungen.

### 11.3 Laufende Betriebskosten (Schätzung)

| Posten | 1.000 MAU | 10.000 MAU | 100.000 MAU |
|---|---|---|---|
| Hetzner Cloud (App + DB) | 30 € | 120 € | 700 € |
| Object Storage + CDN (Video) | 10 € | 60 € | 450 € |
| LLM-API (Foto-Erkennung, ~30 % Nutzung) | 25 € | 220 € | 2.000 € |
| E-Mail (transaktional) | 10 € | 30 € | 150 € |
| Sentry + Analytics (self-hosted) | 15 € | 25 € | 90 € |
| Stripe-Gebühren (bei 4 % Conversion, 30 €/Jahr) | ~4 € | ~40 € | ~400 € |
| **Summe/Monat** | **~95 €** | **~495 €** | **~3.790 €** |
| **Umsatz bei 4 % Conversion à 30 €/Jahr** | 100 € | 1.000 € | 10.000 € |

Die Foto-KI ist der einzige Posten, der nicht linear skaliert werden muss — sie kann im Free-Tier auf 3 Scans/Monat begrenzt und im Paid-Tier großzügig gehalten werden, ohne das Kernversprechen (Barcode gratis) zu verletzen.

---

## 12. Bewegungsvermittlung für Anfänger

Das ist die Anforderung, die am wenigsten technisch und am meisten redaktionell ist — und genau deshalb die schwerste zu kopieren.

### 12.1 Das Problem konkret

Eine Anfängerin öffnet Hevy. Da steht: „Romanian Deadlift — 3×10". Sie weiß nicht, was das ist, wo das Gerät steht, wie schwer sie anfangen soll, wie die Bewegung aussieht, worauf sie achten muss, und ob es normal ist, dass es am nächsten Tag im Rücken zieht. Sie geht nicht wieder hin.

**Die Retention-Krise der Fitness-Apps ist zu großen Teilen ein Kompetenz-Problem, kein Motivations-Problem.** Alle Apps lösen Motivation (Streaks, Badges, Social). Keine löst Kompetenz.

### 12.2 Die vier Ebenen der Vermittlung

#### Ebene 1 — Verstehen (vor der Einheit)

Jede Übung hat eine Detailseite mit:

| Element | Umsetzung |
|---|---|
| **Video Front + Seite** | 8–12 Sekunden, Loop, ohne Ton verständlich, verlangsamte Version zuschaltbar |
| **Zielmuskulatur** | Interaktive Körpergrafik, primär rot / sekundär orange |
| **Aufbau-Schritte** | „1. Bank auf 30° einstellen. 2. Füße flach auf den Boden. 3. …" — inklusive Geräteeinstellung |
| **Ausführungs-Schritte** | Nummeriert, ein Satz pro Schritt, jeder Schritt mit Videozeitmarke verknüpft |
| **3–5 Cues** | Kurze Merksätze: „Brust raus." „Knie nach außen drücken." „Schulterblätter zusammen." Cues sind wirksamer als lange Erklärungen. |
| **Atmung** | „Einatmen beim Absenken, ausatmen beim Drücken." |
| **Tempo** | „2 Sekunden runter, kurz halten, 1 Sekunde hoch." |
| **Häufige Fehler** | 3–5 Stück, je mit Foto (richtig/falsch nebeneinander), Erklärung *warum* es schadet, und konkreter Korrektur |
| **Startgewicht-Empfehlung** | Aus Geschlecht, Körpergewicht, Erfahrung: „Fang mit der leeren Stange an" / „Frauen mit deinem Gewicht starten meist bei 15–20 kg" |
| **„Was ist normal?"** | „Muskelkater 1–2 Tage danach ist normal. Stechender Schmerz im Gelenk ist es nicht — dann abbrechen." |

#### Ebene 2 — Anleiten (während der Einheit)

- **Anfängermodus ist Standard** für alle mit Erfahrung „none" oder „beginner". Vor der ersten Ausführung einer Übung wird die Anleitung automatisch eingeblendet — nicht hinter einem Info-Icon versteckt.
- Während des Satzes: die zwei wichtigsten Cues bleiben groß auf dem Bildschirm sichtbar.
- Nach dem ersten Satz einer neuen Übung: „Wie hat es sich angefühlt?" → *Zu leicht / Genau richtig / Zu schwer / Hat wehgetan*. Bei „Hat wehgetan" → Übung sofort gegen eine gelenkschonende Alternative tauschen, keine Diskussion.
- Der **Studio-Guide**: Kurze Erklärungen für die Dinge, über die niemand redet — wie man Scheiben auflegt und sichert, wie man eine Bank einstellt, wie man höflich fragt, ob man mit rankann, was „Ich mach noch einen Satz" bedeutet. Diese unausgesprochenen Hürden sind für Anfänger real.

#### Ebene 3 — Korrigieren (kamerabasiert, V2)

Technisch umsetzbar und vollständig datenschutzkonform, weil **MediaPipe Pose als WASM-Modul komplett im Browser läuft** — der Videostream verlässt das Gerät nie.

**Wie es funktioniert:**

```
1. Nutzer stellt das Handy seitlich auf, startet den Modus explizit
2. MediaPipe Pose liefert 33 Landmarks pro Frame, ~25 fps auf Mittelklasse-Hardware
3. Aus den Landmarks werden Gelenkwinkel berechnet (Hüfte, Knie, Sprunggelenk,
   Schulter, Ellbogen) sowie die Bahn eines Referenzpunkts über die Zeit
4. Wiederholungszählung über Nulldurchgänge des Hauptgelenkwinkels
   (Berichte aus der Praxis nennen ~95 % Zählgenauigkeit)
5. Regelprüfung gegen exercise_form_rules:
   - Kniebeuge: Knievalgus  → Abstand Knie/Knöchel-Verhältnis unter Schwelle
   - Kniebeuge: Tiefe       → Hüftwinkel im tiefsten Punkt
   - Kreuzheben: Rundrücken → Winkel Schulter–Hüfte–Knie
   - Bankdrücken: Ellbogen  → Winkel Oberarm zum Rumpf
6. Feedback: visuell (Skelett färbt sich) + akustisch ("Knie nach außen")
7. Nach dem Satz: Form-Score + die eine wichtigste Sache zum Verbessern
```

**Grenzen, die die App ehrlich kommunizieren muss:**
- Funktioniert nur bei guter Beleuchtung, freier Sicht auf den ganzen Körper und seitlicher Kameraposition.
- 2D-Pose-Estimation kann Rotationen und Tiefenverschiebungen nur begrenzt erfassen.
- **Ersetzt keinen Trainer.** Das muss auf dem Startbildschirm des Modus stehen, nicht im Kleingedruckten. Es ist ein Hinweisgeber für grobe Fehler, keine biomechanische Analyse.
- Start mit **5 Übungen** (Kniebeuge, Kreuzheben, Bankdrücken, Schulterdrücken, Liegestütz), sauber validiert. Lieber wenige Übungen die funktionieren als viele die falsch korrigieren — Fehlkorrektur ist schlimmer als keine Korrektur.

**Alternative, deutlich günstigere Variante für V1:** *Selbstaufnahme mit Overlay.* Der Nutzer filmt sich, die App legt die erkannte Gelenkspur über das Video und zeigt daneben die Referenzbahn aus dem Lehrvideo. Kein Echtzeit-Feedback, keine Regelvalidierung, kein Fehlkorrektur-Risiko — aber der Aha-Effekt ist fast genauso groß, weil die meisten Anfänger ihre eigene Ausführung noch nie gesehen haben.

#### Ebene 4 — Steigern (über Wochen)

Progressionsleitern pro Bewegungsmuster. Der Anfänger startet nicht bei der Endform, sondern auf einer Stufe, die er heute schon beherrscht:

| Muster | Progressionsleiter |
|---|---|
| Kniebeuge | Box Squat → Goblet Squat → Front Squat → Back Squat |
| Hinge | Hip Hinge mit Stab → Romanian Deadlift Kurzhantel → RDL Langhantel → Kreuzheben |
| Drücken horizontal | Liegestütz an der Wand → Liegestütz erhöht → Liegestütz → Bankdrücken Kurzhantel → Bankdrücken |
| Ziehen vertikal | Latzug → Klimmzug mit Band → Negativ-Klimmzug → Klimmzug |
| Drücken vertikal | Schulterdrücken sitzend Maschine → Kurzhantel sitzend → Kurzhantel stehend → Langhantel |

Aufstieg zur nächsten Stufe wird vorgeschlagen, wenn die aktuelle Stufe über 2 Einheiten mit sauberer Form und RIR ≥ 2 im oberen Wiederholungsbereich absolviert wurde.

### 12.3 Redaktioneller Aufwand

| Position | Umfang | Aufwand |
|---|---|---|
| Videoproduktion | 120 Übungen × 2 Perspektiven | 2 Drehtage + Schnitt, 8.000–15.000 € |
| Textanleitungen DE + EN | 120 × ~400 Wörter × 2 Sprachen | ~4 Wochen (Fachautor mit Trainerlizenz) |
| Fehlerfotos | 120 × 3–5 Fehler, richtig/falsch | Am Drehtag mitproduzieren |
| Fachliche Prüfung | Alles | Sportwissenschaftler / Physiotherapeut, ~1 Woche |
| Formanalyse-Regeln | 5 Übungen initial | ~2 Wochen Entwicklung + Validierung |

**Das ist der teuerste Teil des Produkts — und genau deshalb der wertvollste.** Ein Wettbewerber kann unseren Algorithmus in zwei Wochen nachbauen. Die Bewegungsbibliothek kostet ihn Monate und echtes Fachwissen.

---

## 13. Internationalisierung DE/EN

| Aspekt | Umsetzung |
|---|---|
| Bibliothek | `next-intl`, Nachrichtenkataloge als JSON, ICU MessageFormat für Plurale und Genus |
| Routing | `/de/...` und `/en/...`, `hreflang`-Tags, Spracherkennung über `Accept-Language` mit manueller Überschreibung |
| Inhaltsdaten | Zweisprachige Spalten (`name_de`, `name_en`) statt separater Übersetzungstabelle — bei genau zwei Sprachen ist das einfacher und schneller |
| Einheiten | kg/lb, cm/ft-in, ml/fl oz, kJ/kcal — Umschaltung im Profil, konsistent überall |
| Zahlen und Daten | `Intl.NumberFormat` / `Intl.DateTimeFormat`, Wochenstart Montag (DE) vs. Sonntag (EN-US) |
| Fachbegriffe | Konsequentes Glossar. „Wiederholungen" nicht „Reps", „Sätze" nicht „Sets", aber „RIR" bleibt (etablierter Fachbegriff). Ein Glossar-Dokument für alle Autoren. |
| Lebensmittelnamen | Wo BLS/USDA keine Übersetzung liefert: LLM-Übersetzung, Top-5.000 manuell geprüft, Rest als `estimated` markiert |
| Videos | Untertitel DE/EN als WebVTT, Sprecher-Tonspur optional zweisprachig |
| Rechtstexte | Getrennt verfasst, nicht übersetzt — DE-AGB/Datenschutz nach deutschem Recht, EN-Version für EU-Ausland |
| Erweiterbarkeit | Struktur so anlegen, dass eine dritte Sprache (ES/FR/TR) ohne Schemaänderung dazukommt: `name_i18n jsonb` als Migrationsziel vorsehen |

---

## 14. Recht und Compliance

> **Wichtiger Hinweis:** Ich bin kein Anwalt, und das Folgende ersetzt keine Rechtsberatung. Die Punkte hier sind eine Arbeitsgrundlage für ein Gespräch mit einer auf Digital Health und Datenschutz spezialisierten Kanzlei. Bei einer Gesundheits-App mit Minderjährigen-Nähe, Gesundheitsdaten und KI ist diese Beratung vor dem Launch nicht optional.

### 14.1 DSGVO — Gesundheitsdaten

Von Fitness-Apps, Wearables und Smartwatches erfasste Daten sind **Gesundheitsdaten** im Sinne von Art. 4 Nr. 15 DSGVO und gehören damit zu den besonderen Kategorien nach Art. 9 Abs. 1 DSGVO. Für diese gilt ein grundsätzliches **Verarbeitungsverbot** mit Erlaubnisvorbehalt. In der Praxis ist die Rechtsgrundlage die **ausdrückliche Einwilligung nach Art. 9 Abs. 2 lit. a DSGVO**, die sich explizit auf die Verarbeitung von Gesundheitsdaten für einen festgelegten Zweck beziehen muss.

**Konkrete Anforderungen:**

| Anforderung | Umsetzung |
|---|---|
| Ausdrückliche Einwilligung | Eigener Schritt im Onboarding, nicht in den AGB versteckt, nicht vorangekreuzt, zweckgebunden formuliert, jederzeit widerrufbar |
| Granularität | Getrennte Einwilligungen für: Kernfunktion · Foto-KI-Verarbeitung · Wearable-Sync · anonymisierte Produktanalyse · Marketing. Ablehnung einzelner Zwecke darf die Kernfunktion nicht blockieren |
| Zweckbindung | Gesundheitsdaten **nie** für Werbung, nie an Dritte, nie zum Training externer Modelle |
| Datenminimierung | Nur erheben, was für die berechnete Empfehlung nötig ist. Keine Kontaktlisten, keine Standortdaten, kein Adressbuch |
| Auskunft (Art. 15) und Portabilität (Art. 20) | Vollständiger Export als JSON und CSV, in der App, ohne Support-Ticket, kostenlos, auch im Free-Tier |
| Löschung (Art. 17) | Ein Klick, 30 Tage Karenz mit Wiederherstellungsmöglichkeit, danach harte Löschung inkl. Backups |
| Speicherbegrenzung | Inaktive Konten nach 24 Monaten anonymisieren, mit vorheriger Ankündigung |
| Auftragsverarbeiter | AV-Verträge mit Hosting, E-Mail, Zahlungsanbieter, LLM-Anbieter |
| Drittlandtransfer | Vermeiden. Wenn LLM-Anbieter außerhalb der EU: Standardvertragsklauseln + Transfer Impact Assessment, und **keine identifizierbaren Gesundheitsdaten** im Prompt |
| DSFA (Art. 35) | **Erforderlich.** Umfangreiche Verarbeitung besonderer Kategorien plus automatisierte Empfehlungen. Vor Launch durchführen und dokumentieren |
| Datenschutzbeauftragter | Bei umfangreicher Verarbeitung besonderer Kategorien nach Art. 37 Abs. 1 lit. c zu prüfen — im Zweifel bestellen |
| Verzeichnis von Verarbeitungstätigkeiten | Art. 30, ab Tag 1 pflegen |
| Meldepflicht | Prozess für Art. 33/34 (72 Stunden) vorbereiten, nicht erst im Ernstfall |

### 14.2 Medizinprodukt oder nicht? (MDR)

Die Abgrenzung ist die wichtigste regulatorische Weichenstellung. Eine Wellness-App richtet sich an gesunde Nutzer zur Förderung des Wohlbefindens und unterliegt kaum regulatorischen Anforderungen. Sobald eine App zur **Diagnose, Prävention, Überwachung oder Behandlung von Krankheiten oder Verletzungen** dient, fällt sie voraussichtlich unter die MDR und braucht eine Zertifizierung als Medizinprodukt.

**OnShape positioniert sich eindeutig als Wellness-/Lifestyle-App.** Das erfordert Disziplin im Produkt und im Marketing:

| Erlaubt | Nicht erlaubt |
|---|---|
| „Dein Kalorienziel für dein Wunschgewicht" | „Therapie bei Adipositas" |
| „Trainingsplan für Muskelaufbau" | „Rehabilitationsprogramm nach Bandscheibenvorfall" |
| „Hinweis: Achte auf deine Proteinzufuhr" | „Du hast einen Eisenmangel" |
| „Bei Schmerzen: Übung abbrechen, Arzt fragen" | „Deine Schulterschmerzen kommen von X" |
| „Nährstoffübersicht" | „Diagnose einer Mangelernährung" |
| „Für gesunde Erwachsene ab 16" | Nutzung durch Patienten mit Diagnosen bewerben |

**Regeln, die im Code verankert sein müssen:**
- Keine Diagnose-Formulierungen, nirgends. Auch nicht in KI-generierten Texten — deshalb ein Ausgabefilter auf allen LLM-Antworten.
- Keine Werbung mit Krankheitsbezug (Diabetes, Adipositas als Krankheit, PCOS, Hypertonie).
- Kein Zielgruppen-Targeting auf Patientengruppen.
- Feature-Sperre: Nutzer, die im Screening Herzerkrankungen, Schwangerschaft oder akute Verletzungen angeben, bekommen einen Hinweis auf ärztliche Rücksprache — aber keine „Anpassung der Therapie".

**Grauzonen, die vor Launch geklärt werden müssen:** Die kamerabasierte Formanalyse ist der riskanteste Punkt. Solange sie als „Feedback zur Bewegungsqualität im Training" positioniert ist und nicht als „Verletzungsprävention" oder „Haltungsanalyse", bleibt sie im Wellness-Bereich. Formulierungen wie „erkennt Fehlhaltungen" sind grenzwertig und sollten vermieden werden.

### 14.3 EU AI Act

Die Klassifizierung nach AI Act und MDR/IVDR basiert auf unterschiedlichen Kriterien, auch wenn die Begriffe teils ähnlich sind — offene Fragen bestehen besonders bei Produkten der niedrigsten Risikoklasse und bei selbstlernenden KI-Systemen.

**Einschätzung für OnShape:** Voraussichtlich **minimales bis begrenztes Risiko**. Kein Hochrisiko-Anwendungsfall (keine Biometrie zur Identifizierung, keine Kreditwürdigkeit, kein Beschäftigungskontext, kein Medizinprodukt). Es gelten primär **Transparenzpflichten**.

| Pflicht | Umsetzung |
|---|---|
| Kennzeichnung von KI-Interaktion | Foto-Erkennung, Chat-Assistent und Textgenerierung sind sichtbar als KI markiert |
| Erklärbarkeit | Jede berechnete Empfehlung ist aufklappbar und zeigt ihre Herleitung (`/targets/explain`) |
| Menschliche Überschreibbarkeit | Jedes Ziel, jeder Plan, jede Empfehlung ist manuell änderbar |
| Genauigkeitsangaben | Konfidenzintervalle bei Foto-Schätzungen, dokumentierte Fehlerbereiche bei BMR-Formeln |
| Keine manipulativen Techniken | Verbot dunkler Muster: keine Fake-Dringlichkeit, keine Schuldgefühl-Benachrichtigungen, keine versteckten Preise (Art. 5 verbietet manipulative Techniken, die Schaden verursachen können) |
| Dokumentation | Modellkarten für Foto-KI, Versionierung der Algorithmen, Änderungsprotokoll |

> **Das Verbot manipulativer Praktiken ist bei einer Gesundheits-App besonders relevant.** Streak-Mechaniken, die Schuld erzeugen, oder Benachrichtigungen wie „Du hast heute noch nichts gegessen — dein Streak ist in Gefahr" sind in diesem Kontext nicht nur ethisch fragwürdig, sondern potenziell rechtlich angreifbar.

### 14.4 Weitere Regelwerke

| Thema | Anforderung |
|---|---|
| **Health Claims (VO 1924/2006)** | Keine unzulässigen gesundheitsbezogenen Angaben zu Lebensmitteln. Nutri-Score darf angezeigt, aber nicht als Gesundheitsversprechen interpretiert werden |
| **LMIV (VO 1169/2011)** | 14 Allergene korrekt kennzeichnen; wir geben Herstellerdaten wieder und weisen darauf hin, dass die Verpackung maßgeblich ist |
| **Verbraucherrecht / Abos** | 14 Tage Widerruf, Kündigungsbutton nach § 312k BGB, klare Preisangaben vor Vertragsschluss, keine automatische Verlängerung ohne Hinweis |
| **Jugendschutz** | Altersgrenze 16 Jahre. Bei 16–18: keine Defizit-Ziele, keine Gewichtsziele, nur Trainings- und Gesundheitsfunktionen |
| **Barrierefreiheitsstärkungsgesetz (BFSG)** | Seit 28.06.2025 in Kraft für B2C-Dienstleistungen. WCAG 2.2 AA ist damit nicht nur gute Praxis, sondern rechtlich relevant |
| **TDDDG (ehem. TTDSG)** | Cookie-/Consent-Banner nur wo nötig; bei rein technisch notwendigen Cookies und self-hosted Analytics ohne Personenbezug entfällt es weitgehend |
| **Urheberrecht** | Trainingsprogramm-Namen wie „5/3/1" sind teils markenrechtlich geschützt. Struktur nachbauen ist zulässig, Name und Text übernehmen nicht |

### 14.5 Wellbeing-Schutzmechanismen

Kalorienzähl-Apps stehen im begründeten Verdacht, gestörtes Essverhalten zu begünstigen. Das ist gleichzeitig ein ethisches, ein regulatorisches und ein Reputationsrisiko. Diese Mechanismen sind daher **Produktanforderungen, nicht Nice-to-have**:

| Mechanismus | Umsetzung |
|---|---|
| Harte Kaloriengrenzen | Nie unter 1.200 kcal (♀) / 1.500 kcal (♂), nie unter BMR × 1,1 — nicht überschreibbar |
| BMI-Grenze | Zielgewicht unter BMI 18,5 nicht wählbar |
| Ratengrenzen | Max. 1 % Körpergewicht Verlust pro Woche |
| Muster-Erkennung | Bei wiederholt extrem niedriger Zufuhr, häufigem Ziel-Herunterschrauben oder exzessivem Training: Kalorienanzeige ausblenden, wohlwollender Hinweis, Angebot einer Ressourcenliste (Bundesfachverband Essstörungen, BZgA-Beratungstelefon) |
| Sprache | Keine Wertung von Lebensmitteln als „gut"/„schlecht", „Cheat Meal", „sündigen", „verbrannt" |
| Keine Bestrafung | Kein Streak-Verlust bei Überschreitung, keine roten Warnfarben bei Zielüberschreitung, keine Schuld-Benachrichtigungen |
| Wochen- statt Tagesfokus | Erfolg wird über die Woche gemessen, nicht über den einzelnen Tag |
| Pausenmodus | „Ich möchte eine Weile nicht tracken" — ein Klick, keine Rückgewinnungs-Kampagne, Daten bleiben erhalten |
| Kein Körpervergleich | Keine öffentlichen Vorher-Nachher-Feeds, keine Ranglisten nach Gewicht oder Körperfett |

---

## 15. Geschäftsmodell und Preise

### 15.1 Preisstruktur

| | **Free** | **Plus** | **Coach** |
|---|---|---|---|
| **Preis** | 0 € | **3,99 €/Mon. · 29,99 €/Jahr** | **7,99 €/Mon. · 69,99 €/Jahr** |
| Lebensmittel-Logging | **unbegrenzt** | unbegrenzt | unbegrenzt |
| **Barcode-Scanner** | **unbegrenzt** | unbegrenzt | unbegrenzt |
| Fit-Score & Kaufberatung | 10 Scans/Monat | unbegrenzt | unbegrenzt |
| Makro-Tracking | ✓ | ✓ | ✓ |
| Mikronährstoffe | Basis (5) | vollständig | vollständig |
| Trainingsplan-Generator | 1 aktiver Plan | unbegrenzt | unbegrenzt |
| Übungsvideos & Anleitungen | ✓ vollständig | ✓ | ✓ |
| Trainings-Logging | unbegrenzt | unbegrenzt | unbegrenzt |
| Volumen-Analytics | Basis | vollständig | vollständig |
| Adaptives TDEE | — | ✓ | ✓ |
| Foto-KI-Erkennung | 5/Monat | 100/Monat | unbegrenzt |
| Rezept-Import | — | ✓ | ✓ |
| Wearable-Sync | — | ✓ | ✓ |
| Formanalyse (Kamera) | — | ✓ | ✓ |
| Datenexport | ✓ | ✓ | ✓ |
| Wochenbericht | — | ✓ | ✓ |
| KI-Coach-Chat | — | — | ✓ |
| Individuelle Periodisierung | — | — | ✓ |
| Werbung | **keine** | keine | keine |

**Lifetime-Option:** 129 € (nach Hevys Vorbild — starker Konversionstreiber und Cashflow früh im Lebenszyklus, sollte aber gedeckelt werden, z. B. erste 5.000 Nutzer).

### 15.2 Preisbegründung

| App | Jahrespreis | OnShape Plus (29,99 €) |
|---|---|---|
| MyFitnessPal Premium | 79,99 $ (~74 €) | **60 % günstiger** |
| MyFitnessPal Premium+ | 99,99 $ (~92 €) | 67 % günstiger |
| Fitbod | 95,99 $ (~88 €) | 66 % günstiger |
| Yazio Pro | 36–84 € | vergleichbar bis günstiger |
| Cal AI | bis ~29,99 $/Jahr, intransparent | vergleichbar, aber transparent |
| Hevy Pro | 23,99 $ (~22 €) | etwas teurer, dafür Ernährung inklusive |
| **MFP + Hevy zusammen** | **~96 €** | **69 % günstiger für mehr Funktionsumfang** |

Die entscheidende Botschaft: **Ein Abo statt zwei, für ein Drittel des Preises.**

### 15.3 Warum kostenloser Barcode-Scanner wirtschaftlich funktioniert

Auf den ersten Blick verschenken wir das Feature, für das MFP 79,99 $ verlangt. Tatsächlich:

- **Die Grenzkosten sind praktisch null.** Der Scan läuft im Browser, die Datenbankabfrage kostet Bruchteile eines Cents. MFP monetarisiert hier nicht Kosten, sondern eine erzwungene Abhängigkeit.
- **Es ist unser stärkstes Akquiseargument.** „Der Barcode-Scanner, den MyFitnessPal dir weggenommen hat — bei uns für immer kostenlos" ist eine Botschaft, die sich von selbst verbreitet und exakt die frustrierteste Nutzergruppe des Marktführers adressiert.
- **Der Fit-Score ist der eigentliche Wert** — und der ist limitiert (10 Scans/Monat im Free-Tier). Wir verschenken die Funktion, nicht die Intelligenz.
- **Jeder Scan liefert uns Daten.** `barcode_scans` mit `found = false` zeigt uns exakt, welche Produkte wir als Nächstes verifizieren müssen. Free-Nutzer bauen unseren Burggraben mit.

### 15.4 Wachstumskanäle

| Kanal | Ansatz |
|---|---|
| **Wechsler-Kampagne** | Ein-Klick-Import aus MFP, Yazio, Hevy, Strong. Landing Pages gezielt auf die Beschwerden ausgerichtet („MyFitnessPal Barcode wieder kostenlos") |
| **SEO** | Deutschsprachige Inhalte zu Übungsausführung, Kalorienbedarf, Lebensmittelvergleichen. Die Übungsdatenbank ist ein natürlicher SEO-Asset mit hunderten Longtail-Seiten |
| **Rechner-Tools ohne Anmeldung** | Kalorienbedarf, Makro-, 1RM-, Volumen-Rechner — frei zugänglich, hohes Suchvolumen, natürlicher Funnel |
| **Fitnessstudios / Trainer** | B2B2C: Studios geben ihren Mitgliedern Plus-Zugang, Trainer nutzen die Plandokumentation |
| **Community** | Offener Änderungsverlauf, Feature-Voting, Beitrag zur Lebensmitteldatenbank mit Anerkennung |
| **App Stores** | PWA über PWABuilder als TWA in den Play Store; iOS über Capacitor-Wrapper — dort dann aber mit App-Store-Provision, daher primär Web-Abschluss bewerben |

---

## 16. Roadmap

### Phase 0 — Fundament (Wochen 1–4)

- [ ] Repository, CI/CD, Umgebungen (dev/staging/prod)
- [ ] Datenbankschema + Migrationen (Spring Data JPA/Exposed + Flyway)
- [ ] **Daten-Pipeline: BLS 4.0 + USDA + Open Food Facts importieren, deduplizieren, validieren**
- [ ] Auth, Profil, Onboarding-Flow
- [ ] i18n-Grundgerüst DE/EN
- [ ] Design-System, Komponentenbibliothek

### Phase 1 — MVP (Wochen 5–16)

**Ernährung**
- [ ] Lebensmittelsuche (Volltext + Trigram, < 150 ms)
- [ ] Tagebuch: Logging, Quick-Add, Multi-Select, Kopieren, Meals
- [ ] **Barcode-Scanner + Fit-Score + Alternativen**
- [ ] Kalorien-/Makroberechnung inkl. Erklärungsansicht
- [ ] Gewicht und Körpermaße

**Training**
- [ ] 60 Übungen mit Videos, Anleitungen, Cues, Fehlern (Eigenproduktion)
- [ ] Plangenerator (Volumen, Split, Übungsauswahl)
- [ ] Live-Workout-Modus mit Pausentimer
- [ ] Progression (linear + doppelt)
- [ ] Volumen-Dashboard mit gewichteter Satzzählung

**Basis**
- [ ] Offline-Fähigkeit + Sync
- [ ] Import aus MFP/Yazio/Hevy/Strong
- [ ] Datenexport, Konto löschen
- [ ] Stripe-Abrechnung, Free/Plus
- [ ] DSFA, Datenschutzerklärung, AGB (mit Anwalt)

**Meilenstein:** Geschlossene Beta mit 100 Nutzern, davon mindestens 30 echte Anfänger.

### Phase 2 — V1 (Monate 5–8)

- [ ] Foto-KI-Erkennung mit Konfidenzintervallen
- [ ] Natürlichsprachige Eingabe
- [ ] Adaptives TDEE
- [ ] Übungsbibliothek auf 120 erweitern
- [ ] Programm-Vorlagen (5/3/1-Struktur, PPL, GZCLP …)
- [ ] Wearable-Sync (Garmin, Fitbit, Withings; Apple/Google via Capacitor-Wrapper)
- [ ] Rezept-Import per URL
- [ ] Wochenberichte
- [ ] Selbstaufnahme-Modus mit Bewegungs-Overlay
- [ ] Studio-Guide für Anfänger
- [ ] Deload-Automatik

### Phase 3 — V2 (Monate 9–14)

- [ ] **Kamerabasierte Echtzeit-Formanalyse (5 Übungen)**
- [ ] KI-Coach-Chat (Coach-Tier)
- [ ] Einkaufslisten- und Regal-Vergleichsmodus
- [ ] Preis-pro-Nährstoff-Analyse
- [ ] Trainer-/Studio-Portal (B2B2C)
- [ ] Community-Funktionen (opt-in, ohne Körpervergleich)
- [ ] Dritte Sprache

### Phase 4 — Skalierung (ab Monat 15)

- [ ] Formanalyse auf 20 Übungen
- [ ] Regionale Lebensmitteldatenbanken (AT, CH, NL, ES)
- [ ] Öffentliche API für Partner
- [ ] Native Apps, falls die Konversionsdaten es rechtfertigen

---

## 17. Risiken

| Risiko | Wahrscheinlichkeit | Auswirkung | Gegenmaßnahme |
|---|---|---|---|
| **Datenbankqualität reicht nicht** — Nutzer finden ihre Produkte nicht | Hoch | Kritisch | Manuelle Verified-Layer für Top-2.000 DE-Produkte vor Launch. `barcode_scans`-Telemetrie priorisiert die Nacharbeit datengetrieben. Nutzer können Produkte selbst anlegen. |
| **ODbL-Share-Alike zwingt zur Offenlegung der eigenen Datenbank** | Mittel | Hoch | Strikte Trennung der OFF-Partition, kein Merge auf DB-Ebene, Zusammenführung erst zur Laufzeit. **Anwaltliche Prüfung vor Launch — nicht verhandelbar.** |
| **BLS-Nutzungsbedingungen enger als erwartet** | Mittel | Mittel | Vor der Implementierung die Bedingungen von blsdb.de/MRI schriftlich klären. Fallback: USDA + OFF + eigene Erfassung. |
| **Formanalyse funktioniert im echten Studio nicht** (Licht, Platz, Kamerawinkel) | Hoch | Mittel | V2, nicht MVP. Start mit 5 Übungen. Selbstaufnahme-Overlay als risikoarme Vorstufe in V1. Ehrliche Kommunikation der Grenzen. |
| **Fehlkorrektur durch Formanalyse führt zu Verletzung** | Niedrig | Kritisch | Nur grobe, gut validierte Fehler. Konservative Schwellwerte. Prominenter Hinweis „ersetzt keinen Trainer". Haftungsausschluss + Versicherung. Bei Unsicherheit lieber kein Feedback als falsches. |
| **Regulatorische Neubewertung als Medizinprodukt** | Niedrig | Kritisch | Strikte Wellness-Positionierung, Ausgabefilter auf allen generierten Texten, Marketing-Freigabeprozess, anwaltliche Begleitung |
| **Zu breiter Funktionsumfang für ein kleines Team** | Hoch | Hoch | MVP hart schneiden. Foto-KI ist V1, Formanalyse V2. Lieber Ernährung + Training + Videos exzellent als alles mittelmäßig. |
| **Videoproduktion verzögert den Launch** | Mittel | Mittel | Frühzeitig starten, parallel zur Entwicklung. Für die Beta reichen 40 Übungen. Notfalls lizenzierte Assets als Übergang, mit klarem Ausstiegsplan. |
| **MyFitnessPal macht den Barcode wieder kostenlos** | Niedrig | Mittel | Unser Vorteil ist nicht ein Feature, sondern die Integration von Training + Ernährung + Bewegungsschule. Der Barcode ist der Türöffner, nicht der Burggraben. |
| **Nutzerakquise ist teuer** | Hoch | Hoch | SEO und kostenlose Rechner-Tools statt bezahlter Anzeigen. Wechsler-Import als Reibungssenker. B2B2C über Studios. |
| **Essstörungs-Vorwurf / negative Presse** | Mittel | Hoch | Wellbeing-Guardrails ab Tag 1 sichtbar und dokumentiert, nicht nachträglich. Beratung durch Fachperson für Essstörungen. Transparente Haltung öffentlich kommunizieren. |
| **LLM-Kosten laufen aus dem Ruder** | Mittel | Mittel | Foto-KI im Free-Tier stark begrenzt. Caching identischer Anfragen. Kleineres Modell für einfache Fälle, großes nur bei niedriger Konfidenz. |

---

## 18. Quellen

### Wissenschaftliche Literatur

[1] [Comparison of predictive equations for resting metabolic rate in healthy nonobese and obese adults: a systematic review](https://consensus.app/papers/details/bff4febbc51756b6a2e590684dd14c4b/?utm_source=claude_desktop) (Frankenfield et al., 2005, 740 Zitationen, Journal of the American Dietetic Association)

[2] [Analysis of Predictive Equations for Estimating Resting Energy Expenditure in a Large Cohort of Morbidly Obese Patients](https://consensus.app/papers/details/04fe4ac853db5678a64de32b54992cfa/?utm_source=claude_desktop) (Cancello et al., 2018, 29 Zitationen, Frontiers in Endocrinology)

[3] [VALIDATION OF RESTING ENERGY EXPENDITURE EQUATIONS IN OLDER ADULTS WITH OBESITY](https://consensus.app/papers/details/54e88a3025c752129c17249188ccdf12/?utm_source=claude_desktop) (Griffith et al., 2022, Journal of Nutrition in Gerontology and Geriatrics)

[4] [Development and validation of earlier resting energy expenditure predicting equations in adults living in Tehran](https://consensus.app/papers/details/ab9eeed44dc851e18f416cbb705d17c5/?utm_source=claude_desktop) (Jalilpiran et al., 2024, Health Promotion Perspectives)

[5] [The validity of resting energy expenditure predictive equations in adults with central obesity](https://consensus.app/papers/details/c340843271925d598d86c14fb9139919/?utm_source=claude_desktop) (Pasdar et al., 2019, Nutrition and Health)

[6] [International Society of Sports Nutrition Position Stand: protein and exercise](https://consensus.app/papers/details/d37a061294705590aca659c3d19807c3/?utm_source=claude_desktop) (Jäger et al., 2017, 902 Zitationen, Journal of the ISSN)

[7] [A systematic review of dietary protein during caloric restriction in resistance trained lean athletes: a case for higher intakes](https://consensus.app/papers/details/bf17a203d21e555a951c4809f9baf9e7/?utm_source=claude_desktop) (Helms et al., 2014, 121 Zitationen, IJSNEM)

[8] [Effect of Dietary Protein on Fat-Free Mass in Energy Restricted, Resistance-Trained Individuals: An Updated Systematic Review With Meta-Regression](https://consensus.app/papers/details/83568ebbfadb54cc80ae8420e75c65b3/?utm_source=claude_desktop) (Refalo et al., 2025, Strength & Conditioning Journal)

[9] [The effects of high protein intakes during energy restriction on body composition, energy metabolism and physical performance in recreational athletes](https://consensus.app/papers/details/6f364fb29f125a9fa9865e8828f8fe10/?utm_source=claude_desktop) (Kanaan et al., 2025, European Journal of Clinical Nutrition)

[10] [Dose-response relationship between weekly resistance training volume and increases in muscle mass: A systematic review and meta-analysis](https://consensus.app/papers/details/0fec06fa365f5224b7c53cd5acdd007d/?utm_source=claude_desktop) (Schoenfeld et al., 2017, 572 Zitationen, Journal of Sports Sciences)

[11] [The Resistance Training Dose Response: Meta-Regressions Exploring the Effects of Weekly Volume and Frequency on Muscle Hypertrophy and Strength Gains](https://consensus.app/papers/details/28976bf04deb591980a56525e2ba77d1/?utm_source=claude_desktop) (Pelland et al., 2025, Sports Medicine)

[12] [The Resistance Training Dose-response: Effects Of Weekly Volume On Muscle Hypertrophy](https://consensus.app/papers/details/6941ecc0f5c458b6bdcc63882331f72c/?utm_source=claude_desktop) (Hamaïde et al., 2025, Medicine & Science in Sports & Exercise)

[13] [A Systematic Review of The Effects of Different Resistance Training Volumes on Muscle Hypertrophy](https://consensus.app/papers/details/d82bc2b70af65cea97f69cdebc6ab92a/?utm_source=claude_desktop) (Baz-Valle et al., 2022, 67 Zitationen, Journal of Human Kinetics)

[14] [Resistance Training Variables for Optimization of Muscle Hypertrophy: An Umbrella Review](https://consensus.app/papers/details/ecd39e3d22c25a63ba2d7ed3f1a3af61/?utm_source=claude_desktop) (Bernárdez-Vázquez et al., 2022, 55 Zitationen, Frontiers in Sports and Active Living)

[15] [Higher resistance training volume offsets muscle hypertrophy non-responsiveness in older individuals](https://consensus.app/papers/details/a312f1e1163257e1adc406b3c26da754/?utm_source=claude_desktop) (Lixandrão et al., 2024, Journal of Applied Physiology)

[16] [Resistance training prescription for muscle strength and hypertrophy in healthy adults: a systematic review and Bayesian network meta-analysis](https://consensus.app/papers/details/d7159f4e02555ec4bfbe3a35124223f4/?utm_source=claude_desktop) (Currier et al., 2023, 149 Zitationen, British Journal of Sports Medicine)

[17] [Resistance Training Volume Enhances Muscle Hypertrophy but Not Strength in Trained Men](https://consensus.app/papers/details/6f0d585861b55b5f96cbfefcc042de7d/?utm_source=claude_desktop) (Schoenfeld et al., 2018, 260 Zitationen, Medicine and Science in Sports and Exercise)

[18] [Systematic review and meta-analysis of protein intake to support muscle mass and function in healthy adults](https://consensus.app/papers/details/005a4aa8746d5323bc1d623cb7279c0b/?utm_source=claude_desktop) (Nunes et al., 2022, 212 Zitationen, Journal of Cachexia, Sarcopenia and Muscle)

### Wettbewerbs- und Marktrecherche

- [MyFitnessPal Review 2026 — Accuracy, Pricing, Alternatives](https://www.food-trackers.com/reviews/myfitnesspal/)
- [MyFitnessPal Alternatives 2026: Why Users Are Switching After the Redesign](https://platelens.app/blog/myfitnesspal-alternatives-2026)
- [MyFitnessPal Review 2026: The Largest Food Database, But Is It Enough?](https://calorie-trackers.com/reviews/myfitnesspal/)
- [MyFitnessPal Reviews — PissedConsumer](https://myfitnesspal.pissedconsumer.com/review.html)
- [MyFitnessPal Pricing 2026: Free vs Premium vs Premium+](https://nutriscan.app/blog/posts/myfitnesspal-pricing-2026-guide-2ff09c399a)
- [Cal AI Review 2026: Honest Pros, Cons, and Accuracy Test](https://nutrola.app/en/blog/cal-ai-review-2026)
- [Cal AI Review 2026: Accurate Enough for Weight Loss?](https://trysoma.app/blog/cal-ai-review/)
- [Why Is Cal AI So Expensive? The $200/Year Price Explained](https://nutrola.app/en/blog/why-is-cal-ai-so-expensive)
- [Cal AI Pricing 2026: Monthly vs Yearly and What Premium Unlocks](https://nutriscan.app/blog/posts/cal-ai-pricing-2026-monthly-yearly-premium-abc6e7b26f)
- [9 Beste Kalorien-Tracker-Apps in Deutschland (2026)](https://nutriscan.app/de/blog/posts/nutrition-apps-germany-review)
- [Was ist die beste Kalorienzähler-App? (2026 Vergleich)](https://nutrola.app/de/blog/was-ist-die-beste-kalorienzaehler-app)
- [YAZIO vs Lifesum vs Cronometer Free Tier 2026](https://nutrola.app/en/blog/yazio-vs-lifesum-vs-cronometer-free-tier-2026)
- [Hevy vs Strong vs Fitbod vs Jefit: Best Workout Tracker App in 2026](https://www.sensai.fit/blog/hevy-vs-strong-vs-fitbod-vs-jefit)
- [Hevy Review 2026: Pricing, Free vs Pro Limits](https://www.sensai.fit/blog/hevy-review-2026)
- [Fitbod Review 2026: New $15.99/mo Price, Verdict, and Free Alternatives](https://www.sensai.fit/blog/fitbod-review-2026)
- [Best Strength Training Apps in 2026](https://www.findyouredge.app/news/best-strength-training-apps-2026)
- [7 Best Strength Training Apps in 2026, Tested and Compared](https://askvora.com/blog/best-strength-training-apps-2026)

### Datenquellen und Technik

- [Open Food Facts — Terms of use, contribution and re-use](https://world.openfoodfacts.org/terms-of-use)
- [Open Food Facts — Data, API and SDKs](https://world.openfoodfacts.org/data)
- [Are there conditions to use the API? — Open Food Facts Support](https://support.openfoodfacts.org/help/en-gb/12-api-data-reuse/94-are-there-conditions-to-use-the-api)
- [Nährstoffdatenbank Bundeslebensmittelschlüssel (BLS) jetzt kostenlos nutzbar — LABO](https://www.labo.de/news/naehrstoffdatenbank-bundeslebensmittelschluessel--bls--jetzt-kostenlos-nutzbar.htm)
- [Bundeslebensmittelschlüssel (BLS) jetzt kostenlos nutzbar — Bundesverband der Lebensmittelkontrolleure](https://bvlk.de/bundeslebensmittelschluessel-bls-jetzt-kostenlos-nutzbar/)
- [BLS Datenbank](https://blsdb.de/)
- [Lizenzmodelle für den Bundeslebensmittelschlüssel — Max Rubner-Institut (PDF)](https://www.mri.bund.de/fileadmin/MRI/Service/BLS/Lizenzmodell_Bestellformular_BLS_3.0.pdf)
- [Top Nutrition APIs for App Developers in 2026](https://www.spikeapi.com/blog/top-nutrition-apis-for-developers-2026)
- [Open Nutrition Datasets Compared: USDA, Open Food Facts, fatsecret](https://nutrola.app/en/blog/open-nutrition-datasets-compared-usda-openfoodfacts-nutrola)
- [ExerciseDB API — GitHub](https://github.com/exercisedb/exercisedb-api)
- [WorkoutX vs ExerciseDB vs Wger: Best Exercise API in 2026](https://workoutxapp.com/blog/workoutx-vs-exercisedb.html)
- [MediaPipe Pose Estimation for Sports Apps — Deployment & Limitations](https://www.it-jim.com/blog/mediapipe-for-sports-apps/)
- [From Pixels to Physical Therapy: Real-Time Pose Correction with MediaPipe and React](https://dev.to/beck_moulton/from-pixels-to-physical-therapy-building-a-real-time-pose-correction-system-with-mediapipe-and-3p2d)
- [Rep Tracker — AI-Powered Workout Tracking in Your Browser](https://reptracker.fit/)
- [The Architecture Of Local-First Web Development — Smashing Magazine](https://www.smashingmagazine.com/2026/05/architecture-local-first-web-development/)
- [Best Offline-First Tech Stack for 2026](https://cssauthor.com/offline-first-tech-stack/)
- [Local-First Architecture for Progressive Web Apps](https://blog.openreplay.com/local-first-pwa-architecture/)

### Recht

- [Datenschutzrechtliche Anforderungen an Gesundheits-Apps — isico](https://www.isico.de/blog/datenschutzrechtlichen-anforderungen-bei-gesundheits-und-fitness-apps)
- [Gesundheitsapps — Datenschutz und Datensicherheit — SRD Rechtsanwälte](https://www.srd-rechtsanwaelte.de/blog/gesundheitsapps-datenschutz-datensicherheit)
- [DSGVO-Compliance bei Digital Health Apps — Taylor Wessing](https://www.taylorwessing.com/en/insights-and-events/insights/2021/04/dsgvo-compliance-bei-digital-health-apps)
- [Mobile Apps im Gesundheitswesen: Anforderungen aus dem Datenschutz (PDF)](https://gesundheitsdatenschutz.org/download/datenschutz_med_apps.pdf)
- [Regulatory classification of AI-enabled products for medical use on the basis of the EU AI Act and MDR/IVDR — PMC](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC12287106/)
- [Gesundheits-App entwickeln lassen: Leitfaden](https://sb-techworks.de/de/gesundheits-app-entwickeln-lassen-der-leitfaden-2026/)

---

## Anhang A — Offene Punkte vor Entwicklungsstart

| # | Punkt | Verantwortlich | Vor Phase |
|---|---|---|---|
| 1 | ODbL-Share-Alike-Auswirkung auf unsere Datenbank anwaltlich klären | Recht | 0 |
| 2 | BLS-4.0-Nutzungsbedingungen schriftlich bestätigen lassen | Recht | 0 |
| 3 | DSFA nach Art. 35 DSGVO durchführen und dokumentieren | Datenschutz | 1 |
| 4 | Bedarf eines Datenschutzbeauftragten prüfen | Recht | 1 |
| 5 | Fachliche Prüfung der Trainingsalgorithmen durch Sportwissenschaftler | Fachlich | 1 |
| 6 | Wellbeing-Guardrails mit Fachperson für Essstörungen abstimmen | Fachlich | 1 |
| 7 | Videoproduktion beauftragen (Studio, Model, Schnitt) | Produktion | 1 |
| 8 | Haftpflichtversicherung für Fitness-Beratungsprodukt klären | Recht | 1 |
| 9 | LLM-Anbieter mit EU-Verarbeitung und AV-Vertrag auswählen | Technik | 1 |
| 10 | Entscheidung PWA-only vs. Capacitor-Wrapper für Health-Sync | Technik | 2 |

---

*Ende des Dokuments · Version 1.0 · 5. August 2026*
