package de.optadata.odil.onshape.trainlog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProgressionSuggesterTest {

    @Test
    fun `ohne vorherige daten gibt es keinen vorschlag`() {
        val result = ProgressionSuggester.suggest(null, repMax = 10, targetRir = 2)
        assertNull(result.suggestedWeightKg)
        assertNull(result.suggestedReps)
    }

    @Test
    fun `unter dem wiederholungsdach wird eine wiederholung mehr vorgeschlagen bei gleichem gewicht`() {
        val last = LastSetValues(weightKg = 60.0, reps = 7, rir = 3)
        val result = ProgressionSuggester.suggest(last, repMax = 10, targetRir = 2)
        assertEquals(60.0, result.suggestedWeightKg)
        assertEquals(8, result.suggestedReps)
    }

    @Test
    fun `wiederholungsdach bei ausreichend niedrigem rir erreicht erhoeht das gewicht`() {
        val last = LastSetValues(weightKg = 60.0, reps = 10, rir = 1)
        val result = ProgressionSuggester.suggest(last, repMax = 10, targetRir = 2)
        assertEquals(62.5, result.suggestedWeightKg)
        assertEquals(10, result.suggestedReps)
    }

    @Test
    fun `wiederholungsdach erreicht aber rir noch zu hoch bleibt beim gewicht`() {
        val last = LastSetValues(weightKg = 60.0, reps = 10, rir = 4)
        val result = ProgressionSuggester.suggest(last, repMax = 10, targetRir = 2)
        assertEquals(60.0, result.suggestedWeightKg)
        assertEquals(10, result.suggestedReps)
    }

    @Test
    fun `ohne rir-erfassung zaehlt allein das erreichen des wiederholungsdachs`() {
        val last = LastSetValues(weightKg = 60.0, reps = 10, rir = null)
        val result = ProgressionSuggester.suggest(last, repMax = 10, targetRir = 2)
        assertEquals(62.5, result.suggestedWeightKg)
    }

    @Test
    fun `ohne wiederholungsdach wird einfach wiederholt`() {
        val last = LastSetValues(weightKg = 60.0, reps = 10, rir = 2)
        val result = ProgressionSuggester.suggest(last, repMax = null, targetRir = null)
        assertEquals(60.0, result.suggestedWeightKg)
        assertEquals(10, result.suggestedReps)
    }
}
