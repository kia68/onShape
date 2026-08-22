package de.optadata.odil.onshape.training

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MesocycleProgressionTest {

    private val beginnerCorridor = VolumeCorridorResult(startSetsPerMuscle = 8, maxSetsPerMuscle = 10, weeklyIncrement = 0.5)

    @Test
    fun `sechs wochen mesozyklus entspricht der konzept-tabelle exakt`() {
        // §7.4 Schritt 4: Woche1 RIR3, Woche2 RIR2, Woche3 RIR2, Woche4 RIR1, Woche5 RIR0-1, Woche6 Deload RIR4.
        val corridor = VolumeCorridorResult(startSetsPerMuscle = 12, maxSetsPerMuscle = 16, weeklyIncrement = 1.0)
        val stages = MesocycleProgression.stagesFor(corridor, totalWeeks = 6)

        assertEquals(listOf(3, 2, 2, 1, 0, 4), stages.map { it.targetRir })
        assertEquals(listOf(false, false, false, false, false, true), stages.map { it.isDeload })
        assertEquals(listOf(12, 13, 14, 15, 16, 8), stages.map { it.setsPerMuscle })
    }

    @Test
    fun `letzte woche ist immer die deload-woche`() {
        val stages = MesocycleProgression.stagesFor(beginnerCorridor, totalWeeks = 4)
        assertTrue(stages.last().isDeload)
        assertEquals(4, stages.last().targetRir)
    }

    @Test
    fun `deload halbiert das volumen der letzten aufbauwoche`() {
        val stages = MesocycleProgression.stagesFor(beginnerCorridor, totalWeeks = 6)
        val lastBuildWeek = stages[stages.size - 2]
        val deload = stages.last()
        assertEquals((lastBuildWeek.setsPerMuscle / 2.0).let { kotlin.math.round(it).toInt() }, deload.setsPerMuscle)
    }

    @Test
    fun `sets ueberschreiten nie das korridor-maximum`() {
        val corridor = VolumeCorridorResult(startSetsPerMuscle = 14, maxSetsPerMuscle = 15, weeklyIncrement = 1.5)
        val stages = MesocycleProgression.stagesFor(corridor, totalWeeks = 8)
        for (stage in stages.dropLast(1)) {
            assertTrue(stage.setsPerMuscle <= corridor.maxSetsPerMuscle, "Woche ${stage.weekNumber}: ${stage.setsPerMuscle} > ${corridor.maxSetsPerMuscle}")
        }
    }

    @Test
    fun `rir faellt monoton innerhalb der aufbauphase`() {
        val stages = MesocycleProgression.stagesFor(beginnerCorridor, totalWeeks = 6)
        val buildStages = stages.dropLast(1)
        for (i in 1 until buildStages.size) {
            assertTrue(buildStages[i].targetRir <= buildStages[i - 1].targetRir)
        }
    }

    @Test
    fun `weniger als zwei wochen ist ungueltig`() {
        assertFailsWith<IllegalArgumentException> { MesocycleProgression.stagesFor(beginnerCorridor, totalWeeks = 1) }
    }

    @Test
    fun `zwei wochen ist eine aufbauwoche plus deload`() {
        val stages = MesocycleProgression.stagesFor(beginnerCorridor, totalWeeks = 2)
        assertEquals(2, stages.size)
        assertFalse(stages[0].isDeload)
        assertTrue(stages[1].isDeload)
    }
}
