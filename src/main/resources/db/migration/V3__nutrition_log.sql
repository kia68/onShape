-- §8.3 Ernaehrungsprotokoll (docs/KONZEPT.md)
-- Migrationsreihenfolge laut Konzept: recipes VOR food_entries (Fremdschluessel).

CREATE TYPE meal_slot_t AS ENUM ('breakfast','lunch','dinner','snack','pre_workout','post_workout');
CREATE TYPE entry_method_t AS ENUM ('search','barcode','photo','voice','recipe','quick_add','copy');

CREATE TABLE recipes (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       uuid REFERENCES users(id) ON DELETE CASCADE,  -- NULL = kuratiert
  name_de       text NOT NULL,
  name_en       text,
  servings      numeric(4,1) NOT NULL DEFAULT 1,
  instructions  text,
  source_url    text,
  is_public     boolean NOT NULL DEFAULT false,
  created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE recipe_items (
  recipe_id  uuid NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
  food_id    uuid NOT NULL REFERENCES foods(id),
  grams      numeric(8,2) NOT NULL,
  sort_order smallint NOT NULL DEFAULT 0,
  PRIMARY KEY (recipe_id, food_id, sort_order)
);

CREATE TABLE food_entries (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  food_id       uuid REFERENCES foods(id),
  recipe_id     uuid REFERENCES recipes(id),
  logged_date   date NOT NULL,                  -- lokales Datum des Nutzers
  slot          meal_slot_t NOT NULL,
  grams         numeric(8,2) NOT NULL,
  serving_id    uuid REFERENCES food_servings(id),
  method        entry_method_t NOT NULL,
  confidence    numeric(3,2),                   -- nur bei method='photo'
  -- Naehrwerte denormalisiert gespeichert: historische Eintraege duerfen sich
  -- nicht aendern, wenn die Quelldaten korrigiert werden.
  kcal          numeric(7,2) NOT NULL,
  protein_g     numeric(6,2) NOT NULL,
  fat_g         numeric(6,2) NOT NULL,
  carbs_g       numeric(6,2) NOT NULL,
  micros        jsonb NOT NULL DEFAULT '{}',
  client_id     text,                           -- Idempotenz fuer Offline-Sync
  created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON food_entries (user_id, logged_date);
CREATE UNIQUE INDEX ON food_entries (user_id, client_id) WHERE client_id IS NOT NULL;
