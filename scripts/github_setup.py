#!/usr/bin/env python3
"""
OnShape — GitHub-Backlog-Bootstrap

Erstellt aus docs/KONZEPT.md ein vollstaendiges, strukturiertes Backlog im
GitHub-Repo kia68/onShape: Milestones (Phasen 0-4), Labels (type/phase/area),
Epic-Issues (14 Bereiche) und darunter je ein Issue pro FR-/NFR-Anforderung
bzw. pro nicht-nummeriertem Arbeitspaket (Infra, Recht, Business, Scale).

Nutzung:
    python scripts/github_setup.py                 # Dry-Run: zeigt nur den Plan
    python scripts/github_setup.py --execute        # legt Labels/Milestones/Issues an
    python scripts/github_setup.py --execute --repo kia68/onShape

Voraussetzung: `gh` CLI installiert und eingeloggt (gh auth status).
Das Skript ist idempotent bei Labels/Milestones (per Name wiederverwendet),
legt bei erneutem Lauf aber neue Issues an (kein Abgleich mit bestehenden
Issues) — nicht zweimal mit --execute gegen dasselbe Repo laufen lassen,
ohne das vorher zu wollen.
"""

import argparse
import datetime
import json
import subprocess
import sys
import time

REPO_DEFAULT = "kia68/onShape"
SLEEP_BETWEEN_ISSUES = 3.0  # Sekunden. GitHubs sekundaeres Abuse-Rate-Limit fuer
# Issue-Erstellung wird bei kurzen Abstaenden (< ~1-2s) zuverlaessig ausgeloest;
# ist es einmal ausgeloest, retryt `gh` automatisch mit wachsendem Backoff, was
# einen ganzen Lauf ueber Stunden strecken kann. 3s Abstand bleibt im Test stabil.

# ---------------------------------------------------------------------------
# Labels
# ---------------------------------------------------------------------------

TYPE_LABELS = [
    ("type:epic", "6f42c1", "Epic — buendelt mehrere Tasks eines Bereichs"),
    ("type:task", "1d76db", "Einzelne umsetzbare Anforderung (FR/NFR/Arbeitspaket)"),
]

PHASE_LABELS = [
    ("phase:0", "5319e7", "Phase 0 — Fundament"),
    ("phase:1", "d73a4a", "Phase 1 — MVP"),
    ("phase:2", "fbca04", "Phase 2 — V1"),
    ("phase:3", "0e8a16", "Phase 3 — V2"),
    ("phase:4", "c5def5", "Phase 4 — Skalierung"),
]

AREA_LABELS = [
    ("area:infra", "c2e0c6", "Fundament, CI/CD, Datenmodell, Datenpipeline"),
    ("area:onboarding", "bfd4f2", "Onboarding & Profil"),
    ("area:nutrition", "bfe5bf", "Ernaehrungstracking"),
    ("area:barcode", "f9d0c4", "Barcode-Scanner & Kaufberatung"),
    ("area:photoai", "d4c5f9", "KI-Fotoerkennung"),
    ("area:trainplan", "c5f9d0", "Trainingsplan-Generierung"),
    ("area:trainlog", "f9f0c4", "Trainings-Logging"),
    ("area:movement", "f9c4e8", "Bewegungsvermittlung"),
    ("area:progress", "c4f9f0", "Fortschritt & Auswertung"),
    ("area:integrations", "e0c2e6", "Integrationen (Health, Wearables, Import)"),
    ("area:nfr", "cccccc", "Nicht-funktionale Anforderungen"),
    ("area:legal", "e99695", "Recht & Compliance"),
    ("area:business", "fef2c0", "Geschaeftsmodell & Billing"),
    ("area:scale", "b4d4f9", "Phase 4 — Skalierung"),
]

# ---------------------------------------------------------------------------
# Milestones (Titel exakt wie in KONZEPT.md §16 Roadmap)
# ---------------------------------------------------------------------------

TODAY = datetime.date(2026, 8, 21)

MILESTONES = [
    {
        "key": "phase0",
        "title": "Phase 0 — Fundament (Wochen 1–4)",
        "description": "Repo, CI/CD, Datenmodell, Datenpipeline (BLS/USDA/OFF), Auth-Grundgeruest, i18n, Design-System. Siehe docs/KONZEPT.md §16.",
        "due_on": TODAY + datetime.timedelta(weeks=4),
    },
    {
        "key": "phase1",
        "title": "Phase 1 — MVP (Wochen 5–16)",
        "description": "Ernaehrung + Training + Bewegungsschule MVP-Umfang, Offline+Sync, Billing, Rechtsgrundlagen. Meilenstein: geschlossene Beta, 100 Nutzer. Siehe docs/KONZEPT.md §16.",
        "due_on": TODAY + datetime.timedelta(weeks=4 + 12),
    },
    {
        "key": "phase2",
        "title": "Phase 2 — V1 (Monate 5–8)",
        "description": "Foto-KI, adaptives TDEE, Wearable-Sync, Programm-Vorlagen, Wochenberichte. Siehe docs/KONZEPT.md §16.",
        "due_on": TODAY + datetime.timedelta(weeks=4 + 12 + 17),
    },
    {
        "key": "phase3",
        "title": "Phase 3 — V2 (Monate 9–14)",
        "description": "Kamerabasierte Echtzeit-Formanalyse, KI-Coach-Chat, Trainer-/Studio-Portal, dritte Sprache. Siehe docs/KONZEPT.md §16.",
        "due_on": TODAY + datetime.timedelta(weeks=4 + 12 + 17 + 26),
    },
    {
        "key": "phase4",
        "title": "Phase 4 — Skalierung (ab Monat 15)",
        "description": "Formanalyse-Ausbau, regionale Datenbanken, oeffentliche API, native Apps. Siehe docs/KONZEPT.md §16.",
        "due_on": None,
    },
]

# ---------------------------------------------------------------------------
# Epics
# ---------------------------------------------------------------------------
# key -> (title, area label, milestone key, intro body)

