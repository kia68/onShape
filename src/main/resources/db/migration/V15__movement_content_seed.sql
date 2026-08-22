-- Epic Bewegungsvermittlung (#8), FR-110. KONZEPT.md §12.3 beziffert die volle Redaktion (120
-- Uebungen x 2 Sprachen x Video/Fotoproduktion) auf 8.000-15.000 EUR und Wochen Fachautoren-Zeit
-- -- das ist in dieser Session nicht leistbar (analog zu OCR/Vision-AI/OAuth-Credentials in
-- frueheren Epics: externe Ressourcen, die schlicht nicht vorhanden sind). Video-/Foto-URLs
-- bleiben deshalb NULL (das Frontend zeigt dafuer einen "folgt noch"-Platzhalter).
--
-- Stattdessen: vollstaendiger TEXT-Content (Aufbau, Ausfuehrung, Cues, Atmung, Tempo, haeufige
-- Fehler, "was ist normal") fuer die 5 Uebungen, die ein blutiger Anfaenger im generierten
-- Ganzkoerper-Plan (Epic #6) tatsaechlich zuerst sieht -- FR-111 (automatische Anfaenger-
-- Einblendung) laesst sich damit direkt am echten Anfaenger-Flow aus Epic #7 verifizieren, statt
-- an einer isolierten Detailseite, die niemand erreicht.

UPDATE exercises SET
  setup_steps_de = ARRAY['Fuesse schulterbreit aufstellen, Zehen leicht nach aussen.', 'Blick geradeaus, Brust aufrecht.'],
  setup_steps_en = ARRAY['Stand with feet shoulder-width apart, toes slightly turned out.', 'Look straight ahead, chest upright.'],
  execution_steps_de = ARRAY['Huefte nach hinten schieben, als wolltest du dich auf einen Stuhl setzen.', 'Knie in Zehenrichtung absenken, bis die Oberschenkel mindestens parallel zum Boden sind.', 'Ueber die Fersen zurueck nach oben druecken.'],
  execution_steps_en = ARRAY['Push your hips back as if sitting into a chair.', 'Lower your knees in the direction of your toes until your thighs are at least parallel to the floor.', 'Push back up through your heels.'],
  cues_de = ARRAY['Knie nach aussen druecken', 'Brust raus', 'Gewicht auf den Fersen'],
  cues_en = ARRAY['Push your knees out', 'Chest up', 'Weight on your heels'],
  breathing_de = 'Einatmen beim Absenken, ausatmen beim Hochdruecken.',
  breathing_en = 'Inhale on the way down, exhale on the way up.',
  tempo = '2-0-1-0',
  what_is_normal_de = 'Muskelkater im Oberschenkel/Gesaess 1-2 Tage danach ist normal. Stechender Schmerz im Knie ist es nicht -- dann abbrechen.',
  what_is_normal_en = 'Soreness in your thighs/glutes for 1-2 days afterward is normal. A sharp pain in the knee is not -- stop if that happens.'
WHERE slug = 'bodyweight-squat';

INSERT INTO exercise_mistakes (exercise_id, title_de, title_en, why_bad_de, why_bad_en, fix_de, fix_en, severity)
SELECT id, 'Knie fallen nach innen', 'Knees cave inward',
  'Erhoeht die Belastung auf die Innenbaender des Knies.', 'Increases stress on the knee''s inner ligaments.',
  'Aktiv "Knie nach aussen" denken, notfalls ein Miniband ueber die Knie zur Uebung nutzen.', 'Actively think "push knees out"; a mini-band above the knees can help as a cue.',
  3
FROM exercises WHERE slug = 'bodyweight-squat';

INSERT INTO exercise_mistakes (exercise_id, title_de, title_en, why_bad_de, why_bad_en, fix_de, fix_en, severity)
SELECT id, 'Fersen heben ab', 'Heels lift off the ground',
  'Verlagert die Last auf die Zehen und destabilisiert die Kniebeuge.', 'Shifts the load onto the toes and destabilizes the squat.',
  'Gewicht bewusst in den Fersen halten, Beweglichkeit im Sprunggelenk verbessern.', 'Consciously keep the weight in your heels; work on ankle mobility.',
  2
