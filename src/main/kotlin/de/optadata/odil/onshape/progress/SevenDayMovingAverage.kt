package de.optadata.odil.onshape.progress

import java.time.LocalDate

data class DatedValue(val date: LocalDate, val value: Double)

/**
 * FR-130: "gleitendes 7-Tage-Mittel (nicht die Rohwerte prominent -- Wassereinlagerung
 * demotiviert)". Klassisches TRAILING Mittel ueber die letzten (bis zu) 7 VORHANDENEN
 * Messpunkte -- nicht ueber Kalendertage: Gewicht wird typischerweise nicht taeglich gemessen,
 * ein kalendertage-basiertes Fenster wuerde bei Luecken den Durchschnitt eines einzelnen,
 * isolierten Messpunkts unveraendert durchreichen statt zu glaetten (genau das Gegenteil vom
 * Zweck der Funktion).
 */
object SevenDayMovingAverage {
    private const val WINDOW = 7

    fun compute(points: List<DatedValue>): List<DatedValue> {
        val sorted = points.sortedBy { it.date }
        return sorted.mapIndexed { index, point ->
            val windowStart = (index - WINDOW + 1).coerceAtLeast(0)
            val window = sorted.subList(windowStart, index + 1)
            DatedValue(point.date, window.sumOf { it.value } / window.size)
        }
    }
}
