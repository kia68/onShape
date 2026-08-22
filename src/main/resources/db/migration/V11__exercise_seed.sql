-- Epic Trainingsplan-Generierung: Startbestand an Uebungen, damit der Generator (FR-70) etwas
-- hat, aus dem er waehlen kann. KONZEPT.md nennt langfristig ~120 Uebungen (§16 Roadmap) --
-- dieser Satz deckt bewusst nur die noetige Breite ab (alle 10 Bewegungsmuster x alle
-- Equipment-Stufen x alle drei Erfahrungsstufen), keine Vollstaendigkeit. Video-/Bild-URLs und
-- Anleitungstexte (Bewegungsvermittlung, Epic #8) bleiben leer -- das ist ein eigenes Epic.

INSERT INTO exercises (slug, name_de, name_en, pattern, mechanic, equipment, difficulty, unilateral, met_value, contraindications) VALUES
-- Kniebeuge (squat)
('bodyweight-squat', 'Kniebeuge (Koerpergewicht)', 'Bodyweight Squat', 'squat', 'compound', '{bodyweight}', 'beginner', false, 5.0, '{knee}'),
('goblet-squat', 'Goblet Squat', 'Goblet Squat', 'squat', 'compound', '{dumbbells,kettlebell}', 'beginner', false, 5.5, '{knee}'),
('leg-press', 'Beinpresse', 'Leg Press', 'squat', 'compound', '{gym}', 'beginner', false, 5.0, '{knee}'),
('back-squat', 'Kniebeuge (Langhantel)', 'Back Squat', 'squat', 'compound', '{barbell}', 'intermediate', false, 6.0, '{knee,low_back}'),
('front-squat', 'Frontkniebeuge', 'Front Squat', 'squat', 'compound', '{barbell}', 'advanced', false, 6.5, '{knee,low_back,shoulder}'),
-- Hueftbeugen (hinge)
('glute-bridge', 'Gluecke Bridge', 'Glute Bridge', 'hinge', 'compound', '{bodyweight}', 'beginner', false, 3.5, '{}'),
('dumbbell-rdl', 'Rumaenisches Kreuzheben (Kurzhantel)', 'Dumbbell Romanian Deadlift', 'hinge', 'compound', '{dumbbells}', 'beginner', false, 5.0, '{low_back}'),
('hip-thrust', 'Hip Thrust', 'Hip Thrust', 'hinge', 'compound', '{barbell,gym}', 'intermediate', false, 5.5, '{low_back}'),
('kettlebell-swing', 'Kettlebell Swing', 'Kettlebell Swing', 'hinge', 'compound', '{kettlebell}', 'intermediate', false, 8.0, '{low_back}'),
('barbell-deadlift', 'Kreuzheben', 'Barbell Deadlift', 'hinge', 'compound', '{barbell}', 'advanced', false, 6.5, '{low_back}'),
-- Druecken horizontal (push_horizontal)
('pushup', 'Liegestuetz', 'Push-Up', 'push_horizontal', 'compound', '{bodyweight}', 'beginner', false, 4.0, '{shoulder,wrist}'),
('machine-chest-press', 'Brustpresse (Maschine)', 'Machine Chest Press', 'push_horizontal', 'compound', '{gym}', 'beginner', false, 4.5, '{shoulder}'),
('dumbbell-bench-press', 'Bankdruecken (Kurzhantel)', 'Dumbbell Bench Press', 'push_horizontal', 'compound', '{dumbbells}', 'intermediate', false, 5.0, '{shoulder}'),
('barbell-bench-press', 'Bankdruecken (Langhantel)', 'Barbell Bench Press', 'push_horizontal', 'compound', '{barbell}', 'intermediate', false, 5.5, '{shoulder}'),
('incline-dumbbell-press', 'Schraegbankdruecken (Kurzhantel)', 'Incline Dumbbell Press', 'push_horizontal', 'compound', '{dumbbells}', 'intermediate', false, 5.0, '{shoulder}'),
-- Druecken vertikal (push_vertical)
('pike-pushup', 'Pike Push-Up', 'Pike Push-Up', 'push_vertical', 'compound', '{bodyweight}', 'intermediate', false, 4.5, '{shoulder,wrist}'),
('machine-shoulder-press', 'Schulterpresse (Maschine)', 'Machine Shoulder Press', 'push_vertical', 'compound', '{gym}', 'beginner', false, 4.5, '{shoulder}'),
('dumbbell-shoulder-press', 'Schulterdruecken (Kurzhantel)', 'Dumbbell Shoulder Press', 'push_vertical', 'compound', '{dumbbells}', 'beginner', false, 5.0, '{shoulder}'),
('barbell-ohp', 'Schulterdruecken (Langhantel)', 'Barbell Overhead Press', 'push_vertical', 'compound', '{barbell}', 'intermediate', false, 5.5, '{shoulder,low_back}'),
-- Ziehen horizontal (pull_horizontal)
('band-row', 'Rudern (Band)', 'Band Row', 'pull_horizontal', 'compound', '{bands}', 'beginner', false, 3.5, '{}'),
('seated-cable-row', 'Rudern am Kabelzug', 'Seated Cable Row', 'pull_horizontal', 'compound', '{gym}', 'beginner', false, 4.5, '{low_back}'),
('dumbbell-row', 'Rudern (Kurzhantel)', 'Dumbbell Row', 'pull_horizontal', 'compound', '{dumbbells}', 'beginner', true, 4.5, '{low_back}'),
('barbell-row', 'Rudern (Langhantel)', 'Barbell Row', 'pull_horizontal', 'compound', '{barbell}', 'intermediate', false, 5.5, '{low_back}'),
-- Ziehen vertikal (pull_vertical)
('band-pulldown', 'Latzug (Band)', 'Band Pulldown', 'pull_vertical', 'compound', '{bands}', 'beginner', false, 3.5, '{shoulder}'),
('lat-pulldown', 'Latzug (Maschine)', 'Lat Pulldown', 'pull_vertical', 'compound', '{gym}', 'beginner', false, 4.5, '{shoulder}'),
('assisted-pullup', 'Klimmzug (unterstuetzt)', 'Assisted Pull-Up', 'pull_vertical', 'compound', '{gym}', 'intermediate', false, 5.0, '{shoulder}'),
('pullup', 'Klimmzug', 'Pull-Up', 'pull_vertical', 'compound', '{pullup_bar}', 'advanced', false, 6.0, '{shoulder}'),
-- Tragen (carry)
('farmers-carry', 'Farmer''s Carry', 'Farmer''s Carry', 'carry', 'compound', '{dumbbells,kettlebell}', 'beginner', false, 4.5, '{low_back}'),
-- Rumpf (core)
('plank', 'Unterarmstuetz', 'Plank', 'core', 'isolation', '{bodyweight}', 'beginner', false, 3.0, '{}'),
('dead-bug', 'Dead Bug', 'Dead Bug', 'core', 'isolation', '{bodyweight}', 'beginner', false, 3.0, '{low_back}'),
('cable-woodchop', 'Woodchop am Kabelzug', 'Cable Woodchop', 'core', 'isolation', '{gym}', 'intermediate', true, 4.0, '{low_back}'),
('hanging-leg-raise', 'Haengendes Beinheben', 'Hanging Leg Raise', 'core', 'isolation', '{pullup_bar}', 'advanced', false, 5.0, '{low_back}'),
-- Isolation
('dumbbell-curl', 'Bizepscurl (Kurzhantel)', 'Dumbbell Curl', 'isolation', 'isolation', '{dumbbells}', 'beginner', false, 3.0, '{}'),
('triceps-pushdown', 'Trizepsdruecken', 'Triceps Pushdown', 'isolation', 'isolation', '{gym,bands}', 'beginner', false, 3.0, '{}'),
('lateral-raise', 'Seitheben', 'Lateral Raise', 'isolation', 'isolation', '{dumbbells}', 'beginner', false, 3.0, '{shoulder}'),
('calf-raise', 'Wadenheben', 'Calf Raise', 'isolation', 'isolation', '{bodyweight,dumbbells}', 'beginner', false, 3.0, '{}'),
-- Cardio
('jumping-jacks', 'Hampelmann', 'Jumping Jacks', 'cardio', 'compound', '{bodyweight}', 'beginner', false, 8.0, '{}'),
('stationary-bike', 'Fahrradergometer', 'Stationary Bike', 'cardio', 'compound', '{gym}', 'beginner', false, 7.0, '{}'),
('rowing-machine', 'Rudergeraet', 'Rowing Machine', 'cardio', 'compound', '{gym}', 'intermediate', false, 7.5, '{low_back}');

-- Muskelbeteiligung (§7.4: 1.0 = direkter/primaerer Satz, 0.5 = fraktionaler/sekundaerer Satz).
INSERT INTO exercise_muscles (exercise_id, muscle, factor)
SELECT id, muscle, factor FROM exercises e, (VALUES
  ('bodyweight-squat', 'quads', 1.0), ('bodyweight-squat', 'glutes', 0.5),
  ('goblet-squat', 'quads', 1.0), ('goblet-squat', 'glutes', 0.5),
  ('leg-press', 'quads', 1.0), ('leg-press', 'glutes', 0.5),
  ('back-squat', 'quads', 1.0), ('back-squat', 'glutes', 0.5), ('back-squat', 'lower_back', 0.5),
  ('front-squat', 'quads', 1.0), ('front-squat', 'glutes', 0.5),
  ('glute-bridge', 'glutes', 1.0), ('glute-bridge', 'hamstrings', 0.5),
  ('dumbbell-rdl', 'hamstrings', 1.0), ('dumbbell-rdl', 'glutes', 1.0), ('dumbbell-rdl', 'lower_back', 0.5),
  ('hip-thrust', 'glutes', 1.0), ('hip-thrust', 'hamstrings', 0.5),
  ('kettlebell-swing', 'glutes', 1.0), ('kettlebell-swing', 'hamstrings', 1.0),
  ('barbell-deadlift', 'hamstrings', 1.0), ('barbell-deadlift', 'glutes', 1.0), ('barbell-deadlift', 'lower_back', 1.0), ('barbell-deadlift', 'traps', 0.5),
  ('pushup', 'chest', 1.0), ('pushup', 'front_delt', 0.5), ('pushup', 'triceps', 0.5),
  ('machine-chest-press', 'chest', 1.0), ('machine-chest-press', 'front_delt', 0.5), ('machine-chest-press', 'triceps', 0.5),
  ('dumbbell-bench-press', 'chest', 1.0), ('dumbbell-bench-press', 'front_delt', 0.5), ('dumbbell-bench-press', 'triceps', 0.5),
  ('barbell-bench-press', 'chest', 1.0), ('barbell-bench-press', 'front_delt', 0.5), ('barbell-bench-press', 'triceps', 0.5),
  ('incline-dumbbell-press', 'chest', 1.0), ('incline-dumbbell-press', 'front_delt', 1.0), ('incline-dumbbell-press', 'triceps', 0.5),
  ('pike-pushup', 'front_delt', 1.0), ('pike-pushup', 'triceps', 0.5),
  ('machine-shoulder-press', 'front_delt', 1.0), ('machine-shoulder-press', 'triceps', 0.5),
  ('dumbbell-shoulder-press', 'front_delt', 1.0), ('dumbbell-shoulder-press', 'triceps', 0.5),
  ('barbell-ohp', 'front_delt', 1.0), ('barbell-ohp', 'triceps', 0.5), ('barbell-ohp', 'traps', 0.5),
  ('band-row', 'upper_back', 1.0), ('band-row', 'biceps', 0.5), ('band-row', 'rear_delt', 0.5),
  ('seated-cable-row', 'upper_back', 1.0), ('seated-cable-row', 'biceps', 0.5), ('seated-cable-row', 'rear_delt', 0.5),
  ('dumbbell-row', 'lats', 1.0), ('dumbbell-row', 'biceps', 0.5), ('dumbbell-row', 'rear_delt', 0.5),
  ('barbell-row', 'lats', 1.0), ('barbell-row', 'upper_back', 1.0), ('barbell-row', 'biceps', 0.5),
  ('band-pulldown', 'lats', 1.0), ('band-pulldown', 'biceps', 0.5),
  ('lat-pulldown', 'lats', 1.0), ('lat-pulldown', 'biceps', 0.5),
  ('assisted-pullup', 'lats', 1.0), ('assisted-pullup', 'biceps', 0.5),
  ('pullup', 'lats', 1.0), ('pullup', 'biceps', 0.5), ('pullup', 'forearms', 0.5),
  ('farmers-carry', 'forearms', 1.0), ('farmers-carry', 'traps', 0.5), ('farmers-carry', 'abs', 0.5),
  ('plank', 'abs', 1.0), ('plank', 'obliques', 0.5),
  ('dead-bug', 'abs', 1.0),
  ('cable-woodchop', 'obliques', 1.0), ('cable-woodchop', 'abs', 0.5),
  ('hanging-leg-raise', 'abs', 1.0), ('hanging-leg-raise', 'forearms', 0.5),
  ('dumbbell-curl', 'biceps', 1.0),
  ('triceps-pushdown', 'triceps', 1.0),
  ('lateral-raise', 'side_delt', 1.0),
  ('calf-raise', 'calves', 1.0),
  ('jumping-jacks', 'quads', 0.5), ('jumping-jacks', 'calves', 0.5),
  ('stationary-bike', 'quads', 0.5),
  ('rowing-machine', 'lats', 0.5), ('rowing-machine', 'hamstrings', 0.5)
) AS m(slug, muscle, factor)
WHERE e.slug = m.slug;

-- Progressionsleiter je Bewegungsmuster (fuer FR-74 Uebungstausch "gleicher Zielmuskel").
UPDATE exercises SET progression_to = (SELECT id FROM exercises WHERE slug = 'goblet-squat') WHERE slug = 'bodyweight-squat';
UPDATE exercises SET regression_of = (SELECT id FROM exercises WHERE slug = 'bodyweight-squat'), progression_to = (SELECT id FROM exercises WHERE slug = 'back-squat') WHERE slug = 'goblet-squat';
UPDATE exercises SET regression_of = (SELECT id FROM exercises WHERE slug = 'goblet-squat'), progression_to = (SELECT id FROM exercises WHERE slug = 'front-squat') WHERE slug = 'back-squat';
UPDATE exercises SET regression_of = (SELECT id FROM exercises WHERE slug = 'back-squat') WHERE slug = 'front-squat';

UPDATE exercises SET progression_to = (SELECT id FROM exercises WHERE slug = 'dumbbell-rdl') WHERE slug = 'glute-bridge';
UPDATE exercises SET regression_of = (SELECT id FROM exercises WHERE slug = 'glute-bridge'), progression_to = (SELECT id FROM exercises WHERE slug = 'barbell-deadlift') WHERE slug = 'dumbbell-rdl';
UPDATE exercises SET regression_of = (SELECT id FROM exercises WHERE slug = 'dumbbell-rdl') WHERE slug = 'barbell-deadlift';

UPDATE exercises SET progression_to = (SELECT id FROM exercises WHERE slug = 'dumbbell-bench-press') WHERE slug = 'pushup';
UPDATE exercises SET regression_of = (SELECT id FROM exercises WHERE slug = 'pushup'), progression_to = (SELECT id FROM exercises WHERE slug = 'barbell-bench-press') WHERE slug = 'dumbbell-bench-press';
UPDATE exercises SET regression_of = (SELECT id FROM exercises WHERE slug = 'dumbbell-bench-press') WHERE slug = 'barbell-bench-press';

UPDATE exercises SET progression_to = (SELECT id FROM exercises WHERE slug = 'assisted-pullup') WHERE slug = 'lat-pulldown';
UPDATE exercises SET regression_of = (SELECT id FROM exercises WHERE slug = 'lat-pulldown'), progression_to = (SELECT id FROM exercises WHERE slug = 'pullup') WHERE slug = 'assisted-pullup';
UPDATE exercises SET regression_of = (SELECT id FROM exercises WHERE slug = 'assisted-pullup') WHERE slug = 'pullup';
