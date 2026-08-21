-- Epic Ernaehrungstracking (FR-25 eigene Meals, FR-29 Wasser-Tracking).
-- food_entries/recipes/recipe_items existieren schon (V3), body_measurements fuer FR-30
-- ebenfalls (V6). client_id + Unique-Index nach demselben Idempotenz-Muster wie
-- food_entries (V3) fuer Offline-Sync (FR-31, NFR-04 "konfliktfrei").

CREATE TABLE saved_meals (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name        text NOT NULL,
  created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE saved_meal_items (
  saved_meal_id uuid NOT NULL REFERENCES saved_meals(id) ON DELETE CASCADE,
  food_id       uuid NOT NULL REFERENCES foods(id),
  grams         numeric(8,2) NOT NULL,
  sort_order    smallint NOT NULL DEFAULT 0,
  PRIMARY KEY (saved_meal_id, food_id, sort_order)
);

CREATE TABLE water_entries (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  logged_date   date NOT NULL,
  amount_ml     integer NOT NULL CHECK (amount_ml > 0),
  client_id     text,
  created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON water_entries (user_id, logged_date);
CREATE UNIQUE INDEX ON water_entries (user_id, client_id) WHERE client_id IS NOT NULL;

-- RLS, gleiches Muster wie V8__row_level_security.sql.
ALTER TABLE saved_meals ENABLE ROW LEVEL SECURITY;
ALTER TABLE saved_meals FORCE ROW LEVEL SECURITY;
CREATE POLICY owner_only ON saved_meals
  USING (user_id = app_current_user_id()) WITH CHECK (user_id = app_current_user_id());

ALTER TABLE water_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE water_entries FORCE ROW LEVEL SECURITY;
CREATE POLICY owner_only ON water_entries
  USING (user_id = app_current_user_id()) WITH CHECK (user_id = app_current_user_id());

ALTER TABLE saved_meal_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE saved_meal_items FORCE ROW LEVEL SECURITY;
CREATE POLICY via_saved_meal ON saved_meal_items
  USING (EXISTS (
    SELECT 1 FROM saved_meals m
    WHERE m.id = saved_meal_items.saved_meal_id AND m.user_id = app_current_user_id()
  ))
  WITH CHECK (EXISTS (
    SELECT 1 FROM saved_meals m
    WHERE m.id = saved_meal_items.saved_meal_id AND m.user_id = app_current_user_id()
  ));