EPICS = {
    "infra": (
        "Epic: Fundament & Infrastruktur",
        "area:infra",
        "phase0",
        "Technisches Fundament, auf dem alle anderen Epics aufbauen: Repo/CI/CD, "
        "vollstaendiges Datenmodell (§8), die Lebensmittel-Datenpipeline (§10.4) "
        "und das i18n-/Design-Grundgeruest (§13). Quelle: docs/KONZEPT.md §10, §16 Phase 0.",
    ),
    "onboarding": (
        "Epic: Onboarding & Profil",
        "area:onboarding",
        "phase1",
        "Registrierung, Profilerfassung, Zielauswahl mit medizinischen Grenzen, "
        "Equipment/Verfuegbarkeit, Gesundheits-Screening, Ergebnis-Screen mit "
        "erklaerbarem Tagesziel. Quelle: docs/KONZEPT.md §5.1.",
    ),
    "nutrition": (
        "Epic: Ernaehrungstracking",
        "area:nutrition",
        "phase1",
        "Tagebuch, Quick-Add, Meals/Rezepte, Mikronaehrstoffe, Wasser, Koerpermasse, "
        "Offline-Logging, natuerlichsprachige Eingabe. Quelle: docs/KONZEPT.md §5.2.",
    ),
    "barcode": (
        "Epic: Barcode-Scanner & Kaufberatung",
        "area:barcode",
        "phase1",
        "Das sichtbarste Differenzierungsmerkmal: kostenloser Barcode-Scan, "
        "personalisierter Fit-Score (§7.6), Alternativ-Empfehlung, Allergen-Warnung. "
        "Quelle: docs/KONZEPT.md §5.3.",
    ),
    "photoai": (
        "Epic: KI-Fotoerkennung",
        "area:photoai",
        "phase2",
        "Mahlzeitenfoto -> Zutaten mit Konfidenzintervall statt Scheinpraezision, "
        "immer editierbare Zutatenaufschluesselung. Quelle: docs/KONZEPT.md §5.4.",
    ),
    "trainplan": (
        "Epic: Trainingsplan-Generierung",
        "area:trainplan",
        "phase1",
        "Automatische Plangenerierung aus Profil (Volumen, Split, Uebungsauswahl, "
        "Progression) nach §7.4, inkl. Deload-Automatik. Quelle: docs/KONZEPT.md §5.5.",
    ),
    "trainlog": (
        "Epic: Trainings-Logging",
        "area:trainlog",
        "phase1",
        "Live-Workout-Modus, Pausentimer, RIR-Erfassung, 1RM-Schaetzung, "
        "PR-Erkennung, volle Offline-Faehigkeit. Quelle: docs/KONZEPT.md §5.6.",
    ),
    "movement": (
        "Epic: Bewegungsvermittlung",
        "area:movement",
        "phase1",
        "Kernfeature fuer Anfaenger: Video aus zwei Perspektiven, Cues, Fehlerbilder, "
        "Progressionsleitern, bis hin zur kamerabasierten Formanalyse (V2). "
        "Quelle: docs/KONZEPT.md §5.7 und §12.",
    ),
    "progress": (
        "Epic: Fortschritt & Auswertung",
        "area:progress",
        "phase1",
        "Gewichts-/Kraft-/Volumenverlauf, adaptives TDEE, Wochenberichte, "
        "DSGVO-Datenexport. Quelle: docs/KONZEPT.md §5.8.",
    ),
    "integrations": (
        "Epic: Integrationen",
        "area:integrations",
        "phase1",
        "Health-/Wearable-Sync und CSV-Import aus MyFitnessPal/Yazio/Hevy/Strong "
        "als Wechselhuerden-Senker. Quelle: docs/KONZEPT.md §5.9.",
    ),
    "nfr": (
        "Epic: Nicht-funktionale Anforderungen",
        "area:nfr",
        "phase1",
        "Performance-, Offline-, Barrierefreiheits-, Sicherheits- und "
        "Datenqualitaets-Grundanforderungen, die ab MVP gelten. "
        "Quelle: docs/KONZEPT.md §6.",
    ),
    "legal": (
        "Epic: Recht & Compliance",
        "area:legal",
        "phase0",
        "DSGVO (Gesundheitsdaten, Art. 9), Abgrenzung zur MDR, EU-AI-Act-Pflichten "
        "und Wellbeing-Guardrails. Kein Ersatz fuer Rechtsberatung — Arbeitsgrundlage "
        "fuer die Kanzlei. Quelle: docs/KONZEPT.md §14 und Anhang A.",
    ),
    "business": (
        "Epic: Geschaeftsmodell & Billing",
        "area:business",
        "phase1",
        "Free/Plus/Coach-Tiers mit Feature-Gating, Stripe-Abrechnung, "
        "Lifetime-Deal, Wechsler-Kampagne. Quelle: docs/KONZEPT.md §15.",
    ),
    "scale": (
        "Epic: Phase 4 — Skalierung",
        "area:scale",
        "phase4",
        "Wachstum ueber die MVP/V1/V2-Basis hinaus: Formanalyse-Ausbau, "
        "regionale Datenbanken, Partner-API, native Apps. "
        "Quelle: docs/KONZEPT.md §16 Phase 4.",
    ),
}

# ---------------------------------------------------------------------------
# Tasks
# ---------------------------------------------------------------------------
# Jede Zeile: (epic_key, id, title, body, milestone_key)

def T(epic, id_, title, body, phase):
    return {"epic": epic, "id": id_, "title": f"{id_} — {title}", "body": body, "phase": phase}


