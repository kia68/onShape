-- Epic Trainings-Logging: FR-96 "volle Offline-Faehigkeit" fuer den satzweisen Log innerhalb
-- einer Session -- gleiches Muster wie food_entries/water_entries (client-generierte clientId +
-- partieller Unique-Index + ON CONFLICT DO NOTHING fuer idempotente Retries). workout_sessions
-- hat clientId bereits seit V5 (Session-Ebene); workout_sets brauchte es noch nicht, weil es bis
-- jetzt keinen Schreibpfad dafuer gab.
ALTER TABLE workout_sets ADD COLUMN client_id text;
CREATE UNIQUE INDEX workout_sets_session_client_id_key ON workout_sets (session_id, client_id) WHERE client_id IS NOT NULL;
