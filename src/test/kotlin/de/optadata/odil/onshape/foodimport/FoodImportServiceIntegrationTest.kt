package de.optadata.odil.onshape.foodimport

import de.optadata.odil.onshape.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
@Transactional
class FoodImportServiceIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var service: FoodImportService

    @Autowired
    lateinit var repository: FoodImportRepository

    private fun food(sourceId: String, barcode: String? = null, nameDe: String = "Testprodukt $sourceId") =
        ImportedFood(
            source = FoodSource.OFF,
            sourceId = sourceId,
            barcode = barcode,
            brand = "Testmarke",
            nameDe = nameDe,
            nameEn = "Test product $sourceId",
            kcal = 250.0,
            proteinG = 10.0,
            fatG = 8.0,
            carbsG = 30.0,
            fiberG = 3.0,
            micros = mapOf("iron_mg" to 1.2),
            allergens = listOf("gluten"),
        )

    @Test
    fun `plausible new food is inserted and findable by barcode`() {
        val decision = service.importOne(food(sourceId = "off-int-1", barcode = "4111111111111"))
        assertIs<DedupDecision.InsertNew>(decision)

        val found = repository.findByBarcode("4111111111111")
        assertNotNull(found)
        assertEquals(FoodSource.OFF, found.source)
        assertEquals(TrustLevel.COMMUNITY, found.trust)
    }

    @Test
    fun `re-importing the same source id updates the existing row instead of duplicating`() {
        val first = service.importOne(food(sourceId = "off-int-2", barcode = "4222222222222"))
        assertIs<DedupDecision.InsertNew>(first)

        val second = service.importOne(
            food(sourceId = "off-int-2", barcode = "4222222222222", nameDe = "Testprodukt aktualisiert"),
        )
        assertIs<DedupDecision.UpdateExisting>(second)
    }

    @Test
    fun `implausible candidate is rejected and never reaches the database`() {
        val implausible = food(sourceId = "off-int-3", barcode = "4333333333333").copy(
            kcal = 0.0,
            proteinG = 0.0,
            fatG = 0.0,
            carbsG = 0.0,
        )
        val decision = service.importOne(implausible)
        assertNull(decision)
        assertNull(repository.findByBarcode("4333333333333"))
    }
}
