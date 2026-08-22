-- Epic Trainingsplan-Generierung: Erweiterungen an V5__training.sql.

-- FR-78: Cardio/Konditionstraining im Belastungsmodell. `program_items` war rein
-- wiederholungsbasiert (sets/rep_min/rep_max NOT NULL) -- ein Cardio-Block braucht stattdessen
-- eine Dauer. Statt reps auf 0 zu missbrauchen: reps nullable machen, duration_minutes dazu,
-- Constraint erzwingt "genau eines von beiden".
ALTER TABLE program_items ALTER COLUMN rep_min DROP NOT NULL;
ALTER TABLE program_items ALTER COLUMN rep_max DROP NOT NULL;
ALTER TABLE program_items ADD COLUMN duration_minutes smallint;
ALTER TABLE program_items ADD CONSTRAINT program_items_reps_xor_duration CHECK (
  (rep_min IS NOT NULL AND rep_max IS NOT NULL AND duration_minutes IS NULL) OR
  (rep_min IS NULL AND rep_max IS NULL AND duration_minutes IS NOT NULL)
);

-- FR-74: Uebungstausch mit Grundabfrage ("zu schwer" / "Geraet belegt" / "Schmerzen"), fliesst
-- als Ablehnungs-Historie in kuenftige Plangenerierungen ein (ExerciseScorer w8-Term).
CREATE TYPE exercise_swap_reason_t AS ENUM ('too_hard', 'equipment_occupied', 'pain', 'dislike', 'other');

CREATE TABLE exercise_feedback (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id           uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  exercise_id       uuid NOT NULL REFERENCES exercises(id),
  reason            exercise_swap_reason_t NOT NULL,
  replacement_id    uuid REFERENCES exercises(id),
  created_at        timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON exercise_feedback (user_id, exercise_id);

ALTER TABLE exercise_feedback ENABLE ROW LEVEL SECURITY;
ALTER TABLE exercise_feedback FORCE ROW LEVEL SECURITY;
CREATE POLICY owner_only ON exercise_feedback
  USING (user_id = app_current_user_id()) WITH CHECK (user_id = app_current_user_id());
