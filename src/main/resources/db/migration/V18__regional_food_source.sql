-- SCALE-02 (docs/KONZEPT.md §16 Phase 4): erste regionale Lebensmitteldatenbank.
-- Nur CH (naehrwertdaten.ch, BLV) angebunden -- AT/NL/ES bewusst vertagt, siehe
-- SwissFoodCompositionReader-Kommentar und docs/progress.md fuer die Begruendung je Land.
ALTER TYPE food_source_t ADD VALUE 'naehrwertdaten_ch';
