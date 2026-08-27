package de.optadata.odil.onshape.nutrition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IngredientLineParserTest {

    @Test
    fun `milliliter wird direkt als gramm uebernommen -- dichte-annahme wasser`() {
        val result = IngredientLineParser.parse("400ml pineapple juice")
        assertEquals(400.0, result.quantity)
        assertEquals("ml", result.unit)
        assertEquals("pineapple juice", result.ingredientName)
        assertEquals(400.0, result.gramsResolved)
    }

    @Test
    fun `gramm wird eins zu eins uebernommen`() {
        val result = IngredientLineParser.parse("200 g sugar")
        assertEquals(200.0, result.quantity)
        assertEquals(200.0, result.gramsResolved)
        assertEquals("sugar", result.ingredientName)
    }

    @Test
    fun `kilogramm wird mit 1000 multipliziert`() {
        val result = IngredientLineParser.parse("1.5kg flour")
        assertEquals(1.5, result.quantity)
        assertEquals(1500.0, result.gramsResolved)
    }

    @Test
    fun `liter wird mit 1000 multipliziert`() {
        val result = IngredientLineParser.parse("2 l milk")
        assertEquals(2000.0, result.gramsResolved)
    }

    @Test
    fun `cup hat keine feste dichte -- keine gramm-aufloesung, aber menge und einheit erkannt`() {
        val result = IngredientLineParser.parse("1 cup all-purpose flour")
        assertEquals(1.0, result.quantity)
        assertEquals("cup", result.unit)
        assertEquals("all-purpose flour", result.ingredientName)
        assertNull(result.gramsResolved)
    }

    @Test
    fun `tablespoon-abkuerzung wird erkannt, aber nicht in gramm umgerechnet`() {
        val result = IngredientLineParser.parse("1 tbsp olive oil")
        assertEquals(1.0, result.quantity)
        assertEquals("tbsp", result.unit)
        assertNull(result.gramsResolved)
    }

    @Test
    fun `gemischte zahl aus ganzzahl und bruch wird korrekt addiert`() {
        val result = IngredientLineParser.parse("1 1/2 cups sugar")
        assertEquals(1.5, result.quantity)
    }

    @Test
    fun `reiner bruch ohne ganzzahl wird korrekt geparst`() {
        val result = IngredientLineParser.parse("1/2 cup butter")
        assertEquals(0.5, result.quantity)
    }

    @Test
    fun `unicode-bruchzeichen wird korrekt geparst`() {
        val result = IngredientLineParser.parse("½ cup butter")
        assertEquals(0.5, result.quantity)
    }

    @Test
    fun `dezimalkomma wird wie dezimalpunkt behandelt`() {
        val result = IngredientLineParser.parse("2,5 kg potatoes")
        assertEquals(2.5, result.quantity)
    }

    @Test
    fun `zeile ohne erkennbare einheit -- rest bleibt teil des namens`() {
        val result = IngredientLineParser.parse("3 very ripe bananas, mashed")
        assertEquals(3.0, result.quantity)
        assertNull(result.unit)
        assertEquals("very ripe bananas, mashed", result.ingredientName)
    }

    @Test
    fun `zeile ganz ohne menge liefert nur den namen`() {
        val result = IngredientLineParser.parse("Salt to taste")
        assertNull(result.quantity)
        assertEquals("Salt to taste", result.ingredientName)
    }

    @Test
    fun `rohtext bleibt unveraendert erhalten`() {
        val raw = "  400ml pineapple juice  "
        val result = IngredientLineParser.parse(raw)
        assertEquals("400ml pineapple juice", result.rawText)
    }
}
