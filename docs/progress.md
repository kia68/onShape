# Fortschritt

Kurzprotokoll: welches Epic/Task erledigt wurde. Quelle der Epics/Tasks: `scripts/github_setup.py`, Backlog in [kia68/onShape](https://github.com/kia68/onShape/issues).

## 2026-08-21

- **Epic #1 — Fundament & Infrastruktur — alle 5 Sub-Tasks erledigt**
  - [INFRA-01 (#15) — Repository, CI/CD, Umgebungen](https://github.com/kia68/onShape/issues/15): `.github/workflows/backend-ci.yml` angelegt (Gradle-Build + Tests bei Push/PR auf `master`, JDK 17 Temurin, Testreport-Upload).
  - Stack-Entscheidung Kotlin/Spring Boot: `docs/KONZEPT.md` (§9.1, §10.1, §10.3, §16) und [INFRA-02 (#16)](https://github.com/kia68/onShape/issues/16) an Kotlin/Spring-Boot-Stack angepasst (Flyway/Spring Data JPA statt Drizzle).
  - [INFRA-02 (#16) — Datenbankschema + Migrationen](https://github.com/kia68/onShape/issues/16): vollstaendiges Schema aus §8 als Flyway-Migrationen V1-V7 + Row-Level Security (V8) ueber `app.current_user_id`. Dabei entdeckt und gefixt: `spring-boot-starter-flyway` fehlte (Boot 4.1 hat FlywayAutoConfiguration in einem eigenen Modul) — Migrationen liefen bis dahin nie, ohne dass es auffiel. Verifiziert gegen echten Postgres-16-Testcontainer.
  - [INFRA-03 (#17) — Daten-Pipeline: BLS 4.0 + USDA + Open Food Facts](https://github.com/kia68/onShape/issues/17): Dedup (Barcode stark, Trigram-Fuzzy, ODbL-konform nie quellen-uebergreifend gemerged), Plausibilitaetspruefung (Atwater/Makrosumme/Nullwerte/Ausreisser), Vertrauensstufen, echte REST-Clients fuer Open Food Facts + USDA FoodData Central, Datei-Importer fuer BLS 4.0. Kein Live-Netzwerkabruf in dieser Session, nur Unit-/Integrationstests gegen echtes Schema.
  - [INFRA-04 (#18) — i18n-Grundgeruest DE/EN](https://github.com/kia68/onShape/issues/18): Next.js-Frontend unter `frontend/` gescaffoldet, next-intl mit `/de/`/`/en/`-Routing, hreflang, ICU-Plural, Intl.NumberFormat/DateTimeFormat, Fachbegriff-Glossar.
  - [INFRA-05 (#19) — Design-System, Komponentenbibliothek](https://github.com/kia68/onShape/issues/19): Tailwind v4 Theme-Tokens + erste Radix/shadcn-Komponente (Button) mit sichtbarem Fokusring fuer WCAG 2.2 AA.

## 2026-08-20

- Backlog bootstrapped: 5 Milestones (Phasen 0–4), 14 Epics, 125 Tasks aus `docs/KONZEPT.md` per `scripts/github_setup.py` in `kia68/onShape` angelegt.
- Repo initialisiert, Remote `origin` gesetzt, initialer Commit gepusht.
