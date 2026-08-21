-- Row-Level Security auf allen Nutzertabellen (docs/KONZEPT.md §8, Einleitung).
--
-- Die Anwendung laeuft mit einem einzigen DB-Rollen-Pool (kein Login pro Endnutzer).
-- Pro Request/Transaktion setzt die Anwendungsschicht daher die Session-Variable
-- app.current_user_id (z. B. via `SET LOCAL app.current_user_id = '<uuid>'` innerhalb
-- der Transaktion), bevor Nutzerdaten gelesen/geschrieben werden. Ohne gesetzten Wert
-- liefert app_current_user_id() NULL und die Policies verweigern jeden Zeilenzugriff
-- auf privaten Daten (Fail-Closed).

CREATE FUNCTION app_current_user_id() RETURNS uuid
LANGUAGE sql STABLE AS $$
  SELECT NULLIF(current_setting('app.current_user_id', true), '')::uuid
$$;

-- Tabellen mit direkter, verpflichtender user_id-Spalte: nur der Owner darf lesen/schreiben.
DO $$
DECLARE
  t text;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'profiles', 'nutrition_targets', 'food_entries',
    'workout_sessions', 'body_measurements', 'tdee_estimates', 'barcode_scans'
  ]
  LOOP
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
    EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
    EXECUTE format(
      'CREATE POLICY owner_only ON %I USING (user_id = app_current_user_id()) WITH CHECK (user_id = app_current_user_id())',
      t
    );
  END LOOP;
END $$;

-- users: Owner darf nur die eigene Zeile sehen/aendern (Vergleich ueber id statt user_id).
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;
CREATE POLICY self_only ON users USING (id = app_current_user_id()) WITH CHECK (id = app_current_user_id());

-- recipes, programs: user_id ist NULLable (NULL = kuratiert/Vorlage, oeffentlich lesbar).
-- Eigene Rezepte/Programme bleiben zusaetzlich sichtbar; is_public steuert Rezepte extra.
ALTER TABLE recipes ENABLE ROW LEVEL SECURITY;
ALTER TABLE recipes FORCE ROW LEVEL SECURITY;
CREATE POLICY owner_or_public ON recipes
  USING (user_id IS NULL OR user_id = app_current_user_id() OR is_public)
  WITH CHECK (user_id IS NULL OR user_id = app_current_user_id());

ALTER TABLE programs ENABLE ROW LEVEL SECURITY;
ALTER TABLE programs FORCE ROW LEVEL SECURITY;
CREATE POLICY owner_or_template ON programs
  USING (user_id IS NULL OR user_id = app_current_user_id())
  WITH CHECK (user_id IS NULL OR user_id = app_current_user_id());

-- Kindtabellen ohne eigene user_id-Spalte: Zugriff ueber den Eigentuemer der Elternzeile.
ALTER TABLE recipe_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE recipe_items FORCE ROW LEVEL SECURITY;
CREATE POLICY via_recipe ON recipe_items
  USING (EXISTS (
    SELECT 1 FROM recipes r
    WHERE r.id = recipe_items.recipe_id
      AND (r.user_id IS NULL OR r.user_id = app_current_user_id() OR r.is_public)
  ))
  WITH CHECK (EXISTS (
    SELECT 1 FROM recipes r
    WHERE r.id = recipe_items.recipe_id
      AND (r.user_id IS NULL OR r.user_id = app_current_user_id())
  ));

ALTER TABLE program_days ENABLE ROW LEVEL SECURITY;
ALTER TABLE program_days FORCE ROW LEVEL SECURITY;
CREATE POLICY via_program ON program_days
  USING (EXISTS (
    SELECT 1 FROM programs p
    WHERE p.id = program_days.program_id
      AND (p.user_id IS NULL OR p.user_id = app_current_user_id())
  ))
  WITH CHECK (EXISTS (
    SELECT 1 FROM programs p
    WHERE p.id = program_days.program_id
      AND (p.user_id IS NULL OR p.user_id = app_current_user_id())
  ));

ALTER TABLE program_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE program_items FORCE ROW LEVEL SECURITY;
CREATE POLICY via_program_day ON program_items
  USING (EXISTS (
    SELECT 1 FROM program_days pd
    JOIN programs p ON p.id = pd.program_id
    WHERE pd.id = program_items.program_day_id
      AND (p.user_id IS NULL OR p.user_id = app_current_user_id())
  ))
  WITH CHECK (EXISTS (
    SELECT 1 FROM program_days pd
    JOIN programs p ON p.id = pd.program_id
    WHERE pd.id = program_items.program_day_id
      AND (p.user_id IS NULL OR p.user_id = app_current_user_id())
  ));

ALTER TABLE workout_sets ENABLE ROW LEVEL SECURITY;
ALTER TABLE workout_sets FORCE ROW LEVEL SECURITY;
CREATE POLICY via_session ON workout_sets
  USING (EXISTS (
    SELECT 1 FROM workout_sessions ws
    WHERE ws.id = workout_sets.session_id AND ws.user_id = app_current_user_id()
  ))
  WITH CHECK (EXISTS (
    SELECT 1 FROM workout_sessions ws
    WHERE ws.id = workout_sets.session_id AND ws.user_id = app_current_user_id()
  ));