TASKS = [
    # ---- INFRA (Phase 0) --------------------------------------------------
    T("infra", "INFRA-01", "Repository, CI/CD, Umgebungen",
      "Repository-Setup, CI/CD-Pipeline, Umgebungen dev/staging/prod.", "phase0"),
    T("infra", "INFRA-02", "Datenbankschema + Migrationen",
      "Vollstaendiges PostgreSQL-Schema aus §8 als Flyway-Migrationen "
      "(Spring Data JPA oder Exposed als ORM): "
      "users, profiles, nutrition_targets, foods, food_servings, food_entries, "
      "recipes, recipe_items, exercises, exercise_muscles, exercise_mistakes, "
      "exercise_form_rules, programs, program_days, program_items, "
      "workout_sessions, workout_sets, weekly_muscle_volume (materialized view), "
      "body_measurements, tdee_estimates, barcode_scans. Row-Level Security auf "
      "allen Nutzertabellen. Quelle: §8.", "phase0"),
    T("infra", "INFRA-03", "Daten-Pipeline: BLS 4.0 + USDA + Open Food Facts",
      "Import, Deduplizierung (Barcode als starker Schluessel, Trigram-Fuzzy-Match "
      "auf Marke/Name), Plausibilitaetspruefung (Atwater-Check, Makro-Summen, "
      "Ausreisser), Uebersetzungslayer, Portionsgroessen, Vertrauensstufen "
      "(verified/community/estimated). ODbL-Partition fuer Open-Food-Facts-Daten "
      "strikt getrennt halten (kein DB-Merge, Share-Alike-Pflicht). "
      "Nightly Delta-Import. Quelle: §10.4, §11.1.", "phase0"),
    T("infra", "INFRA-04", "i18n-Grundgeruest DE/EN",
      "next-intl, Nachrichtenkataloge JSON, ICU MessageFormat, Routing /de/ /en/, "
      "hreflang, Accept-Language-Erkennung, Intl.NumberFormat/DateTimeFormat, "
      "Fachbegriff-Glossar. Quelle: §13.", "phase0"),
    T("infra", "INFRA-05", "Design-System, Komponentenbibliothek",
      "Tailwind CSS + shadcn/ui, barrierefreie Radix-Primitives als Basis fuer "
      "WCAG 2.2 AA (NFR-06).", "phase0"),

    # ---- ONBOARDING (FR-01..FR-11) ----------------------------------------
    T("onboarding", "FR-01", "Registrierung E-Mail/Passwort, Apple, Google, Passkeys",
      "Registrierung per E-Mail/Passwort, Apple, Google. Passkeys als Option.", "phase1"),
    T("onboarding", "FR-02", "Profil-Erfassung",
      "Geschlecht (m/w/divers/keine Angabe), Geburtsdatum, Groesse, Gewicht, "
      "Aktivitaetslevel (Beruf + Alltag), Trainingserfahrung (nie / <6 Mon. / "
      "6-24 Mon. / >2 Jahre).", "phase1"),
    T("onboarding", "FR-03", "Zielauswahl",
      "Abnehmen · Muskelaufbau · Zunehmen · Kraft · Gesundheit/Erhaltung · Recomp.", "phase1"),
    T("onboarding", "FR-04", "Zielrate mit medizinischen Grenzen",
      "Zielrate waehlbar mit harten Grenzen: Abnehmen 0,25-1,0 % KG/Woche, "
      "Zunehmen 0,125-0,5 % KG/Woche. Schnellere Raten sind blockiert, nicht nur "
      "gewarnt.", "phase1"),
    T("onboarding", "FR-05", "Equipment-Erfassung",
      "Fitnessstudio (voll) · Home-Gym (Auswahlliste: Kurzhanteln, Langhantel, "
      "Baender, Klimmzugstange, Kettlebell ...) · Nur Koerpergewicht.", "phase1"),
    T("onboarding", "FR-06", "Verfuegbarkeit",
      "Trainingstage/Woche (2-6), Minuten pro Einheit (20-120).", "phase1"),
    T("onboarding", "FR-07", "Gesundheits-Screening (PAR-Q+ Kurzform)",
      "Herzprobleme, Schwangerschaft, akute Verletzungen, Medikamente. Bei "
      "Treffern -> Hinweis auf aerztliche Ruecksprache, kein Ausschluss.", "phase1"),
    T("onboarding", "FR-08", "Einschraenkungen/Verletzungen -> Uebungsfilter",
      "Knie, Schulter, unterer Ruecken, Handgelenk, Huefte -> Uebungsfilter im "
      "Plangenerator.", "phase1"),
    T("onboarding", "FR-09", "Ernaehrungspraeferenzen und Allergene",
      "omnivor, vegetarisch, vegan, pescetarisch, halal, koscher; 14 EU-Allergene.", "phase1"),
    T("onboarding", "FR-10", "Onboarding <= 90 Sekunden",
      "Fortschrittsbalken. Jederzeit abbrechbar mit Defaults.", "phase1"),
    T("onboarding", "FR-11", "Ergebnis-Screen mit erklaerbarem Tagesziel",
      "„Dein Tagesziel: 2.140 kcal · 165 g Protein · 60 g Fett · 235 g "
      "Kohlenhydrate“ mit Erklaerung, wie es berechnet wurde (aufklappbar). "
      "Designprinzip: jede berechnete Zahl ist antippbar und erklaert sich selbst.", "phase1"),

    # ---- NUTRITION (FR-20..FR-34) ------------------------------------------
    T("nutrition", "FR-20", "Tagesansicht mit Kalorien/Makros/Restbudget",
      "Kalorien, Makros (P/F/K) und Rest-Budget oberhalb der Falz. Kalorien pro "
      "Mahlzeit direkt sichtbar.", "phase1"),
    T("nutrition", "FR-21", "Mahlzeiten-Slots",
      "Fruehstueck, Mittag, Abend, Snacks — vom Nutzer umbenennbar und "
      "erweiterbar.", "phase1"),
    T("nutrition", "FR-22", "Quick-Add <= 3 Taps",
      "Suche -> Ergebnis antippen -> Menge bestaetigen. Portionsgroesse vorbelegt "
      "mit der zuletzt genutzten Menge dieses Nutzers.", "phase1"),
    T("nutrition", "FR-23", "Multi-Select in der Suche",
      "Mehrere Lebensmittel gleichzeitig auswaehlen und in einem Zug loggen.", "phase1"),
    T("nutrition", "FR-24", "Tage/Mahlzeiten kopieren",
      "„Gestern kopieren“ / „Letzte Woche Montag kopieren“ / "
      "einzelne Mahlzeit kopieren.", "phase1"),
    T("nutrition", "FR-25", "Eigene Meals speichern",
      "Mehrere Lebensmittel als eine benannte, wiederverwendbare Einheit.", "phase1"),
    T("nutrition", "FR-26", "Eigene Rezepte",
      "Zutaten + Portionsanzahl -> Naehrwerte pro Portion. Skalierbar.", "phase1"),
    T("nutrition", "FR-27", "Rezept-Import per URL",
      "JSON-LD Recipe-Schema parsen, Fallback auf LLM-Extraktion.", "phase2"),
    T("nutrition", "FR-28", "Mikronaehrstoff-Tracking",
      "Mindestens Ballaststoffe, Zucker, gesaettigte Fette, Salz, Kalium, "
      "Kalzium, Eisen, Magnesium, Zink, Vitamin D, B12, C, Folat.", "phase1"),
    T("nutrition", "FR-29", "Wasser-Tracking",
      "Wasser-Tracking mit Tagesziel.", "phase1"),
    T("nutrition", "FR-30", "Gewicht, Koerpermasse, Koerperfett",
      "Gewicht, Koerpermasse (Taille, Huefte, Brust, Arm, Oberschenkel), "
      "Koerperfett (optional).", "phase1"),
    T("nutrition", "FR-31", "Offline-Logging",
      "Eintraege werden lokal gespeichert und synchronisieren bei Verbindung.", "phase1"),
    T("nutrition", "FR-32", "Unbegrenztes Logging im Free-Tier",
      "Kein Eintragslimit — direkter Gegensatz zu MyFitnessPals 5-Eintraege-Limit.", "phase1"),
    T("nutrition", "FR-33", "Natuerlichsprachige Eingabe",
      "„2 Eier, eine Scheibe Vollkornbrot und einen Kaffee mit Milch“ "
      "-> strukturierte Eintraege.", "phase2"),
    T("nutrition", "FR-34", "Naehrwertetikett fotografieren -> OCR",
      "Fuer Produkte, die nicht in der Datenbank sind.", "phase2"),

    # ---- BARCODE (FR-40..FR-51) --------------------------------------------
    T("barcode", "FR-40", "Barcode-Scan im Browser",
      "EAN-8/13, UPC-A/E ueber die Kamera direkt im Browser via BarcodeDetector "
      "API, Fallback zxing-wasm.", "phase1"),
    T("barcode", "FR-41", "Barcode-Scan kostenlos und unbegrenzt",
      "Kein Limit, keine Paywall, nie — im Free-Tier.", "phase1"),
    T("barcode", "FR-42", "Scan-Ergebnis < 1,5 Sekunden",
      "Lokaler Cache fuer bereits gescannte Produkte.", "phase1"),
    T("barcode", "FR-43", "Fit-Score (0-100)",
      "Wie gut passt dieses Produkt zu diesem Nutzer heute? Berechnung nach §7.6.", "phase1"),
    T("barcode", "FR-44", "Ampel-Darstellung mit Klartext-Begruendung",
      "Z. B. „Passt gut — 22 g Protein pro Portion, du liegst heute 60 g "
      "unter deinem Proteinziel.“", "phase1"),
    T("barcode", "FR-45", "Bessere Alternative",
      "Zeigt bis zu 3 Produkte derselben Kategorie mit besserem Fit-Score, "
      "bevorzugt in DE verfuegbare Produkte. Algorithmus §7.7.", "phase1"),
    T("barcode", "FR-46", "Preis-pro-Protein / Preis-pro-Kalorie",
      "Wo Preisdaten verfuegbar sind.", "phase3"),
    T("barcode", "FR-47", "Nutri-Score, NOVA-Klassifizierung, Zusatzstoffliste",
      "Aus Open Food Facts vorhanden, anzeigen.", "phase1"),
    T("barcode", "FR-48", "Allergen-Warnung basierend auf Nutzerprofil",
      "Prominent, rot, vor allem anderen.", "phase1"),
    T("barcode", "FR-49", "Produkt nicht gefunden -> Foto + OCR anlegen",
      "Beitrag geht (mit Einwilligung) an Open Food Facts zurueck.", "phase1"),
    T("barcode", "FR-50", "Einkaufslisten-Modus",
      "Mehrere Produkte hintereinander scannen, am Ende Gesamtuebersicht "
      "(„Dieser Einkauf deckt 3 Tage Protein“).", "phase2"),
    T("barcode", "FR-51", "Regal-Vergleichsmodus",
      "Zwei Produkte nacheinander scannen -> direkter Seite-an-Seite-Vergleich.", "phase2"),

    # ---- PHOTOAI (FR-60..FR-66) --------------------------------------------
    T("photoai", "FR-60", "Mahlzeitenfoto -> erkannte Zutaten",
      "Mahlzeitenfoto -> erkannte Zutaten mit geschaetzter Menge.", "phase2"),
    T("photoai", "FR-61", "Konfidenzintervall statt Scheinpraezision",
      "„540-680 kcal (mittlere Sicherheit)“. Kein einzelner Wert.", "phase2"),
    T("photoai", "FR-62", "Zutatenaufschluesselung editierbar",
      "Immer sichtbar und pro Zutat editierbar. Nie ausblenden.", "phase2"),
    T("photoai", "FR-63", "Rueckfrage bei niedriger Konfidenz",
      "Aktive Rueckfrage mit visuellen Portionsgroessen („Wie viel Reis?“ "
      "mit drei Fotos).", "phase2"),
    T("photoai", "FR-64", "Referenzobjekt-Erkennung zur Groessenkalibrierung",
      "Hand, Besteck, Standardteller.", "phase3"),
    T("photoai", "FR-65", "Zutaten-Matching gegen eigene Datenbank",
      "Erkannte Zutaten werden gegen die eigene Datenbank gematcht, nicht frei "
      "halluziniert.", "phase2"),
    T("photoai", "FR-66", "Bilder nach Verarbeitung loeschen",
      "Sofern der Nutzer sie nicht ausdruecklich speichert.", "phase2"),

    # ---- TRAINPLAN (FR-70..FR-79) ------------------------------------------
    T("trainplan", "FR-70", "Automatische Plangenerierung aus Profil",
      "Ziel, Erfahrung, Tage/Woche, Zeit/Einheit, Equipment, Verletzungen, Alter, "
      "Geschlecht. Algorithmus in §7.4.", "phase1"),
    T("trainplan", "FR-71", "Split-Auswahl automatisch, manuell ueberschreibbar",
      "Ganzkoerper (2-3 Tage), Oberkoerper/Unterkoerper (4 Tage), "
      "Push/Pull/Legs (5-6 Tage).", "phase1"),
    T("trainplan", "FR-72", "Mesozyklus-Struktur mit Deload",
      "4-6 Wochen Aufbau mit steigendem Volumen, dann 1 Woche Deload.", "phase1"),
    T("trainplan", "FR-73", "Einheit mit Uebungen, Saetzen, Wdh, RIR, Pausen",
      "Jede Einheit mit Uebungen, Saetzen, Wiederholungsbereich, Ziel-RIR und "
      "Pausenzeiten.", "phase1"),
    T("trainplan", "FR-74", "Uebungstausch mit Grundabfrage",
      "Jede Uebung gegen eine Alternative mit gleichem Zielmuskel und "
      "verfuegbarem Equipment tauschbar. Grund abfragen („zu schwer“ / "
      "„Geraet belegt“ / „Schmerzen“) -> fliesst ins "
      "Nutzermodell ein.", "phase1"),
    T("trainplan", "FR-75", "Manuelle Plan-Erstellung und -Bearbeitung",
      "Fuer Fortgeschrittene.", "phase1"),
    T("trainplan", "FR-76", "Import etablierter Programme als Vorlagen",
      "5/3/1, Starting Strength, PHUL, GZCLP, nSuns ... — Eigenimplementierungen "
      "der Struktur, keine Kopie geschuetzter Inhalte (Markennamen beachten).", "phase2"),
    T("trainplan", "FR-77", "Volumen-Dashboard",
      "Saetze pro Muskelgruppe pro Woche, mit Zielkorridor visualisiert.", "phase1"),
    T("trainplan", "FR-78", "Cardio im Belastungsmodell",
      "Cardio-/Konditionstraining wird mitgezaehlt (Fitbods grosser Fehler "
      "vermeiden).", "phase1"),
    T("trainplan", "FR-79", "Automatische Deload-Empfehlung",
      "Bei 3 Wochen stagnierender Leistung, wiederholt verfehltem RIR-Ziel, "
      "subjektiv hoher Erschoepfung, oder >8 Wochen im Kaloriendefizit.", "phase2"),

    # ---- TRAINLOG (FR-90..FR-98) -------------------------------------------
    T("trainlog", "FR-90", "Live-Workout-Modus",
      "Aktuelle Uebung gross, letztes Mal danebengeschrieben, Satz eintragen in "
      "2 Taps.", "phase1"),
    T("trainlog", "FR-91", "Vorbelegte Werte + Progressionsvorschlag",
      "Automatisch aus der letzten Einheit.", "phase1"),
    T("trainlog", "FR-92", "Pausentimer mit Ton/Vibration",
      "Automatisch startend nach Satzeintrag, Dauer aus dem Plan.", "phase1"),
    T("trainlog", "FR-93", "RIR-/RPE-Erfassung pro Satz",
      "Optional, fuer Fortgeschrittene, im Anfaengermodus ausgeblendet.", "phase1"),
    T("trainlog", "FR-94", "Warm-up-Satz-Rechner",
      "Aus dem Arbeitsgewicht automatisch 2-3 Aufwaermsaetze.", "phase2"),
    T("trainlog", "FR-95", "Supersaetze, Dropsaetze, Cluster-Saetze",
      "Erweiterte Satzformen.", "phase2"),
    T("trainlog", "FR-96", "Wake Lock + volle Offline-Faehigkeit",
      "Bildschirm bleibt an waehrend des Workouts (Wake Lock API).", "phase1"),
    T("trainlog", "FR-97", "1RM-Schaetzung",
      "Epley + Brzycki, gemittelt, und Verlaufskurve pro Uebung.", "phase1"),
    T("trainlog", "FR-98", "Persoenliche Rekorde automatisch erkennen",
      "Gewicht, Wiederholungen, geschaetztes 1RM, Volumen — automatisch feiern.", "phase1"),

    # ---- MOVEMENT (FR-110..FR-118) -----------------------------------------
    T("movement", "FR-110", "Uebungs-Detailseite: Video, Anleitung, Cues, Fehler",
      "Video/Animation aus zwei Perspektiven (Front + Seite), Textanleitung in "
      "Schritten, 3-5 Cues, Liste haeufiger Fehler mit Bild, Zielmuskulatur "
      "visualisiert.", "phase1"),
    T("movement", "FR-111", "Anfaengermodus: Anleitung automatisch einblenden",
      "Vor der ersten Ausfuehrung einer Uebung, nicht versteckt hinter einem "
      "Info-Icon.", "phase1"),
    T("movement", "FR-112", "Erste-Mal-Checkliste (Quiz)",
      "Kurzes Quiz nach der Anleitung („Wo sollten die Ellbogen "
      "zeigen?“) — sichert, dass die Kernpunkte angekommen sind.", "phase2"),
    T("movement", "FR-113", "Videos mit DE/EN-Untertiteln",
      "Ohne Ton verstaendlich (Studioumgebung ist laut).", "phase1"),
    T("movement", "FR-114", "Progressionsleiter pro Bewegungsmuster",
      "Z. B. Kniebeuge -> Box Squat -> Goblet Squat -> Front Squat -> Back "
      "Squat. Anfaenger startet auf der passenden Stufe.", "phase2"),
    T("movement", "FR-115", "Kamerabasierte Formanalyse im Browser",
      "MediaPipe Pose via WASM, rein clientseitig: Gelenkwinkel-Analyse, "
      "Wiederholungszaehlung, Live-Feedback bei definierten Fehlern "
      "(Knievalgus, Rundruecken, unvollstaendige Tiefe). Start mit 5 Uebungen "
      "(Kniebeuge, Kreuzheben, Bankdruecken, Schulterdruecken, Liegestuetz). "
      "Ablauf und Regelwerk in §12.2 Ebene 3.", "phase3"),
    T("movement", "FR-116", "Formanalyse ausschliesslich lokal",
      "Kein Videostream verlaesst das Geraet. Muss explizit gestartet werden.", "phase3"),
    T("movement", "FR-117", "Selbstaufnahme-Modus mit Bewegungs-Overlay",
      "Nutzer filmt sich, App legt Gelenkspur ueber das Video, Nutzer sieht "
      "seine Bahn vs. Referenzbahn. Risikoarme Vorstufe zur Echtzeit-Formanalyse.", "phase3"),
    T("movement", "FR-118", "Studio-Guide",
      "„Wie stelle ich die Bank ein“, „Wie lege ich die Scheiben "
      "auf“, „Wie frage ich nach einem Geraet“ — die "
      "unausgesprochenen Huerden fuer Anfaenger.", "phase2"),

    # ---- PROGRESS (FR-130..FR-137) -----------------------------------------
    T("progress", "FR-130", "Gewichtsverlauf mit 7-Tage-Mittel",
      "Nicht die Rohwerte prominent — Wassereinlagerung demotiviert.", "phase1"),
    T("progress", "FR-131", "Kalorien-/Makro-Verlauf, Adhaerenz-Quote",
      "Wochendurchschnitte.", "phase1"),
    T("progress", "FR-132", "Kraftverlauf pro Uebung",
      "Geschaetztes 1RM ueber Zeit.", "phase1"),
    T("progress", "FR-133", "Volumen pro Muskelgruppe pro Woche",
      "Mit Zielkorridor.", "phase1"),
    T("progress", "FR-134", "Adaptives TDEE",
      "Reale Kalorienverbrennung aus Gewichtsverlauf × Kalorienzufuhr "
      "rueckgerechnet, nach 14 Tagen Daten. Algorithmus §7.1 Schritt 4.", "phase2"),
    T("progress", "FR-135", "Woechentlicher Bericht",
      "Was lief gut, was nicht, eine konkrete Empfehlung.", "phase2"),
    T("progress", "FR-136", "Fortschrittsfotos, lokal verschluesselt",
      "Standardmaessig nicht in der Cloud.", "phase2"),
    T("progress", "FR-137", "Datenexport CSV/JSON",
      "Vollstaendig, kostenlos, im Free-Tier (DSGVO Art. 20).", "phase1"),

    # ---- INTEGRATIONS (FR-150..FR-153) -------------------------------------
    T("integrations", "FR-150", "Apple Health / Google Fit",
      "Schritte, aktive Kalorien, Gewicht, Herzfrequenz (bidirektional). "
      "Benoetigt vermutlich Capacitor-Wrapper, siehe LEGAL-10.", "phase2"),
    T("integrations", "FR-151", "Garmin, Fitbit, Withings, Polar",
      "Wearable-Sync ueber deren Web-APIs.", "phase2"),
    T("integrations", "FR-152", "Strava-Import",
      "Fuer Ausdaueraktivitaeten.", "phase3"),
    T("integrations", "FR-153", "Import aus MyFitnessPal, Yazio, Hevy, Strong",
      "CSV-Import — Wechselhuerde senken ist ein Akquisekanal.", "phase1"),

    # ---- NFR (NFR-01..NFR-14) ----------------------------------------------
    T("nfr", "NFR-01", "Performance: LCP < 1,8s, INP < 200ms",
      "Largest Contentful Paint < 1,8 s auf 4G-Mittelklassegeraet. Interaction "
      "to Next Paint < 200 ms.", "phase1"),
    T("nfr", "NFR-02", "Performance: Lebensmittelsuche < 150ms (p95)",
      "Erste Ergebnisse in < 150 ms (p95).", "phase1"),
    T("nfr", "NFR-03", "Performance: Barcode-Scan < 1,5s (p95)",
      "Scan bis Ergebnis.", "phase1"),
    T("nfr", "NFR-04", "Offline: vollstaendiges Logging + konfliktfreier Sync",
      "Essen + Training offline. Sync bei Verbindung, konfliktfrei. "
      "Konfliktstrategie: Feld-Level Last-Write-Wins, §9.6.", "phase1"),
    T("nfr", "NFR-05", "Verfuegbarkeit 99,9% monatlich",
      "SLA-Ziel fuer den Betrieb.", "phase1"),
    T("nfr", "NFR-06", "Barrierefreiheit WCAG 2.2 AA",
      "Tastaturbedienbar. Screenreader-getestet. Kontrast >= 4,5:1. Nicht "
      "farbcodierte Information allein. Rechtlich relevant seit BFSG "
      "(28.06.2025).", "phase1"),
    T("nfr", "NFR-07", "Mobile-first, PWA-installierbar",
      "Touch-Ziele >= 44 px, einhaendig bedienbar (wichtige Aktionen im "
      "unteren Drittel).", "phase1"),
    T("nfr", "NFR-08", "Sicherheit: TLS 1.3, Argon2id, Rate-Limiting, OWASP Top 10",
      "TLS 1.3, Verschluesselung ruhender Daten, Argon2id fuer Passwoerter, "
      "Rate-Limiting, OWASP Top 10 abgedeckt.", "phase1"),
    T("nfr", "NFR-09", "Datenschutz: EU-Hosting (Frankfurt)",
      "Kein Drittland-Transfer ohne Rechtsgrundlage.", "phase1"),
    T("nfr", "NFR-10", "Skalierung: 100.000 MAU ohne Architekturaenderung",
      "Kapazitaetsziel fuer die Architektur.", "phase1"),
    T("nfr", "NFR-11", "i18n: vollstaendige DE/EN-Paritaet ab Tag 1",
      "Alle Einheiten (kg/lb, cm/ft) und Datumsformate lokalisiert.", "phase1"),
    T("nfr", "NFR-12", "Beobachtbarkeit: selbst gehostet / EU-basiert",
      "Strukturierte Logs, Traces, Fehler-Tracking, Produkt-Analytics — kein "
      "Google Analytics.", "phase1"),
    T("nfr", "NFR-13", "Testabdeckung: 100% Unit-Tests fuer Algorithmus-Kernmodule",
      "Kalorien, Makros, Progression, Fit-Score — mit Referenzwerten.", "phase1"),
    T("nfr", "NFR-14", "Datenqualitaet: Quellenangabe + Vertrauenslevel",
      "Jeder Lebensmitteleintrag hat eine Quellenangabe und ein Vertrauenslevel, "
      "sichtbar fuer den Nutzer.", "phase1"),

    # ---- LEGAL (Anhang A + zentrale Compliance-Umsetzungen) ----------------
    T("legal", "LEGAL-01", "ODbL-Share-Alike-Auswirkung anwaltlich klaeren",
      "Wirkung auf die eigene Datenbank pruefen lassen, bevor OFF-Daten "
      "verarbeitet werden. Nicht verhandelbar vor Launch. Quelle: Anhang A #1, §10.4.", "phase0"),
    T("legal", "LEGAL-02", "BLS-4.0-Nutzungsbedingungen schriftlich bestaetigen lassen",
      "Bei blsdb.de / Max Rubner-Institut vor Produktivnutzung. Quelle: Anhang A #2.", "phase0"),
    T("legal", "LEGAL-03", "DSFA nach Art. 35 DSGVO durchfuehren und dokumentieren",
      "Erforderlich wegen umfangreicher Verarbeitung besonderer Kategorien "
      "(Gesundheitsdaten) plus automatisierter Empfehlungen. Quelle: Anhang A #3, §14.1.", "phase1"),
    T("legal", "LEGAL-04", "Bedarf eines Datenschutzbeauftragten pruefen",
      "Nach Art. 37 Abs. 1 lit. c — im Zweifel bestellen. Quelle: Anhang A #4.", "phase1"),
    T("legal", "LEGAL-05", "Fachliche Pruefung der Trainingsalgorithmen",
      "Durch Sportwissenschaftler:in. Quelle: Anhang A #5.", "phase1"),
    T("legal", "LEGAL-06", "Wellbeing-Guardrails mit Fachperson abstimmen",
      "Essstoerungs-Fachperson prueft die Guardrails aus §14.5 vor Launch. "
      "Quelle: Anhang A #6.", "phase1"),
    T("legal", "LEGAL-07", "Videoproduktion beauftragen",
      "Studio, Model, Schnitt fuer die Uebungsbibliothek (§11.2, §12.3). "
      "Quelle: Anhang A #7.", "phase1"),
    T("legal", "LEGAL-08", "Haftpflichtversicherung klaeren",
      "Fuer ein Fitness-Beratungsprodukt, insbesondere im Hinblick auf die "
      "Formanalyse. Quelle: Anhang A #8.", "phase1"),
    T("legal", "LEGAL-09", "LLM-Anbieter mit EU-Verarbeitung + AV-Vertrag auswaehlen",
      "Fuer Foto-Erkennung und Textgenerierung. Quelle: Anhang A #9, §14.1.", "phase1"),
    T("legal", "LEGAL-10", "Entscheidung PWA-only vs. Capacitor-Wrapper",
      "Fuer Health-Sync (Apple Health / Google Fit lassen sich aus dem Browser "
      "nicht direkt anbinden). Quelle: Anhang A #10, §10.2.", "phase2"),
    T("legal", "LEGAL-11", "Granulare Einwilligungs-Flows implementieren",
      "Getrennte Einwilligungen fuer Kernfunktion · Foto-KI-Verarbeitung · "
      "Wearable-Sync · anonymisierte Produktanalyse · Marketing. Ablehnung "
      "einzelner Zwecke darf die Kernfunktion nicht blockieren. Quelle: §14.1.", "phase1"),
    T("legal", "LEGAL-12", "Wellbeing-Guardrails im Produkt umsetzen",
      "Harte Kaloriengrenzen (nie < 1.200/1.500 kcal, nie < BMR × 1,1), "
      "BMI-Grenze (kein Ziel < 18,5), Ratengrenzen, Muster-Erkennung bei "
      "restriktivem Verhalten, keine wertende Sprache, kein Streak-Verlust, "
      "Wochen- statt Tagesfokus, Pausenmodus. Quelle: §14.5.", "phase1"),
    T("legal", "LEGAL-13", "EU-AI-Act Output-Filter fuer alle LLM-Texte",
      "Keine Diagnose-Formulierungen in generierten Texten, Kennzeichnung von "
      "KI-Interaktion, Erklaerbarkeit (/targets/explain), menschliche "
      "Ueberschreibbarkeit jeder Empfehlung. Quelle: §14.3.", "phase1"),

    # ---- BUSINESS -----------------------------------------------------------
    T("business", "BIZ-01", "Pricing-Tiers & Feature-Gating",
      "Free / Plus (3,99 €/Mon., 29,99 €/Jahr) / Coach (7,99 €/Mon., "
      "69,99 €/Jahr) mit Feature-Gates gemaess Tabelle in §15.1.", "phase1"),
    T("business", "BIZ-02", "Stripe-Abrechnung",
      "Subscriptions, SCA-Unterstuetzung, 1-Klick-Kuendigung (§ 312k BGB), "
      "transparente Rechnungshistorie, EU-Entitaet.", "phase1"),
    T("business", "BIZ-03", "Lifetime-Deal (129 €)",
      "Gedeckelt auf die ersten 5.000 Nutzer, siehe §15.1.", "phase1"),
    T("business", "BIZ-04", "Wechsler-Kampagne / Landingpages",
      "Ein-Klick-Import aus MFP/Yazio/Hevy/Strong bewerben, Landingpages auf "
      "konkrete Konkurrenz-Beschwerden zugeschnitten („Barcode wieder "
      "kostenlos“). Quelle: §15.4.", "phase1"),

    # ---- SCALE (Phase 4) -----------------------------------------------------
    T("scale", "SCALE-01", "Formanalyse auf 20 Uebungen erweitern",
      "Ausbau ueber die initialen 5 Uebungen hinaus.", "phase4"),
    T("scale", "SCALE-02", "Regionale Lebensmitteldatenbanken",
      "AT, CH, NL, ES.", "phase4"),
    T("scale", "SCALE-03", "Oeffentliche API fuer Partner",
      "Externe Integrationen ermoeglichen.", "phase4"),
    T("scale", "SCALE-04", "Native Apps",
      "Falls die Konversionsdaten es rechtfertigen (§10.2).", "phase4"),
]

