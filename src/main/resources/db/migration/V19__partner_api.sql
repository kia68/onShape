-- SCALE-03 (docs/KONZEPT.md §16 Phase 4): oeffentliche, self-service API fuer Partner.
-- Keine RLS: kein user_id-Bezug, gleiches Muster wie foods/exercises (oeffentliche
-- Referenzdaten, siehe V8__row_level_security.sql-Kommentar) -- Zugriff wird ausschliesslich
-- ueber PartnerApiKeyFilter/PartnerApiKeyService gesteuert, nicht ueber Postgres RLS.
CREATE TABLE partner_api_keys (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_name text NOT NULL,
  contact_email     text NOT NULL,
  key_prefix        text NOT NULL,        -- erste Zeichen des Klartext-Keys, fuer Support/Anzeige
  key_hash          text NOT NULL UNIQUE, -- SHA-256-Hex des vollen Klartext-Keys
  created_at        timestamptz NOT NULL DEFAULT now(),
  revoked_at        timestamptz,
  last_used_at      timestamptz
);

CREATE INDEX ON partner_api_keys (key_hash);
