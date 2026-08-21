# Fortschritt

Kurzprotokoll: welches Epic/Task erledigt wurde. Quelle der Epics/Tasks: `scripts/github_setup.py`, Backlog in [kia68/onShape](https://github.com/kia68/onShape/issues).

## 2026-08-21

- **Epic #3 — Ernaehrungstracking — FR-20..FR-32 umgesetzt** (FR-27 Rezept-URL-Import, FR-33 Freitext-Eingabe, FR-34 Etiketten-OCR sind laut KONZEPT.md V1, nicht MVP -- bewusst vertagt, brauchen LLM/OCR-Anbindung analog zum OAuth-Vorgehen bei Epic 2)
  - Backend (Paket `nutrition`): Volltextsuche (FR-22, GIN-Indizes aus V2), Eintrag loggen/Multi-Select/loeschen/kopieren (FR-20/22/23/24), eigene Meals (FR-25), Rezepte mit Naehrwert-Skalierung pro Portion (FR-26), Mikronaehrstoff-Summierung (FR-28), Wasser-Tracking (FR-29), Koerpermasse (FR-30, erweitert `BodyMeasurementRepository` aus Epic 2), Offline-Sync-Idempotenz ueber `client_id` (FR-31, Unique-Index war in V3 schon vorbereitet), kein Eintragslimit (FR-32, schlicht nicht implementiert).
  - Neue Tabellen `saved_meals`/`saved_meal_items`/`water_entries` (V10) mit RLS nach demselben Muster wie V8.
  - Tagesansicht joint Lebensmittel-/Rezeptnamen dazu (`FoodEntryWithName`) statt nur IDs zurueckzugeben -- reine Anzeige-Denormalisierung, die eigentlichen Naehrwerte bleiben unveraendert historisch fixiert.
  - Frontend: Tagesansicht (`/nutrition`) mit 6 Mahlzeiten-Slots, Quick-Add-Suche mit Multi-Select, Wasser-Widget, Tageskopie. Echter Offline-Queue (`localStorage`, `clientId`-basiert) fuer FR-31 -- im Browser mit simuliertem Netzwerkausfall verifiziert: Eintrag erscheint sofort optimistisch, synct nach Reconnect, bleibt nach Reload persistent ohne Duplikat.
  - 88 automatisierte Tests (raufgesetzt von 64 aus Epic 2), inkl. RLS-Isolation und Idempotenz-Retry gegen echten Postgres-Testcontainer.
  - Kein UI fuer eigene Meals/Rezepte (Backend fertig inkl. Tests, aber ohne dedizierte Frontend-Seite) -- bewusste Scope-Entscheidung, um Tagesansicht + Offline-Sync (die spuerbarsten MVP-Features) sauber fertigzustellen statt alles halbfertig anzufassen.

- **Epic #2 — Onboarding & Profil — FR-01..FR-11 umgesetzt** (Backend + Frontend, live end-to-end gegen echten Postgres/Browser verifiziert)
  - Auth (FR-01): Registrierung/Login per E-Mail+Argon2id-Passwort, HS256-JWT (handgerollt statt jjwt, wegen Jackson-3/`tools.jackson`-Versionskonflikt). Google/Apple-OAuth2-Login strukturell vorbereitet (Spring Security `oauth2Login`, nur aktiv wenn `spring.security.oauth2.client.registration.*` gesetzt ist), Passkeys **nicht** umgesetzt (WebAuthn-Ceremony zu sicherheitskritisch fuer diese Session, offener Folgeschritt).
  - RLS-Luecke in V8 gefunden und gefixt: `self_only`-Policy auf `users` blockierte Registrierung und Login-Lookup (Henne-Ei-Problem, `app.current_user_id` ist vor der Authentifizierung zwangslaeufig NULL). Fix ueber neue, eng gefasste Session-Variable `app.auth_lookup` in `V9__users_auth_lookup_policy.sql`, ausschliesslich im Login-/Registrierungs-Codepfad gesetzt.
  - Profil/Ziel/Equipment/Verfuegbarkeit/Ernaehrungspraeferenzen (FR-02, FR-05, FR-06, FR-09) in einem kombinierten `PUT /api/onboarding/profile`-Aufruf (FR-10: <=90s-Flow).
  - FR-04 medizinische Ratengrenzen (0,25-1,0 %/Woche Abnehmen, 0,125-0,5 %/Woche Zu-/Aufbau) als harter Block, nicht nur Warnung. FR-07 PAR-Q+-Screening bewusst nicht persistiert (kein Ausschluss, nur Live-Hinweis; vermeidet Speicherung von Gesundheitsdaten ohne geklaerte Rechtsgrundlage, siehe Epic #12).
  - FR-11 Tagesziel-Berechnung nach KONZEPT.md §7.1/§7.2 (Mifflin-St-Jeor/Katch-McArdle, TDEE, zielabhaengige Kalorienanpassung, Protein/Fett/Kohlenhydrate/Ballaststoffe/Wasser) mit vollstaendiger, aufklappbarer Herleitung (`nutrition_targets.calculation` jsonb) -- Designprinzip "jede Zahl erklaert sich selbst". Harte Sicherheitsgrenzen (Mindestalter 16, Ziel-BMI >= 18,5, Kalorien-Untergrenze BMR×1,1/1200/1500 kcal) aus KONZEPT.md §7.1 umgesetzt.
  - Frontend: mehrstufiger Onboarding-Wizard unter `frontend/src/app/[locale]/onboarding/` (Konto/Basisdaten/Ziel/Setup), Fortschrittsbalken, "mit Standardwerten fertigstellen" (FR-10), Ergebnis-Screen mit generischer, rekursiver Renderer-Komponente fuer die Herleitung.
  - Testabdeckung (NFR-13): Unit-Tests fuer Rechenkern (Calculator/GoalRateValidator/SafetyLimits/HealthScreening) mit Referenzwerten, MockMvc-Integrationstests gegen echten Postgres-Testcontainer (Register/Login/Onboarding/RLS-Isolation zwischen zwei Nutzern). 64 Tests, alle gruen.
  - Manuell im Browser end-to-end verifiziert (Chrome via Claude-in-Chrome): Registrierung -> vierstufiger Wizard -> Tagesziel-Screen, Zahlen decken sich exakt mit der Handrechnung.

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
