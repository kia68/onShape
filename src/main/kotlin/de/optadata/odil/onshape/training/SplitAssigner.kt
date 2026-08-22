package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.onboarding.Experience

data class DayTemplate(val nameKey: String, val label: String, val patterns: List<MovementPattern>, val includeCore: Boolean)

data class SplitPlan(val splitType: String, val days: List<DayTemplate>)

private val FULL_BODY_PATTERNS = listOf(
    MovementPattern.SQUAT, MovementPattern.HINGE, MovementPattern.PUSH_HORIZONTAL,
    MovementPattern.PUSH_VERTICAL, MovementPattern.PULL_HORIZONTAL, MovementPattern.PULL_VERTICAL,
)
private val UPPER_PATTERNS = listOf(MovementPattern.PUSH_HORIZONTAL, MovementPattern.PUSH_VERTICAL, MovementPattern.PULL_HORIZONTAL, MovementPattern.PULL_VERTICAL)
private val LOWER_PATTERNS = listOf(MovementPattern.SQUAT, MovementPattern.HINGE)
private val PUSH_PATTERNS = listOf(MovementPattern.PUSH_HORIZONTAL, MovementPattern.PUSH_VERTICAL)
private val PULL_PATTERNS = listOf(MovementPattern.PULL_HORIZONTAL, MovementPattern.PULL_VERTICAL)
private val LEGS_PATTERNS = listOf(MovementPattern.SQUAT, MovementPattern.HINGE)

/**
 * KONZEPT.md §7.4 Schritt 2, Split-Zuordnungstabelle. Anfaenger bekommen IMMER Ganzkoerper,
 * unabhaengig von der Tageszahl ("mehr Uebungswiederholung bedeutet schnelleres motorisches
 * Lernen") -- das ist eine harte Regel, kein Vorschlag.
 *
 * "Jeder Plan muss die sechs Grundmuster enthalten" (Schritt 3) wird bei Splits ueber die
 * WOCHE erfuellt, nicht zwingend an jedem einzelnen Tag -- bei Ganzkoerper trivial pro Tag.
 */
object SplitAssigner {

    /** [forceFullBody] deckt sowohl die Anfaenger-Hartregel als auch [VolumeCorridor.preferHighFrequencySplit]
     * (>60 Jahre) ab -- beide fuehren zum selben Ganzkoerper-Ergebnis, siehe [ProgramGenerator]. */
    /** FR-71: "manuell ueberschreibbar" -- [splitTypeOverride] setzt sich ueber die
     * automatische Zuordnung hinweg (auch ueber die Anfaenger-Hartregel). Ein unbekannter Wert
     * faellt auf die automatische Zuordnung zurueck. */
    fun assign(daysPerWeek: Int, experience: Experience, forceFullBody: Boolean = false, splitTypeOverride: String? = null): SplitPlan {
        val days = daysPerWeek.coerceIn(1, 6)
        if (splitTypeOverride != null) {
            planFor(splitTypeOverride, days)?.let { return it }
        }
        if (forceFullBody || experience == Experience.NONE || experience == Experience.BEGINNER) {
            return SplitPlan("full_body", fullBodyDays(days))
        }
        return when (days) {
            1, 2 -> SplitPlan("full_body", fullBodyDays(days))
            3 -> SplitPlan("full_body", fullBodyDays(3))
            4 -> SplitPlan(
                "upper_lower",
                listOf(upper("A"), lower("A"), upper("B"), lower("B")),
            )
            5 -> SplitPlan(
                "ppl_upper_lower",
                listOf(push(), pull(), legs(), upper("A"), lower("A")),
            )
            else -> SplitPlan(
                "ppl",
                listOf(push("A"), pull("A"), legs("A"), push("B"), pull("B"), legs("B")),
            )
        }
    }

    private fun planFor(splitType: String, days: Int): SplitPlan? = when (splitType) {
        "full_body" -> SplitPlan("full_body", fullBodyDays(days))
        "upper_lower" -> SplitPlan("upper_lower", listOf(upper("A"), lower("A"), upper("B"), lower("B")))
        "ppl_upper_lower" -> SplitPlan("ppl_upper_lower", listOf(push(), pull(), legs(), upper("A"), lower("A")))
        "ppl" -> SplitPlan("ppl", listOf(push("A"), pull("A"), legs("A"), push("B"), pull("B"), legs("B")))
        else -> null
    }

    private fun fullBodyDays(count: Int): List<DayTemplate> =
        ('A'..'Z').take(count).map { letter ->
            DayTemplate("full_body", "Ganzkoerper $letter", FULL_BODY_PATTERNS, includeCore = true)
        }

    private fun upper(suffix: String) = DayTemplate("upper", "Oberkoerper $suffix", UPPER_PATTERNS, includeCore = false)
    private fun lower(suffix: String) = DayTemplate("lower", "Unterkoerper $suffix", LOWER_PATTERNS, includeCore = true)
    private fun push(suffix: String = "") = DayTemplate("push", "Push $suffix".trim(), PUSH_PATTERNS, includeCore = false)
    private fun pull(suffix: String = "") = DayTemplate("pull", "Pull $suffix".trim(), PULL_PATTERNS, includeCore = false)
    private fun legs(suffix: String = "") = DayTemplate("legs", "Legs $suffix".trim(), LEGS_PATTERNS, includeCore = true)
}
