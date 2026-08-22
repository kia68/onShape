package de.optadata.odil.onshape.integrations

import de.optadata.odil.onshape.training.ExerciseCatalogEntry
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExerciseMatcherTest {

    private val backSquatId = UUID.randomUUID()
    private val pullupId = UUID.randomUUID()
    private val farmersCarryId = UUID.randomUUID()
    private val catalog = listOf(
        ExerciseCatalogEntry(backSquatId, "back-squat", "Kniebeuge (Langhantel)", "Back Squat"),
        ExerciseCatalogEntry(pullupId, "pullup", "Klimmzug", "Pull-Up"),
        ExerciseCatalogEntry(farmersCarryId, "farmers-carry", "Farmer's Carry", "Farmer's Carry"),
    )

    @Test
    fun `exakter treffer unabhaengig von gross-klein und satzzeichen`() {
        assertEquals(pullupId, ExerciseMatcher.match(catalog, "pull-up"))
        assertEquals(farmersCarryId, ExerciseMatcher.match(catalog, "Farmer's Carry"))
    }

    @Test
    fun `wortreihenfolge spielt keine rolle`() {
        assertEquals(backSquatId, ExerciseMatcher.match(catalog, "Squat Back"))
    }

    @Test
    fun `alias findet unterschiedlich benannte katalogeintraege`() {
        assertEquals(backSquatId, ExerciseMatcher.match(catalog, "Squat (Barbell)"))
    }

    @Test
    fun `unbekannte uebung bleibt unmatched statt geraten zu werden`() {
        assertNull(ExerciseMatcher.match(catalog, "Nordic Hamstring Curl"))
    }

    @Test
    fun `leerer name bleibt unmatched`() {
        assertNull(ExerciseMatcher.match(catalog, "   "))
    }
}
