-- §8.1 Nutzer und Profil (docs/KONZEPT.md)

CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

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
  goal_rate_pct_week  numeric(4,3) NOT NULL DEFAULT 0.5,   -- % Koerpergewicht/Woche
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

-- Zielhistorie: nie ueberschreiben, immer neue Version.
-- Wichtig fuer Nachvollziehbarkeit (AI Act) und fuer die Fortschrittsanzeige.
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
  calculation     jsonb NOT NULL,         -- vollstaendige Herleitung, fuer "Wie kam das zustande?"
  created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON nutrition_targets (user_id, valid_from DESC);
