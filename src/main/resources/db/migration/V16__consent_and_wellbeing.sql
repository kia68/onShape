-- Epic Recht & Compliance (#12): LEGAL-11 (granulare Einwilligungen, KONZEPT.md §14.1) und
-- LEGAL-12 (Wellbeing-Schutzmechanismen im Produkt, KONZEPT.md §14.5 "Pausenmodus").

CREATE TYPE consent_purpose_t AS ENUM ('core', 'photo_ai', 'wearable_sync', 'analytics', 'marketing');

-- Kein user_consents.granted-Default: jede Zeile ist eine explizite Entscheidung, die die
-- Anwendungsschicht geschrieben hat (§14.1 "nicht vorangekreuzt"). Fehlt eine Zeile fuer einen
-- Zweck, gilt er als (noch) nicht erteilt -- das entscheidet die Anwendungsschicht, nicht die DB.
CREATE TABLE user_consents (
  user_id     uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  purpose     consent_purpose_t NOT NULL,
  granted     boolean NOT NULL,
  granted_at  timestamptz,
  revoked_at  timestamptz,
  updated_at  timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, purpose)
);

ALTER TABLE user_consents ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_consents FORCE ROW LEVEL SECURITY;
CREATE POLICY owner_only ON user_consents
  USING (user_id = app_current_user_id()) WITH CHECK (user_id = app_current_user_id());

-- Pausenmodus (§14.5 "Ich moechte eine Weile nicht tracken"): bewusst zwei simple Spalten statt
-- einer eigenen Tabelle -- es gibt pro Nutzer genau einen aktuellen Zustand, keine Historie noetig.
-- Liegt auf `profiles`, nicht `users`: existiert erst nach dem Onboarding, was fuer eine
-- Trackingpause inhaltlich sowieso vorausgesetzt ist.
ALTER TABLE profiles ADD COLUMN tracking_paused boolean NOT NULL DEFAULT false;
ALTER TABLE profiles ADD COLUMN tracking_paused_at timestamptz;
