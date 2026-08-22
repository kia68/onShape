package de.optadata.odil.onshape.movement

import de.optadata.odil.onshape.onboarding.Sex
import de.optadata.odil.onshape.training.Mechanic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StartingWeightRecommenderTest {

    @Test
    fun `langhantel-uebungen empfehlen immer die leere stange`() {
        val result = StartingWeightRecommender.recommend(listOf("barbell"), Mechanic.COMPOUND, Sex.MALE, 90.0)
        assertEquals(20.0, result.weightKg)
        assertEquals("barbell_empty", result.reasonCode)
    }

    @Test
    fun `reine koerpergewichtsuebungen brauchen keine empfehlung`() {
        val result = StartingWeightRecommender.recommend(listOf("bodyweight"), Mechanic.COMPOUND, Sex.FEMALE, 65.0)
        assertNull(result.weightKg)
        assertEquals("bodyweight_only", result.reasonCode)
    }

    @Test
    fun `kurzhantel-verbundsuebung skaliert mit koerpergewicht und geschlecht`() {
        val male = StartingWeightRecommender.recommend(listOf("dumbbells"), Mechanic.COMPOUND, Sex.MALE, 80.0)
        val female = StartingWeightRecommender.recommend(listOf("dumbbells"), Mechanic.COMPOUND, Sex.FEMALE, 80.0)
        // 80kg x 0.20 = 16.0 -> auf den naechsten 2,5kg-Schritt gerundet = 15.0
        assertEquals(15.0, male.weightKg)
        assert(male.weightKg!! > female.weightKg!!)
        assertEquals("bodyweight_relative", male.reasonCode)
    }

    @Test
    fun `isolationsuebung mit kurzhantel bekommt einen niedrigeren faktor als eine verbundsuebung`() {
        val compound = StartingWeightRecommender.recommend(listOf("dumbbells"), Mechanic.COMPOUND, Sex.MALE, 80.0)
        val isolation = StartingWeightRecommender.recommend(listOf("dumbbells"), Mechanic.ISOLATION, Sex.MALE, 80.0)
        assert(isolation.weightKg!! < compound.weightKg!!)
    }

    @Test
    fun `empfehlung wird nie unter den kleinsten gewichtsschritt gerundet`() {
        val result = StartingWeightRecommender.recommend(listOf("dumbbells"), Mechanic.ISOLATION, Sex.FEMALE, 40.0)
        assert(result.weightKg!! >= 2.5)
    }

    @Test
    fun `unbekanntes equipment liefert keine empfehlung`() {
        val result = StartingWeightRecommender.recommend(listOf("gym"), Mechanic.COMPOUND, Sex.MALE, 80.0)
        assertNull(result.weightKg)
        assertEquals("no_recommendation", result.reasonCode)
    }
}
