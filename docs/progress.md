# Fortschritt

Kurzprotokoll: welches Epic/Task erledigt wurde. Quelle der Epics/Tasks: `scripts/github_setup.py`, Backlog in [kia68/onShape](https://github.com/kia68/onShape/issues).

## 2026-08-21

- **Epic #1 — Fundament & Infrastruktur**
  - [INFRA-01 (#15) — Repository, CI/CD, Umgebungen](https://github.com/kia68/onShape/issues/15): `.github/workflows/backend-ci.yml` angelegt (Gradle-Build + Tests bei Push/PR auf `master`, JDK 17 Temurin, Testreport-Upload).
  - Stack-Entscheidung Kotlin/Spring Boot: `docs/KONZEPT.md` (§9.1, §10.1, §10.3, §16) und [INFRA-02 (#16)](https://github.com/kia68/onShape/issues/16) an Kotlin/Spring-Boot-Stack angepasst (Flyway/Spring Data JPA statt Drizzle).

## 2026-08-20

- Backlog bootstrapped: 5 Milestones (Phasen 0–4), 14 Epics, 125 Tasks aus `docs/KONZEPT.md` per `scripts/github_setup.py` in `kia68/onShape` angelegt.
- Repo initialisiert, Remote `origin` gesetzt, initialer Commit gepusht.
