package de.optadata.odil.onshape.progress

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * FR-131 "Adhaerenz-Quote". KONZEPT.md definiert den Begriff nicht praezise -- Interpretation:
 * Anteil der Tage im Zeitraum, an denen UEBERHAUPT etwas geloggt wurde (Tracking-Konsistenz),
 * nicht ob ein Kalorienziel getroffen wurde. Eine zielbasierte Definition wuerde zielkonformes,
 * aber leicht abweichendes Essen faelschlich als "nicht adhaerent" werten und braeuchte eine
 * willkuerliche Toleranzschwelle, die KONZEPT.md nirgends nennt -- Tracking-Konsistenz ist die
 * unzweideutigere, direkter messbare Groesse.
 */
object AdherenceCalculator {
    fun rate(loggedDates: Set<LocalDate>, from: LocalDate, to: LocalDate): Double {
        val totalDays = ChronoUnit.DAYS.between(from, to) + 1
        if (totalDays <= 0) return 0.0
        val loggedInRange = loggedDates.count { it >= from && it <= to }
        return loggedInRange.toDouble() / totalDays
    }
}
