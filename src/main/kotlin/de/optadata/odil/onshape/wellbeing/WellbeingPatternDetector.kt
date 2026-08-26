package de.optadata.odil.onshape.wellbeing

/**
 * LEGAL-12 (KONZEPT.md §14.5 "Muster-Erkennung"): reine, DB-freie Kernlogik, damit die
 * Interpretationsentscheidungen unten mit Referenzwerten testbar sind (NFR-13). KONZEPT.md
 * beschreibt die drei Ausloeser nur in Prosa ohne Schwellenwerte -- jede Regel unten dokumentiert
 * ihre eigene Interpretationsentscheidung.
 */
data class WellbeingInput(
    /** Ein Wert pro Tag MIT mindestens einem Eintrag im 7-Tage-Betrachtungsfenster (fehlende
     * Tage = nicht geloggt, zaehlen nicht als "niedrige Zufuhr" -- konsistent mit
     * [de.optadata.odil.onshape.progress.AdherenceCalculator]). */
    val loggedDailyKcal: List<Double>,
    val targetKcal: Int?,
    /** Ziel-kcal je Neuberechnung der letzten 30 Tage, chronologisch AUFSTEIGEND sortiert. */
    val kcalHistoryChronological: List<Int>,
    val trainingSessionsLast7Days: Int,
)

enum class WellbeingFlag { EXTREME_LOW_INTAKE, FREQUENT_GOAL_LOWERING, EXCESSIVE_TRAINING }

object WellbeingPatternDetector {

    /** "Wiederholt extrem niedrige Zufuhr": Interpretation = unter 50% des aktuellen Kalorienziels
     * an mindestens 3 der geloggten Tage im 7-Tage-Fenster. 50% ist bewusst konservativ (nicht
     * z.B. 80%) -- normale Tagesschwankungen sollen keinen Fehlalarm ausloesen, ein wiederholt
     * halbiertes Ziel ist dagegen eindeutig. */
    const val EXTREME_LOW_INTAKE_RATIO = 0.5
    const val EXTREME_LOW_INTAKE_MIN_DAYS = 3

    /** "Haeufiges Ziel-Herunterschrauben": Interpretation = 3 oder mehr Absenkungen des kcal-Ziels
     * innerhalb der letzten 30 Tage. Nur Absenkungen zaehlen, eine Erhoehung (z.B. nach einem
     * Ziel-Wechsel von Abnehmen zu Erhaltung) ist unproblematisch. */
    const val FREQUENT_GOAL_LOWERING_MIN_COUNT = 3

    /** "Exzessives Training": Interpretation = Training an JEDEM der letzten 7 Tage, ganz ohne
     * Ruhetag. KONZEPT.md nennt keinen Zahlenwert und keinen Bezug zum individuellen Plan;
     * null Ruhetage in Folge ist branchenweit die unstrittigste, plan-unabhaengige Grenze. */
    const val EXCESSIVE_TRAINING_SESSIONS_PER_WEEK = 7

    fun evaluate(input: WellbeingInput): Set<WellbeingFlag> {
        val flags = mutableSetOf<WellbeingFlag>()

        val target = input.targetKcal
        if (target != null) {
            val extremeLowDays = input.loggedDailyKcal.count { it < target * EXTREME_LOW_INTAKE_RATIO }
            if (extremeLowDays >= EXTREME_LOW_INTAKE_MIN_DAYS) flags += WellbeingFlag.EXTREME_LOW_INTAKE
        }

        val lowerings = input.kcalHistoryChronological.zipWithNext().count { (prev, next) -> next < prev }
        if (lowerings >= FREQUENT_GOAL_LOWERING_MIN_COUNT) flags += WellbeingFlag.FREQUENT_GOAL_LOWERING

        if (input.trainingSessionsLast7Days >= EXCESSIVE_TRAINING_SESSIONS_PER_WEEK) flags += WellbeingFlag.EXCESSIVE_TRAINING

        return flags
    }
}