PHASE_TITLES = {m["key"]: m["title"] for m in MILESTONES}


# ---------------------------------------------------------------------------
# gh-CLI Helpers
# ---------------------------------------------------------------------------

def run(args, input_text=None):
    result = subprocess.run(
        args, capture_output=True, text=True, encoding="utf-8", input=input_text
    )
    if result.returncode != 0:
        raise RuntimeError(
            f"Kommando fehlgeschlagen: {' '.join(args)}\n"
            f"stdout: {result.stdout}\nstderr: {result.stderr}"
        )
    return result.stdout.strip()


def ensure_labels(repo):
    print(f"Labels anlegen/aktualisieren in {repo} ...")
    for name, color, desc in TYPE_LABELS + PHASE_LABELS + AREA_LABELS:
        run([
            "gh", "label", "create", name,
            "--repo", repo,
            "--color", color,
            "--description", desc,
            "--force",
        ])
    print(f"  {len(TYPE_LABELS) + len(PHASE_LABELS) + len(AREA_LABELS)} Labels ok.")


def ensure_milestones(repo):
    print(f"Milestones anlegen in {repo} ...")
    existing_raw = run([
        "gh", "api", f"repos/{repo}/milestones",
        "--method", "GET", "-f", "state=all", "--paginate",
    ])
    existing = {m["title"]: m["number"] for m in json.loads(existing_raw)}

    key_to_number = {}
    for m in MILESTONES:
        if m["title"] in existing:
            key_to_number[m["key"]] = existing[m["title"]]
            print(f"  vorhanden: {m['title']} (#{existing[m['title']]})")
            continue
        args = [
            "gh", "api", f"repos/{repo}/milestones",
            "-f", f"title={m['title']}",
            "-f", f"description={m['description']}",
            "--jq", ".number",
        ]
        if m["due_on"]:
            args[3:3] = ["-f", f"due_on={m['due_on'].isoformat()}T00:00:00Z"]
        number = run(args)
        key_to_number[m["key"]] = int(number)
        print(f"  angelegt: {m['title']} (#{number})")
    return key_to_number


