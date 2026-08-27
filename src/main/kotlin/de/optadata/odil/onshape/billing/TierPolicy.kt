package de.optadata.odil.onshape.billing

/**
 * BIZ-01: die Feature-Matrix aus KONZEPT.md §15.1, als reine, DB-freie Konstanten (NFR-13
 * testbar). Von den 17 Zeilen der Tabelle sind sechs gegen bereits bestehende Funktionen
 * durchsetzbar (die letzten beiden, Wochenbericht/FR-135 und Adaptives TDEE/FR-134, als
 * Nachtrag zu Epic Fortschritt & Auswertung, siehe [canShowWeeklyReport]/[canShowAdaptiveTdee])
 * -- der Rest ist entweder in jedem Tier unbegrenzt (Lebensmittel-Logging, Barcode-Scanner,
 * Makro-Tracking, Trainings-Logging, Datenexport -- Letzteres zusaetzlich aus DSGVO-Gruenden,
 * §14, nie einschraenkbar) oder betrifft Features, die in keinem bisherigen Epic gebaut wurden
 * (Foto-KI/Epic #5, Rezept-URL-Import/FR-27, Wearable-Sync/FR-150f, Formanalyse/FR-115ff,
 * KI-Coach-Chat, individuelle Periodisierung) -- fuer die gibt es folgerichtig noch keine
 * Gate-Logik, nur die Tier-Zuordnung selbst ist hier als Referenz fuer die jeweils spaetere
 * Umsetzung dokumentiert.
 */
object TierPolicy {

    /** "Fit-Score & Kaufberatung: 10 Scans/Monat" -- Kalendermonat (nicht gleitendes 30-Tage-
     * Fenster), einfachste, fuer Nutzer nachvollziehbare Interpretation ("reset am 1."). */
    const val FIT_SCORE_SCANS_PER_MONTH_FREE = 10

    fun fitScoreScanLimitPerMonth(tier: Tier): Int? = if (tier == Tier.FREE) FIT_SCORE_SCANS_PER_MONTH_FREE else null

    /** Ob die (bereits berechnete) Fit-Score-/Kaufberatung fuer den [alreadyScoredThisMonth].
     * Scan noch angezeigt werden darf. Betrifft NUR Score/Begruendung/Alternativen -- ein
     * erkannter Allergen-/Praeferenz-Konflikt bleibt in JEDEM Tier sichtbar (Sicherheits-
     * relevanz schlaegt Monetarisierung, siehe BarcodeScanService-KDoc). */
    fun canShowFitScore(tier: Tier, alreadyScoredThisMonth: Int): Boolean {
        val limit = fitScoreScanLimitPerMonth(tier) ?: return true
        return alreadyScoredThisMonth < limit
    }

    /** "Trainingsplan-Generator: 1 aktiver Plan" -- da das System technisch ohnehin immer nur
     * EIN Programm gleichzeitig aktiv fuehrt (`ProgramRepository.insert` deaktiviert die
     * vorherige Version), kann sich die Einschraenkung nur auf die Gesamtzahl je Account
     * beziehen: Free-Nutzer duerfen genau ein Programm ERSTELLEN (generiert oder manuell),
     * jeder weitere Generierungs-/Erstellungsversuch wird geblockt. Plus/Coach koennen beliebig
     * oft neu generieren (z. B. nach jedem Mesozyklus). */
    const val PROGRAM_CREATIONS_TOTAL_FREE = 1

    fun programCreationLimit(tier: Tier): Int? = if (tier == Tier.FREE) PROGRAM_CREATIONS_TOTAL_FREE else null

    fun canCreateProgram(tier: Tier, alreadyCreated: Int): Boolean {
        val limit = programCreationLimit(tier) ?: return true
        return alreadyCreated < limit
    }

    /** "Volumen-Analytics: Basis" -- KONZEPT nennt keinen Zahlenwert. Interpretation: Free
     * sieht die Volumen-Historie (FR-133) nur fuer die letzten 4 Wochen (ein typischer
     * Mesozyklus-Block, siehe MesocycleProgression), Plus/Coach die volle Historie. */
    const val VOLUME_HISTORY_WEEKS_FREE = 4

    fun volumeHistoryWindowWeeks(tier: Tier): Int? = if (tier == Tier.FREE) VOLUME_HISTORY_WEEKS_FREE else null

    /** "Mikronaehrstoffe: Basis (5)" -- KONZEPT nennt keine konkreten fuenf. Interpretation:
     * die fuer eine allgemeine, trainierende Zielgruppe relevantesten/haeufigsten
     * Mangel-Kandidaten (Eisen v. a. bei Frauen, Calcium & Vitamin D fuer Knochengesundheit,
     * B12 v. a. bei den im Profil abfragbaren vegetarischen/veganen Praeferenzen, Magnesium
     * fuer Muskelfunktion). Schluessel folgen der bestehenden `foods.micros`-Konvention
     * (siehe V2__foods.sql-Kommentar, `MicroNutrients`). */
    val MICRONUTRIENT_BASIS_KEYS = setOf("iron_mg", "calcium_mg", "vitamin_d_ug", "vitamin_b12_ug", "magnesium_mg")

    fun <T> filterMicros(tier: Tier, micros: Map<String, T>): Map<String, T> =
        if (tier == Tier.FREE) micros.filterKeys { it in MICRONUTRIENT_BASIS_KEYS } else micros

    /** "Wochenbericht: —/✓/✓" (§15.1) -- anders als die Gates oben ist das kein Kontingent,
     * sondern eine vollstaendige Sperre fuer FREE (FR-135, Epic Fortschritt & Auswertung). */
    fun canShowWeeklyReport(tier: Tier): Boolean = tier != Tier.FREE

    /** "Adaptives TDEE: —/✓/✓" (§15.1) -- gleiches Muster wie [canShowWeeklyReport] (FR-134). */
    fun canShowAdaptiveTdee(tier: Tier): Boolean = tier != Tier.FREE
}
