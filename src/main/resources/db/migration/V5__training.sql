-- §8.5 Trainingsplanung und -protokoll (docs/KONZEPT.md)

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
  generation_ctx jsonb,                  -- Eingaben des Generators, fuer Nachvollziehbarkeit
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
  duration_sec  integer,          -- fuer Cardio/Isometrie
  distance_m    numeric(8,2),
  rir           smallint,
  is_warmup     boolean NOT NULL DEFAULT false,
  completed     boolean NOT NULL DEFAULT true,
  form_score    numeric(4,1),     -- aus der Kameraanalyse, wenn genutzt
  logged_at     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON workout_sets (session_id);
CREATE INDEX ON workout_sets (exercise_id, logged_at DESC);

-- Materialisierte Sicht fuer das Volumen-Dashboard.
-- Beachte die Gewichtung mit em.factor (direkte vs. fraktionale Saetze, §7.4)
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
