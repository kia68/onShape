-- Epic Trainings-Logging: FR-95 Supersaetze, Dropsaetze, Cluster-Saetze. Supersaetze nutzen die
-- bereits seit V5 vorhandene, bisher ungenutzte Spalte program_items.superset_group -- hier nur
-- der set_technique_t-Enum + die zwei neuen Spalten fuer Drop-/Cluster-Saetze auf workout_sets.
-- ALLE Zeilen einer zusammengesetzten Satzgruppe (auch der erste/Haupt-Satz) tragen set_technique
-- + sub_set_index (ab 0 hochzaehlend); ein einfacher Satz hat beide Felder NULL. So muss nie ein
-- bereits geloggter Satz nachtraeglich per UPDATE veraendert werden (append-only, offline-sync-
-- freundlich, gleiches Prinzip wie der Rest des Logging-Schreibpfads seit V13).
CREATE TYPE set_technique_t AS ENUM ('dropset', 'cluster');
ALTER TABLE workout_sets ADD COLUMN set_technique set_technique_t;
ALTER TABLE workout_sets ADD COLUMN sub_set_index smallint;
ALTER TABLE workout_sets ADD CONSTRAINT workout_sets_technique_subindex_xor
    CHECK ((set_technique IS NULL) = (sub_set_index IS NULL));
