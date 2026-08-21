-- Fix fuer ein Henne-Ei-Problem in V8__row_level_security.sql: die `self_only`-Policy auf
-- `users` (USING/WITH CHECK: id = app_current_user_id()) gilt fuer ALLE Befehle, auch INSERT,
-- weil ohne FOR-Klausel automatisch FOR ALL gemeint ist. FORCE ROW LEVEL SECURITY macht das
-- zusaetzlich fuer den Tabelleneigentuemer verbindlich. Vor der Registrierung existiert aber
-- noch keine id, und vor dem Login-Lookup per E-Mail ist app.current_user_id() zwangslaeufig
-- noch NULL -- beide Faelle wuerden von self_only kategorisch verweigert.
--
-- Loesung: eine zweite, eng gefasste Session-Variable app.auth_lookup, die AUSSCHLIESSLICH
-- im Registrierungs-/Login-Codepfad gesetzt wird (siehe AuthService/RlsSession.asAuthLookup),
-- nie zusammen mit app.current_user_id in derselben Transaktion. Permissive Policies werden
-- pro Befehl mit OR verknuepft, self_only bleibt fuer alle authentifizierten Zugriffe wie
-- gehabt in Kraft.
CREATE POLICY auth_lookup ON users
  USING (current_setting('app.auth_lookup', true) = 'on')
  WITH CHECK (current_setting('app.auth_lookup', true) = 'on');