FROM exercises WHERE slug = 'bodyweight-squat';

INSERT INTO exercise_mistakes (exercise_id, title_de, title_en, why_bad_de, why_bad_en, fix_de, fix_en, severity)
SELECT id, 'Rundruecken im untersten Punkt', 'Rounded back at the bottom',
  'Erhoeht den Druck auf die Bandscheiben.', 'Increases pressure on the spinal discs.',
  'Nur so tief gehen, wie die Wirbelsaeule neutral bleibt.', 'Only go as deep as you can while keeping your spine neutral.',
  3
FROM exercises WHERE slug = 'bodyweight-squat';


UPDATE exercises SET
  setup_steps_de = ARRAY['Rueckenlage, Knie aufgestellt, Fuesse huefbreit nah am Gesaess.', 'Arme neben dem Koerper ablegen.'],
  setup_steps_en = ARRAY['Lie on your back, knees bent, feet hip-width apart close to your glutes.', 'Rest your arms beside your body.'],
  execution_steps_de = ARRAY['Gesaess anspannen und Huefte nach oben druecken, bis Schultern, Huefte und Knie eine Linie bilden.', 'Kurz oben halten.', 'Kontrolliert absenken.'],
  execution_steps_en = ARRAY['Squeeze your glutes and push your hips up until shoulders, hips, and knees form a straight line.', 'Hold briefly at the top.', 'Lower with control.'],
  cues_de = ARRAY['Gesaess fest anspannen', 'Nicht ins Hohlkreuz druecken', 'Durch die Fersen druecken'],
  cues_en = ARRAY['Squeeze your glutes hard', 'Do not overarch your lower back', 'Drive through your heels'],
  breathing_de = 'Ausatmen beim Hochdruecken, einatmen beim Absenken.',
  breathing_en = 'Exhale as you push up, inhale as you lower.',
  tempo = '1-1-2-0',
  what_is_normal_de = 'Ziehen im Gesaess am naechsten Tag ist normal. Schmerz im unteren Ruecken ist es nicht.',
  what_is_normal_en = 'A pulling sensation in the glutes the next day is normal. Lower back pain is not.'
WHERE slug = 'glute-bridge';

INSERT INTO exercise_mistakes (exercise_id, title_de, title_en, why_bad_de, why_bad_en, fix_de, fix_en, severity)
SELECT id, 'Ueberstreckung im unteren Ruecken', 'Overarching the lower back',
  'Verlagert die Last vom Gesaess auf die Lendenwirbelsaeule.', 'Shifts the load from the glutes onto the lumbar spine.',
  'Bauch leicht anspannen, Huefte nur so hoch heben wie ohne Hohlkreuz moeglich.', 'Brace your abs lightly, only lift the hips as high as possible without overarching.',
  2
FROM exercises WHERE slug = 'glute-bridge';

INSERT INTO exercise_mistakes (exercise_id, title_de, title_en, why_bad_de, why_bad_en, fix_de, fix_en, severity)
SELECT id, 'Fuesse zu weit vom Gesaess entfernt', 'Feet too far from the glutes',
  'Nimmt Spannung aus dem Gesaess und belastet die Oberschenkelrueckseite staerker.', 'Takes tension off the glutes and shifts load to the hamstrings.',
  'Fersen sollten mit ausgestrecktem Arm gerade so mit den Fingerspitzen erreichbar sein.', 'Your heels should be just reachable with your fingertips when your arm is fully extended.',
  1
FROM exercises WHERE slug = 'glute-bridge';


