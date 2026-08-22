-- Epic Bewegungsvermittlung (#8): KONZEPT.md §12.2 Ebene 1 nennt "Was ist normal?" explizit als
-- eigenes Content-Element der Uebungs-Detailseite ("Muskelkater 1-2 Tage danach ist normal.
-- Stechender Schmerz im Gelenk ist es nicht"), das im urspruenglichen Schema (V4) noch fehlte.
ALTER TABLE exercises ADD COLUMN what_is_normal_de text;
ALTER TABLE exercises ADD COLUMN what_is_normal_en text;
