package de.optadata.odil.onshape.nutrition

import tools.jackson.databind.json.JsonMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MicroNutrientsTest {

    private val objectMapper = JsonMapper.builder().build()

    @Test
    fun `parse liefert leere map fuer null oder leer`() {
        assertEquals(emptyMap(), MicroNutrients.parse(objectMapper, null))
        assertEquals(emptyMap(), MicroNutrients.parse(objectMapper, ""))
        assertEquals(emptyMap(), MicroNutrients.parse(objectMapper, "  "))
    }

    @Test
    fun `parse liest jsonb-map`() {
        val result = MicroNutrients.parse(objectMapper, """{"iron_mg":2.1,"vitamin_d_ug":1.2}""")
        assertEquals(2.1, result["iron_mg"])
        assertEquals(1.2, result["vitamin_d_ug"])
    }

    @Test
    fun `scale skaliert per-100g werte auf tatsaechliche grammzahl`() {
        val per100g = mapOf("iron_mg" to 2.0)
        val scaled = MicroNutrients.scale(per100g, 250.0)
        assertEquals(5.0, scaled["iron_mg"])
    }

    @Test
    fun `sum addiert ueber mehrere eintraege und behandelt unterschiedliche schluessel`() {
        val sum = MicroNutrients.sum(listOf(mapOf("iron_mg" to 1.0, "zinc_mg" to 2.0), mapOf("iron_mg" to 3.0)))
        assertEquals(4.0, sum["iron_mg"])
        assertEquals(2.0, sum["zinc_mg"])
    }

    @Test
    fun `sum von leerer liste ist leere map`() {
        assertTrue(MicroNutrients.sum(emptyList()).isEmpty())
    }

    @Test
    fun `toJsonb roundtrip ueber parse`() {
        val original = mapOf("iron_mg" to 2.5, "zinc_mg" to 1.1)
        val jsonb = MicroNutrients.toJsonb(objectMapper, original)
        val roundtripped = MicroNutrients.parse(objectMapper, jsonb.value)
        assertEquals(original, roundtripped)
    }
}