UPDATE exercises SET
  setup_steps_de = ARRAY['Haende schulterbreit, leicht ausserhalb der Schultern auf dem Boden.', 'Koerper von Kopf bis Ferse gerade, Bauch angespannt.'],
  setup_steps_en = ARRAY['Hands shoulder-width apart, slightly outside your shoulders, on the floor.', 'Body in a straight line from head to heels, core braced.'],
  execution_steps_de = ARRAY['Ellbogen beugen und Brust kontrolliert Richtung Boden senken.', 'Kurz vor dem Boden stoppen.', 'Zurueck nach oben druecken, bis die Arme fast gestreckt sind.'],
  execution_steps_en = ARRAY['Bend your elbows and lower your chest toward the floor with control.', 'Stop just short of the floor.', 'Push back up until your arms are nearly straight.'],
  cues_de = ARRAY['Koerper wie ein Brett', 'Ellbogen ca. 45 Grad zum Koerper', 'Bauchnabel einziehen'],
  cues_en = ARRAY['Body like a plank', 'Elbows at about 45 degrees to your body', 'Draw your belly button in'],
  breathing_de = 'Einatmen beim Absenken, ausatmen beim Hochdruecken.',
  breathing_en = 'Inhale on the way down, exhale on the way up.',
  tempo = '2-0-1-0',
  what_is_normal_de = 'Zittern in den Armen bei den letzten Wiederholungen ist normal. Schmerz im Handgelenk ist es nicht -- auf Fauststuetz oder Griffe ausweichen.',
  what_is_normal_en = 'Shaking arms on the last reps is normal. Wrist pain is not -- switch to fist support or push-up handles.'
WHERE slug = 'pushup';

INSERT INTO exercise_mistakes (exercise_id, title_de, title_en, why_bad_de, why_bad_en, fix_de, fix_en, severity)
SELECT id, 'Huefte saggt durch', 'Hips sag',
  'Fehlende Rumpfspannung belastet den unteren Ruecken.', 'Lack of core tension loads the lower back.',
  'Gesaess und Bauch aktiv anspannen, ggf. auf den Knien starten.', 'Actively brace glutes and abs; start on your knees if needed.',
  2
FROM exercises WHERE slug = 'pushup';

INSERT INTO exercise_mistakes (exercise_id, title_de, title_en, why_bad_de, why_bad_en, fix_de, fix_en, severity)
SELECT id, 'Ellbogen zeigen komplett zur Seite', 'Elbows flare straight out to the sides',
  'Erhoeht die Belastung auf die Schulter.', 'Increases stress on the shoulder joint.',
  'Ellbogen naeher am Koerper fuehren, ca. 45 Grad.', 'Keep elbows closer to your body, about 45 degrees.',
  2
FROM exercises WHERE slug = 'pushup';


UPDATE exercises SET
  setup_steps_de = ARRAY['Haende schulterbreit auf dem Boden, Huefte weit nach oben strecken (umgekehrtes V).', 'Beine so weit wie moeglich durchstrecken.'],
  setup_steps_en = ARRAY['Hands shoulder-width apart on the floor, hips pushed high into an inverted V.', 'Straighten your legs as much as possible.'],
  execution_steps_de = ARRAY['Kopf kontrolliert Richtung Boden zwischen den Haenden absenken.', 'Kurz vor dem Boden stoppen.', 'Zurueck nach oben druecken.'],
  execution_steps_en = ARRAY['Lower your head toward the floor between your hands with control.', 'Stop just short of the floor.', 'Push back up.'],
  cues_de = ARRAY['Huefte hoch halten', 'Kopf zwischen den Haenden', 'Schultern von den Ohren wegdruecken'],
  cues_en = ARRAY['Keep hips high', 'Head between your hands', 'Push your shoulders away from your ears'],
  breathing_de = 'Einatmen beim Absenken, ausatmen beim Hochdruecken.',
  breathing_en = 'Inhale on the way down, exhale on the way up.',
  tempo = '2-0-1-0',
  what_is_normal_de = 'Belastungsgefuehl in der Schulter (nicht im Gelenk) ist normal. Einschiessender Schmerz beim Absenken ist es nicht.',
  what_is_normal_en = 'A working sensation in the shoulder muscle (not the joint) is normal. A sudden sharp pain when lowering is not.'
