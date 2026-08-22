package de.optadata.odil.onshape.trainlog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersonalRecordDetectorTest {

    @Test
    fun `erster satz ueberhaupt ist in allen vier dimensionen ein rekord`() {
        val set = LoggedSet(weightKg = 100.0, reps = 5, isWarmup = false, completed = true)
        val results = PersonalRecordDetector.detect(set, PriorBests())
        val types = results.map { it.type }.toSet()
        assertEquals(setOf(
            PersonalRecordType.MAX_WEIGHT, PersonalRecordType.MAX_REPS_AT_WEIGHT,
            PersonalRecordType.EST_1RM, PersonalRecordType.VOLUME,
        ), types)
    }

    @Test
    fun `aufwaermsaetze zaehlen nie als rekord`() {
        val set = LoggedSet(weightKg = 200.0, reps = 5, isWarmup = true, completed = true)
        assertTrue(PersonalRecordDetector.detect(set, PriorBests()).isEmpty())
    }

    @Test
    fun `nicht abgeschlossene saetze zaehlen nie als rekord`() {
        val set = LoggedSet(weightKg = 200.0, reps = 5, isWarmup = false, completed = false)
        assertTrue(PersonalRecordDetector.detect(set, PriorBests()).isEmpty())
    }

    @Test
    fun `schwereres gewicht als bisher ist ein gewichts-rekord`() {
        val set = LoggedSet(weightKg = 105.0, reps = 3, isWarmup = false, completed = true)
        val prior = PriorBests(maxWeightKg = 100.0, maxRepsAtOrAboveWeight = 10, maxEstimated1Rm = 130.0, maxSetVolume = 500.0)
        val results = PersonalRecordDetector.detect(set, prior)
        val weightPr = results.first { it.type == PersonalRecordType.MAX_WEIGHT }
        assertEquals(100.0, weightPr.previousBest)
        assertEquals(105.0, weightPr.newValue)
    }

    @Test
    fun `leichteres gewicht ohne verbesserung ist kein rekord in irgendeiner dimension`() {
        val set = LoggedSet(weightKg = 80.0, reps = 5, isWarmup = false, completed = true)
        val prior = PriorBests(
            maxWeightKg = 100.0, maxRepsAtOrAboveWeight = 10,
            maxEstimated1Rm = OneRepMaxCalculator.estimate(100.0, 10), maxSetVolume = 1000.0,
        )
        assertTrue(PersonalRecordDetector.detect(set, prior).isEmpty())
    }

    @Test
    fun `priorBestsFrom aggregiert die historie korrekt`() {
        val samples = listOf(
            WorkingSetSample(80.0, 8), WorkingSetSample(100.0, 5), WorkingSetSample(90.0, 6),
        )
        val prior = PersonalRecordDetector.priorBestsFrom(samples, newWeightKg = 90.0)
        assertEquals(100.0, prior.maxWeightKg)
        // nur Saetze mit Gewicht >= 90.0 zaehlen fuer den Wiederholungsrekord: (100,5) und (90,6) -> max reps 6
        assertEquals(6, prior.maxRepsAtOrAboveWeight)
        assertEquals(OneRepMaxCalculator.estimate(100.0, 5), prior.maxEstimated1Rm)
        // hoechstes Einzelsatz-Volumen ist 80.0 x 8 = 640 (nicht an das Gewichtsfilter fuer den Wiederholungsrekord gebunden)
        assertEquals(640.0, prior.maxSetVolume)
    }

    @Test
    fun `priorBestsFrom ohne historie liefert leere bestwerte`() {
        val prior = PersonalRecordDetector.priorBestsFrom(emptyList(), newWeightKg = 50.0)
        assertEquals(PriorBests(), prior)
    }

    @Test
    fun `wiederholungsrekord ist gegen den bisherigen bestwert bei mindestens diesem gewicht`() {
        val set = LoggedSet(weightKg = 100.0, reps = 12, isWarmup = false, completed = true)
        val prior = PriorBests(maxWeightKg = 120.0, maxRepsAtOrAboveWeight = 8, maxEstimated1Rm = 999.0, maxSetVolume = 9999.0)
        val results = PersonalRecordDetector.detect(set, prior)
        val repsPr = results.single { it.type == PersonalRecordType.MAX_REPS_AT_WEIGHT }
        assertEquals(8.0, repsPr.previousBest)
        assertEquals(12.0, repsPr.newValue)
    }
}
