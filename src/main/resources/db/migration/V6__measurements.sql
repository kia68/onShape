-- §8.6 Messwerte und adaptives Modell (docs/KONZEPT.md)

CREATE TABLE body_measurements (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id        uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  measured_on    date NOT NULL,
  weight_kg      numeric(5,2),
  body_fat_pct   numeric(4,1),
  waist_cm       numeric(5,1),
  hip_cm         numeric(5,1),
  chest_cm       numeric(5,1),
  arm_cm         numeric(5,1),
  thigh_cm       numeric(5,1),
  source         text NOT NULL DEFAULT 'manual',  -- 'manual','apple_health','garmin',...
  UNIQUE (user_id, measured_on, source)
);

CREATE TABLE tdee_estimates (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id        uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  computed_on    date NOT NULL,
  window_days    smallint NOT NULL,
  avg_intake     numeric(7,1) NOT NULL,
  weight_delta_kg numeric(5,3) NOT NULL,
  tdee_observed  numeric(7,1) NOT NULL,
  tdee_smoothed  numeric(7,1) NOT NULL,
  log_adherence  numeric(4,3) NOT NULL,
  applied        boolean NOT NULL DEFAULT false,
  UNIQUE (user_id, computed_on)
);