def fetch_existing_issues(repo):
    """Titel -> Nummer, fuer alle bereits vorhandenen Issues (offen+geschlossen).
    Macht das Skript sicher wiederholbar: ein abgebrochener/neu gestarteter Lauf
    legt keine Duplikate an, sondern erkennt und ueberspringt bereits angelegte
    Epics/Tasks anhand des (eindeutigen) Titels."""
    raw = run([
        "gh", "issue", "list", "--repo", repo, "--state", "all",
        "--limit", "500", "--json", "number,title",
    ])
    return {row["title"]: row["number"] for row in json.loads(raw)}


def create_issue(repo, title, body, labels, milestone_number, existing):
    if title in existing:
        number = existing[title]
        return number, f"https://github.com/{repo}/issues/{number}", True

    args = [
        "gh", "issue", "create",
        "--repo", repo,
        "--title", title,
        "--body", body,
    ]
    for label in labels:
        args += ["--label", label]
    if milestone_number:
        args += ["--milestone", str(milestone_number)]
    url = run(args)
    time.sleep(SLEEP_BETWEEN_ISSUES)
    number = int(url.rstrip("/").split("/")[-1])
    existing[title] = number
    return number, url, False


def update_issue_body(repo, number, body):
    run(["gh", "issue", "edit", str(number), "--repo", repo, "--body", body])


