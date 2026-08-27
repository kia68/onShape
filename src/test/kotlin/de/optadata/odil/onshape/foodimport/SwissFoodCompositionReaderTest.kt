package de.optadata.odil.onshape.foodimport

import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SwissFoodCompositionReaderTest {

    // Spaltennamen mit eingebettetem Komma muessen wie im echten Export gequotet sein,
    // sonst zerlegt der RFC-4180-Parser sie faelschlich in zwei Spalten.
    private val header = "ID,name D,name F,name E,category D,protein,unit,source," +
        "\"fat, total\",unit,source,\"carbohydrates, available\",unit,source," +
        "dietary fibres,unit,source,energy kcal,unit,source"

    private fun readerFor(vararg rows: String): List<ImportedFood> {
        val file = createTempFile(suffix = ".csv")
        file.writeText((listOf(header) + rows).joinToString("\n"))
        return SwissFoodCompositionReader(file.toString()).fetchDelta()
    }

    @Test
    fun `empty path yields no entries`() {
        assertTrue(SwissFoodCompositionReader("").fetchDelta().isEmpty())
    }

    @Test
    fun `missing file yields no entries`() {
        assertTrue(SwissFoodCompositionReader("does/not/exist.csv").fetchDelta().isEmpty())
    }

    @Test
    fun `parses a well-formed row by column name`() {
        val result = readerFor("123,Apfel,Pomme,Apple,Fruit,0.3,g,BLV,0.2,g,BLV,14,g,BLV,2.4,g,BLV,52,kcal,BLV")

        assertEquals(1, result.size)
        val food = result.first()
        assertEquals(FoodSource.NAEHRWERTDATEN_CH, food.source)
        assertEquals("123", food.sourceId)
        assertEquals("Apfel", food.nameDe)
        assertEquals("Apple", food.nameEn)
        assertEquals("Fruit", food.category)
        assertEquals(52.0, food.kcal)
        assertEquals(0.3, food.proteinG)
        assertEquals(0.2, food.fatG)
        assertEquals(14.0, food.carbsG)
        assertEquals(2.4, food.fiberG)
        assertNull(food.saltG)
        assertNull(food.barcode)
    }

    @Test
    fun `row without kcal is skipped`() {
        val result = readerFor("124,Wasser,Eau,Water,Beverage,0,g,BLV,0,g,BLV,0,g,BLV,0,g,BLV,,kcal,BLV")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `comma decimal separator is normalized`() {
        val food = readerFor("125,Banane,Banane,Banana,Fruit,\"1,1\",g,BLV,0,g,BLV,23,g,BLV,2,g,BLV,89,kcal,BLV").first()
        assertEquals(1.1, food.proteinG)
    }

    @Test
    fun `missing english name falls back to german name`() {
        val food = readerFor("126,Birne,Poire,,Fruit,0.4,g,BLV,0.1,g,BLV,15,g,BLV,3,g,BLV,57,kcal,BLV").first()
        assertEquals("Birne", food.nameEn)
    }
}
