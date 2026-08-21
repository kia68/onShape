-- §8.4 Uebungen und Bewegungsvermittlung (docs/KONZEPT.md)

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
  aliases            text[] NOT NULL DEFAULT '{}',   -- "Bankdruecken", "Bench Press", "Flachbank"
  pattern            movement_pattern_t NOT NULL,
  mechanic           mechanic_t NOT NULL,
  equipment          text[] NOT NULL,
  difficulty         difficulty_t NOT NULL,
  unilateral         boolean NOT NULL DEFAULT false,
  met_value          numeric(4,2),                   -- fuer Kalorienschaetzung
  contraindications  text[] NOT NULL DEFAULT '{}',   -- 'knee','shoulder','low_back'
  -- Bewegungsvermittlung
  video_front_url    text,
  video_side_url     text,
  thumbnail_url      text,
  setup_steps_de     text[] NOT NULL DEFAULT '{}',
  setup_steps_en     text[] NOT NULL DEFAULT '{}',
  execution_steps_de text[] NOT NULL DEFAULT '{}',
  execution_steps_en text[] NOT NULL DEFAULT '{}',
  cues_de            text[] NOT NULL DEFAULT '{}',   -- "Brust raus", "Knie nach aussen druecken"
  cues_en            text[] NOT NULL DEFAULT '{}',
  breathing_de       text,
  breathing_en       text,
  tempo              text,                            -- "2-0-1-0"
  -- Progressionsleiter
  regression_of      uuid REFERENCES exercises(id),
  progression_to     uuid REFERENCES exercises(id),
  created_at         timestamptz NOT NULL DEFAULT now()
);

-- Muskelbeteiligung mit Faktor: DER Schluessel fuer korrekte Volumenzaehlung (§7.4)
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

-- Regelwerk fuer die kamerabasierte Formanalyse (V2)
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