# ---------------------------------------------------------------------------
# Plan / Report
# ---------------------------------------------------------------------------

def print_plan():
    print("=" * 78)
    print("PLAN — OnShape GitHub-Backlog (Dry-Run, nichts wurde angelegt)")
    print("=" * 78)
    print(f"Milestones: {len(MILESTONES)}")
    for m in MILESTONES:
        due = m["due_on"].isoformat() if m["due_on"] else "-"
        print(f"  - {m['title']}  (faellig: {due})")
    print(f"\nLabels: {len(TYPE_LABELS) + len(PHASE_LABELS) + len(AREA_LABELS)}")
    print(f"\nEpics: {len(EPICS)}")
    for key, (title, area, phase, _) in EPICS.items():
        n_tasks = sum(1 for t in TASKS if t["epic"] == key)
        print(f"  - {title}  [{area}, {PHASE_TITLES[phase]}]  — {n_tasks} Tasks")
    print(f"\nTasks gesamt: {len(TASKS)}")
    print(f"Issues gesamt (Epics + Tasks): {len(EPICS) + len(TASKS)}")
    print("\nMit --execute ausfuehren, um Labels/Milestones/Issues in GitHub "
          "anzulegen.")
    print("Hinweis: 'gh project'-Befehle brauchen den OAuth-Scope 'project' "
          "(aktuell nicht vorhanden -> 'gh auth refresh -s project'), falls "
          "die Issues zusaetzlich in ein GitHub-Projects-Board sollen.")