WHERE slug = 'pike-pushup';

INSERT INTO exercise_mistakes (exercise_id, title_de, title_en, why_bad_de, why_bad_en, fix_de, fix_en, severity)
SELECT id, 'Huefte sinkt waehrend der Bewegung', 'Hips drop during the movement',
  'Verwandelt die Uebung faktisch in einen normalen Liegestuetz und nimmt die Schulterbelastung raus.', 'Effectively turns the exercise into a regular push-up and removes the shoulder loading.',
  'Huefte bewusst oben halten, Fuesse naeher an die Haende stellen.', 'Consciously keep your hips high; place your feet closer to your hands.',
  2
FROM exercises WHERE slug = 'pike-pushup';

INSERT INTO exercise_mistakes (exercise_id, title_de, title_en, why_bad_de, why_bad_en, fix_de, fix_en, severity)
SELECT id, 'Kopf stoesst gegen den Boden', 'Head bumps into the floor',
  'Fehlende Kontrolle am Ende der Bewegung.', 'Lack of control at the end of the range of motion.',
  'Bewegung verkuerzen, bis genug Kraft fuer die volle Range da ist.', 'Shorten the range of motion until you have enough strength for the full range.',
  2
FROM exercises WHERE slug = 'pike-pushup';


UPDATE exercises SET
  setup_steps_de = ARRAY['Unterarme auf dem Boden, Ellbogen unter den Schultern.', 'Fuesse huefbreit, auf die Zehen stuetzen.'],
  setup_steps_en = ARRAY['Forearms on the floor, elbows under your shoulders.', 'Feet hip-width apart, up on your toes.'],
  execution_steps_de = ARRAY['Koerper von Kopf bis Ferse zu einer geraden Linie anspannen.', 'Position halten.', 'Ruhig weiteratmen.'],
  execution_steps_en = ARRAY['Brace your body into a straight line from head to heels.', 'Hold the position.', 'Keep breathing calmly.'],
  cues_de = ARRAY['Bauchnabel zur Wirbelsaeule ziehen', 'Gesaess leicht anspannen', 'Nicht die Luft anhalten'],
  cues_en = ARRAY['Draw your belly button toward your spine', 'Lightly squeeze your glutes', 'Do not hold your breath'],
  breathing_de = 'Ruhig und gleichmaessig weiteratmen, nicht die Luft anhalten.',
  breathing_en = 'Breathe calmly and steadily, do not hold your breath.',
  tempo = 'statisch',
  what_is_normal_de = 'Zittern im Rumpf gegen Ende ist normal. Schmerz im unteren Ruecken ist es nicht -- Haltedauer verkuerzen.',
  what_is_normal_en = 'Shaking in your core toward the end is normal. Lower back pain is not -- shorten the hold.'
WHERE slug = 'plank';

INSERT INTO exercise_mistakes (exercise_id, title_de, title_en, why_bad_de, why_bad_en, fix_de, fix_en, severity)
SELECT id, 'Huefte haengt durch', 'Hips sag down',
  'Entlastet die Bauchmuskulatur und belastet den unteren Ruecken.', 'Takes load off the abs and stresses the lower back.',
  'Gesaess anspannen, Becken leicht einrollen.', 'Squeeze your glutes, tuck your pelvis slightly.',
  2
FROM exercises WHERE slug = 'plank';

INSERT INTO exercise_mistakes (exercise_id, title_de, title_en, why_bad_de, why_bad_en, fix_de, fix_en, severity)
SELECT id, 'Huefte zu hoch (Dach-Position)', 'Hips too high (pike position)',
  'Nimmt die Spannung aus dem Rumpf.', 'Takes the tension out of the core.',
  'Gerade Linie von Kopf bis Ferse anstreben, ggf. im Spiegel kontrollieren.', 'Aim for a straight line from head to heels; check in a mirror if possible.',
  1
FROM exercises WHERE slug = 'plank';
