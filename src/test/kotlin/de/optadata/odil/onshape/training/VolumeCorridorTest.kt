package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.onboarding.Experience
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VolumeCorridorTest {

    @Test
    fun `anfaenger korridor entspricht der konzept-tabelle`() {
        val result = VolumeCorridor.forProfile(Experience.BEGINNER, age = 30)
        assertEquals(8, result.startSetsPerMuscle)
        assertEquals(10, result.maxSetsPerMuscle)
    }

    @Test
    fun `fortgeschrittene korridor entspricht der konzept-tabelle`() {
        val result = VolumeCorridor.forProfile(Experience.INTERMEDIATE, age = 30)
        assertEquals(12, result.startSetsPerMuscle)
        assertEquals(16, result.maxSetsPerMuscle)
    }

    @Test
    fun `erfahrene korridor entspricht der konzept-tabelle`() {
        val result = VolumeCorridor.forProfile(Experience.ADVANCED, age = 30)
        assertEquals(14, result.startSetsPerMuscle)
        assertEquals(20, result.maxSetsPerMuscle)
    }

    @Test
    fun `ueber 60 wird unabhaengig von erfahrung auf 10 bis 14 begrenzt`() {
        for (experience in Experience.entries) {
            val result = VolumeCorridor.forProfile(experience, age = 65)
            assertEquals(10, result.startSetsPerMuscle, "Experience=$experience")
            assertEquals(14, result.maxSetsPerMuscle, "Experience=$experience")
        }
    }

    @Test
    fun `genau 60 jahre gilt noch nicht als ueber 60`() {
        val result = VolumeCorridor.forProfile(Experience.ADVANCED, age = 60)
        assertEquals(14, result.startSetsPerMuscle)
    }

    @Test
    fun `preferHighFrequencySplit nur ab 61`() {
        assertFalse(VolumeCorridor.preferHighFrequencySplit(60))
        assertTrue(VolumeCorridor.preferHighFrequencySplit(61))
    }
}
