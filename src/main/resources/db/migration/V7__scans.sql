-- §8.7 Scans und Empfehlungen (docs/KONZEPT.md)

CREATE TABLE barcode_scans (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  barcode       text NOT NULL,
  food_id       uuid REFERENCES foods(id),
  found         boolean NOT NULL,
  fit_score     smallint,
  score_breakdown jsonb,                -- Erklaerbarkeit: welche Komponente wie viel beitrug
  logged        boolean NOT NULL DEFAULT false,
  scanned_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON barcode_scans (barcode);   -- treibt "haeufig gescannt, aber nicht gefunden"
