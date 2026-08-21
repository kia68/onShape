-- §8.2 Lebensmittel (docs/KONZEPT.md)

CREATE TYPE food_source_t     AS ENUM ('bls','usda','off','brand_verified','user','ai_estimate');
CREATE TYPE trust_t           AS ENUM ('verified','community','estimated');

CREATE TABLE foods (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  source            food_source_t NOT NULL,
  source_id         text,                      -- BLS-Schluessel, FDC-ID, OFF-Code
  trust             trust_t NOT NULL,
  barcode           text,                      -- EAN/UPC
  brand             text,
  name_de           text NOT NULL,
  name_en           text NOT NULL,
  category          text,                      -- OFF-Taxonomie
  nova_group        smallint CHECK (nova_group BETWEEN 1 AND 4),
  nutriscore        char(1),

  -- Alle Werte pro 100 g bzw. 100 ml
  kcal              numeric(7,2) NOT NULL,
  protein_g         numeric(6,2) NOT NULL,
  fat_g             numeric(6,2) NOT NULL,
  saturated_fat_g   numeric(6,2),
  trans_fat_g       numeric(6,2),
  carbs_g           numeric(6,2) NOT NULL,
  sugar_g           numeric(6,2),
  fiber_g           numeric(6,2),
  salt_g            numeric(6,3),
  micros            jsonb NOT NULL DEFAULT '{}',  -- {"vitamin_d_ug":1.2,"iron_mg":2.1,...}

  allergens         text[] NOT NULL DEFAULT '{}',
  additives         text[] NOT NULL DEFAULT '{}',
  is_liquid         boolean NOT NULL DEFAULT false,
  satiety_index     numeric(4,2),                 -- berechnet, siehe §7.6
  verified_at       timestamptz,
  verified_by       uuid REFERENCES users(id),
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ON foods (barcode) WHERE barcode IS NOT NULL AND trust = 'verified';
CREATE INDEX ON foods (source, source_id);
CREATE INDEX foods_search_de_idx ON foods
  USING GIN (to_tsvector('german', coalesce(brand,'') || ' ' || name_de));
CREATE INDEX foods_search_en_idx ON foods
  USING GIN (to_tsvector('english', coalesce(brand,'') || ' ' || name_en));
CREATE INDEX foods_trgm_idx ON foods USING GIN (name_de gin_trgm_ops);  -- Tippfehlertoleranz

-- Portionsgroessen: der eigentliche Genauigkeitshebel.
-- "1 Scheibe", "1 mittlere Banane", "1 Portion (laut Hersteller)"
CREATE TABLE food_servings (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  food_id      uuid NOT NULL REFERENCES foods(id) ON DELETE CASCADE,
  label_de     text NOT NULL,
  label_en     text NOT NULL,
  grams        numeric(7,2) NOT NULL,
  is_default   boolean NOT NULL DEFAULT false
);