def execute(repo):
    ensure_labels(repo)
    ensure_milestones(repo)

    print(f"\nBereits vorhandene Issues abfragen (fuer Resume-Sicherheit) ...")
    existing = fetch_existing_issues(repo)
    print(f"  {len(existing)} vorhandene Issues gefunden.")

    print(f"\nEpic-Issues anlegen in {repo} ...")
    epic_numbers = {}
    for key, (title, area, phase, intro) in EPICS.items():
        number, url, was_existing = create_issue(
            repo, title, intro,
            labels=["type:epic", area, f"phase:{phase[-1]}"],
            milestone_number=PHASE_TITLES[phase],
            existing=existing,
        )
        epic_numbers[key] = number
        tag = "vorhanden" if was_existing else "angelegt"
        print(f"  #{number}  {title}  [{tag}]  ({url})")

    print(f"\nTask-Issues anlegen in {repo} ...")
    epic_task_numbers = {key: [] for key in EPICS}
    for t in TASKS:
        epic_key = t["epic"]
        _, area, _, _ = EPICS[epic_key]
        body = (
            f"{t['body']}\n\n---\n"
            f"Epic: #{epic_numbers[epic_key]}\n"
            f"Quelle: docs/KONZEPT.md"
        )
        number, url, was_existing = create_issue(
            repo, t["title"], body,
            labels=["type:task", area, f"phase:{t['phase'][-1]}"],
            milestone_number=PHASE_TITLES[t["phase"]],
            existing=existing,
        )
        epic_task_numbers[epic_key].append((number, t["title"]))
        tag = "vorhanden" if was_existing else "angelegt"
        print(f"  #{number}  {t['title']}  [{tag}]")

    print(f"\nEpic-Checklisten aktualisieren in {repo} ...")
    for key, (title, area, phase, intro) in EPICS.items():
        checklist = "\n".join(
            f"- [ ] #{n} {ttl}" for n, ttl in epic_task_numbers[key]
        )
        final_body = f"{intro}\n\n## Tasks\n{checklist}"
        update_issue_body(repo, epic_numbers[key], final_body)
        time.sleep(SLEEP_BETWEEN_ISSUES)
        print(f"  #{epic_numbers[key]} aktualisiert "
              f"({len(epic_task_numbers[key])} Tasks)")

    print("\nFertig.")
    print(f"Epics: {len(epic_numbers)}, Tasks: {sum(len(v) for v in epic_task_numbers.values())}")
    print(f"Repo: https://github.com/{repo}/issues")
    print(f"Milestones: https://github.com/{repo}/milestones")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default=REPO_DEFAULT, help="owner/repo, Default: " + REPO_DEFAULT)
    parser.add_argument("--execute", action="store_true",
                         help="Tatsaechlich anlegen (ohne diesen Flag: nur Plan anzeigen)")
    args = parser.parse_args()

    if not args.execute:
        print_plan()
        return

    print(f"Fuehre AUS gegen {args.repo} — legt {len(EPICS) + len(TASKS)} Issues an.\n")
    execute(args.repo)


if __name__ == "__main__":
    sys.exit(main())
