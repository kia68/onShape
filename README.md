# OnShape

Fitness- und Ernährungs-Web-App (PWA), zweisprachig DE/EN.

## Was ist OnShape?

Ernährungs-Apps (MyFitnessPal, Yazio) können Kalorien zählen, aber nichts mit Training anfangen. Trainings-Apps (Hevy, Strong) können Sätze protokollieren, aber nichts mit Ernährung. Wer beides will, jongliert zwei Apps, die nichts voneinander wissen — obwohl Ernährung und Training physiologisch ein einziges System sind: Das Kalorienziel hängt vom Trainingsvolumen ab, die Regeneration von der Proteinzufuhr.

OnShape verbindet vier Dinge in einem geschlossenen Regelkreis:

1. **Ernährungstracking** mit einer verifizierten, mehrsprachigen Lebensmitteldatenbank (BLS 4.0, USDA, Open Food Facts) — Barcode-Scanner kostenlos, nicht hinter einer Paywall.
2. **Kaufberatung am Regal** — der Scanner sagt nicht nur „350 kcal", sondern ob das Produkt zum Tagesziel passt und ob es eine bessere Alternative gibt.
3. **Individuelle Trainingsplan-Generierung** nach Geschlecht, Alter, Erfahrung, Equipment und Ziel, mit automatischer Progression.
4. **Bewegungsschule** — jede Übung mit Video, Textanleitung und Cue-Liste, damit Anfänger nicht ratlos vor dem Gerät stehen.

Das vollständige Konzept- und Anforderungsdokument steht in [`docs/KONZEPT.md`](docs/KONZEPT.md), der aktuelle Umsetzungsstand in [`docs/progress.md`](docs/progress.md).

## Tech-Stack

| Bereich | Technologie |
|---|---|
| Backend | Kotlin, Spring Boot 4.1, PostgreSQL 16, Flyway |
| Frontend | Next.js 16 (App Router), React, TypeScript, Tailwind CSS, next-intl |
| Auth | E-Mail/Passwort (Argon2id, JWT), Google/Apple-OAuth2 vorbereitet |
| Tests | JUnit 5, Testcontainers (echtes Postgres), MockMvc |

## Voraussetzungen

- [Docker](https://www.docker.com/) (für PostgreSQL und die Integrationstests)
- Java 17 ([Temurin](https://adoptium.net/) empfohlen) — wird sonst über den mitgelieferten `./gradlew`-Wrapper geholt
- Node.js 20.9 oder neuer

## Schnellstart

### 1. Datenbank starten

```bash
docker compose up -d
```

Startet Postgres 16 auf Port `5432` (Datenbank/User/Passwort: `onshape`).

### 2. Backend starten

```bash
./gradlew bootRun
```

Läuft auf `http://localhost:8080`. Beim ersten Start führt Flyway automatisch alle Datenbank-Migrationen aus (`src/main/resources/db/migration`). Die Standardwerte in `application.properties` reichen für die lokale Entwicklung aus — für einen Produktionsbetrieb müssen mindestens `JWT_SECRET` (Zufallswert, min. 32 Byte) sowie ggf. `CORS_ALLOWED_ORIGINS` gesetzt werden.

### 3. Frontend starten

```bash
cd frontend
npm install
npm run dev
```

Läuft auf `http://localhost:3000` (oder dem nächsten freien Port, falls belegt) und spricht das Backend unter `http://localhost:8080` an. Abweichende Backend-Adresse über `NEXT_PUBLIC_API_BASE_URL` konfigurierbar.

### 4. Loslegen

Im Browser `http://localhost:3000/de/onboarding` öffnen, Konto erstellen und den Onboarding-Flow durchlaufen. Danach ist die Ernährungs-Tagesansicht unter `http://localhost:3000/de/nutrition` erreichbar.

## Projektstruktur

```
src/main/kotlin/...    Backend (Kotlin/Spring Boot), nach Fachbereich paketiert
src/main/resources/
  db/migration/         Flyway-Migrationen (Datenbankschema, Reihenfolge = Versionsnummer)
src/test/kotlin/...     Backend-Tests (Unit + Integrationstests gegen echtes Postgres)
frontend/               Next.js-Frontend
docs/KONZEPT.md          Vollständiges Produkt- und Anforderungsdokument
docs/progress.md         Fortschrittsprotokoll je Epic
```

## Tests

```bash
./gradlew test          # Backend — braucht laufendes Docker (Testcontainers)
cd frontend && npm run build && npx eslint src   # Frontend — Typecheck + Lint
```
