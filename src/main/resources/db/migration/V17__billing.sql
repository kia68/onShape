-- Epic Geschaeftsmodell & Billing (#13): BIZ-01 (Tiers/Feature-Gating), BIZ-02 (Stripe),
-- BIZ-03 (Lifetime-Deal). KONZEPT.md §15.1.

CREATE TYPE tier_t AS ENUM ('free', 'plus', 'coach');
CREATE TYPE billing_period_t AS ENUM ('monthly', 'yearly', 'lifetime');
CREATE TYPE subscription_status_t AS ENUM ('active', 'canceled');

-- Kein Tier-Feld auf users -- keine Zeile = FREE, gleiches Muster wie user_consents (V16):
-- jede Abo-Aenderung ist ein expliziter, von Stripe-Webhooks getriebener Event, kein
-- stillschweigend gepflegtes Feld.
CREATE TABLE subscriptions (
  user_id                 uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  tier                    tier_t NOT NULL,
  billing_period          billing_period_t NOT NULL,
  status                  subscription_status_t NOT NULL DEFAULT 'active',
  -- Lifetime-Deal (BIZ-03, §15.1: "gedeckelt, z.B. erste 5.000 Nutzer") ist kein eigener Tier,
  -- sondern ein dauerhaft aktives Abo ohne current_period_end -- tier bleibt 'coach' (siehe
  -- SubscriptionService-KDoc fuer die Interpretationsentscheidung, welcher Tier gewaehrt wird).
  is_lifetime             boolean NOT NULL DEFAULT false,
  stripe_customer_id      text,
  stripe_subscription_id  text,
  current_period_end      timestamptz,
  created_at              timestamptz NOT NULL DEFAULT now(),
  updated_at              timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ON subscriptions (stripe_customer_id) WHERE stripe_customer_id IS NOT NULL;

ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions FORCE ROW LEVEL SECURITY;
CREATE POLICY owner_only ON subscriptions
  USING (user_id = app_current_user_id()) WITH CHECK (user_id = app_current_user_id());

-- Zwei Faelle brauchen Zugriff ohne (bzw. quer ueber) Nutzerkontext, analog zum
-- auth_lookup-Henne-Ei-Problem aus V9: (1) Stripe-Webhooks liefern nur eine Stripe-ID, keinen
-- eingeloggten Request; (2) der Lifetime-Deal-Deckel (BIZ-03, §15.1) muss ALLE Nutzer zaehlen,
-- nicht nur den aktuell eingeloggten. Eine gemeinsame, eng gefasste Policy fuer beide System-
-- Faelle, ausschliesslich in RlsSession.asSystemLookup gesetzt, nie zusammen mit
-- app.current_user_id in derselben Transaktion.
CREATE POLICY system_lookup ON subscriptions
  USING (current_setting('app.system_lookup', true) = 'on')
  WITH CHECK (current_setting('app.system_lookup', true) = 'on');
