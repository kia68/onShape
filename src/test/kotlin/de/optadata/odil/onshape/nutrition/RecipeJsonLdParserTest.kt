package de.optadata.odil.onshape.nutrition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipeJsonLdParserTest {

    private fun html(jsonLd: String) = """
        <html><head>
        <script type="application/ld+json">
        $jsonLd
        </script>
        </head><body>ignored</body></html>
    """.trimIndent()

    @Test
    fun `einzelnes recipe-objekt mit howtostep-anleitung wird geparst`() {
        val jsonLd = """
            {
              "@context": "https://schema.org/",
              "@type": "Recipe",
              "name": "Non-Alcoholic Pina Colada",
              "recipeIngredient": ["400ml pineapple juice", "100ml cream of coconut", "ice"],
              "recipeInstructions": [
                {"@type": "HowToStep", "text": "Blend the pineapple juice and cream of coconut."},
                {"@type": "HowToStep", "text": "Add ice and blend until smooth."}
              ],
              "recipeYield": "4 servings"
            }
        """.trimIndent()

        val result = RecipeJsonLdParser.parse(html(jsonLd))
        assertTrue(result != null)
        assertEquals("Non-Alcoholic Pina Colada", result.name)
        assertEquals(4.0, result.servings)
        assertEquals(3, result.ingredientLines.size)
        assertEquals("400ml pineapple juice", result.ingredientLines[0])
        assertEquals("1. Blend the pineapple juice and cream of coconut.\n2. Add ice and blend until smooth.", result.instructions)
    }

    @Test
    fun `recipe als teil eines json-arrays mehrerer typen wird gefunden`() {
        val jsonLd = """
            [
              {"@type": "WebSite", "name": "Some Food Blog"},
              {
                "@type": "Recipe",
                "name": "Banana Bread",
                "recipeIngredient": ["3 very ripe bananas, mashed", "1 cup all-purpose flour"],
                "recipeInstructions": "Mix everything and bake."
              }
            ]
        """.trimIndent()

        val result = RecipeJsonLdParser.parse(html(jsonLd))
        assertTrue(result != null)
        assertEquals("Banana Bread", result.name)
        assertEquals("1. Mix everything and bake.", result.instructions)
    }

    @Test
    fun `recipe innerhalb von at-graph wird gefunden -- yoast-seo-muster`() {
        val jsonLd = """
            {
              "@context": "https://schema.org",
              "@graph": [
                {"@type": "BreadcrumbList", "itemListElement": []},
                {
                  "@type": "Recipe",
                  "name": "Simple Pasta",
                  "recipeIngredient": ["200g pasta", "2 tbsp olive oil"],
                  "recipeInstructions": [
                    {
                      "@type": "HowToSection",
                      "name": "Cooking",
                      "itemListElement": [
                        {"@type": "HowToStep", "text": "Boil the pasta."},
                        {"@type": "HowToStep", "text": "Toss with olive oil."}
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = RecipeJsonLdParser.parse(html(jsonLd))
        assertTrue(result != null)
        assertEquals("Simple Pasta", result.name)
        assertEquals(2, result.ingredientLines.size)
        assertEquals("1. Boil the pasta.\n2. Toss with olive oil.", result.instructions)
    }

    @Test
    fun `recipeyield als reine zahl wird uebernommen`() {
        val jsonLd = """{"@type": "Recipe", "name": "X", "recipeIngredient": ["1 egg"], "recipeYield": 6}"""
        val result = RecipeJsonLdParser.parse(html(jsonLd))
        assertEquals(6.0, result?.servings)
    }

    @Test
    fun `seite ohne json-ld liefert null`() {
        assertNull(RecipeJsonLdParser.parse("<html><body>Kein Rezept hier.</body></html>"))
    }

    @Test
    fun `json-ld ohne recipe-typ liefert null`() {
        val jsonLd = """{"@type": "WebSite", "name": "Nicht relevant"}"""
        assertNull(RecipeJsonLdParser.parse(html(jsonLd)))
    }

    @Test
    fun `recipe ohne zutatenliste liefert null -- kein sinnvoller import moeglich`() {
        val jsonLd = """{"@type": "Recipe", "name": "Leer"}"""
        assertNull(RecipeJsonLdParser.parse(html(jsonLd)))
    }

    @Test
    fun `kaputtes json in einem script-block wird uebersprungen, nachfolgender block noch gelesen`() {
        val broken = """<script type="application/ld+json">{not valid json</script>"""
        val valid = """
            <script type="application/ld+json">
            {"@type": "Recipe", "name": "Gueltig", "recipeIngredient": ["1 egg"]}
            </script>
        """.trimIndent()
        val result = RecipeJsonLdParser.parse("<html><head>$broken$valid</head></html>")
        assertEquals("Gueltig", result?.name)
    }
}
