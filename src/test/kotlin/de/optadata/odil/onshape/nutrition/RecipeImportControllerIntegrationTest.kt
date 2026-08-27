package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.AbstractIntegrationTest
import de.optadata.odil.onshape.auth.AuthResponse
import de.optadata.odil.onshape.auth.RegisterRequest
import java.net.URL
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

/** FR-27. Ersetzt [RecipeUrlFetcher] durch eine Fake-Implementierung (siehe [FetcherConfig])
 * statt echtem Netzwerk -- gleiches Prinzip wie bei den `FoodSourceClient`-Implementierungen.
 * Eigener, isolierter Spring-Kontext durch die zusaetzliche `@TestConfiguration`, siehe
 * [de.optadata.odil.onshape.security.AuthRateLimitFilterIntegrationTest] fuer denselben Effekt
 * durch abweichende Properties. Bewusst OHNE @Transactional, siehe
 * AuthControllerIntegrationTest-Kommentar (Epic Onboarding). */
@SpringBootTest
@AutoConfigureMockMvc
class RecipeImportControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    @Autowired lateinit var fakeFetcher: FakeRecipeUrlFetcher

    @TestConfiguration
    class FetcherConfig {
        @Bean
        @Primary
        fun recipeUrlFetcher(): FakeRecipeUrlFetcher = FakeRecipeUrlFetcher()
    }

    class FakeRecipeUrlFetcher : RecipeUrlFetcher {
        var html: String? = null
        override fun fetchHtml(url: URL): String? = html
    }

    private fun registerAndGetToken(): String {
        val email = "recipe-import-test-${System.nanoTime()}@example.test"
        val response = mockMvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        ).andReturn().response.contentAsString
        return objectMapper.readValue(response, AuthResponse::class.java).token
    }

    private fun bearer(token: String) = "Bearer $token"

    /** Gleicher Name in beiden Sprachspalten -- vermeidet, dass der Test von der (Default-)
     * Locale des Import-Aufrufs abhaengt, siehe [FoodSearchRepository.search]. */
    private fun seedFood(name: String, kcal: Double): UUID =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO foods (source, trust, name_de, name_en, kcal, protein_g, fat_g, carbs_g)
            VALUES ('user'::food_source_t, 'estimated'::trust_t, ?, ?, ?, 1, 0, 10) RETURNING id
            """.trimIndent(),
            UUID::class.java, name, name, kcal,
        )!!

    private fun recipeHtml(jsonLd: String) = """
        <html><head><script type="application/ld+json">$jsonLd</script></head><body></body></html>
    """.trimIndent()

    @Test
    fun `import liefert einen entwurf mit geparsten zutaten und lebensmittel-vorschlaegen`() {
        val foodName = "Ananassaftimporttest${System.nanoTime()}"
        val foodId = seedFood(foodName, 54.0)
        fakeFetcher.html = recipeHtml(
            """{"@type":"Recipe","name":"Test-Rezept","recipeIngredient":["400ml $foodName","1 cup ice"],"recipeInstructions":"Alles mischen.","recipeYield":"2 servings"}""",
        )
        val token = registerAndGetToken()

        val response = mockMvc.perform(
            post("/api/nutrition/recipes/import").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("url" to "https://8.8.8.8/recipe"))),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Test-Rezept"))
            .andExpect(jsonPath("$.servings").value(2.0))
            .andExpect(jsonPath("$.ingredients.length()").value(2))
            .andExpect(jsonPath("$.ingredients[0].gramsResolved").value(400.0))
            .andExpect(jsonPath("$.ingredients[1].unit").value("cup"))
            .andExpect(jsonPath("$.ingredients[1].gramsResolved").doesNotExist())
            .andReturn().response.contentAsString

        val json = objectMapper.readTree(response)
        val suggestions = json.get("ingredients").get(0).get("suggestions")
        assertTrueSuggestionsContainFood(suggestions, foodId)
    }

    /** Bewusst eine for-Schleife statt `.map` -- `JsonNode` bringt in Jackson 3.x eine eigene
     * `map(...)`-Methode mit (Baum-Transformation), die Kotlins `Iterable.map`-Extension bei
     * gleichlautendem Aufruf verdeckt und hier zu falschen/leeren Ergebnissen fuehrte. */
    private fun assertTrueSuggestionsContainFood(suggestions: tools.jackson.databind.JsonNode, foodId: UUID) {
        val ids = mutableListOf<String>()
        for (suggestion in suggestions) ids += suggestion.get("id").asString()
        kotlin.test.assertTrue(foodId.toString() in ids, "Erwartete Uebereinstimmung $foodId nicht unter den Vorschlaegen $ids")
    }

    @Test
    fun `ungueltige url wird abgelehnt bevor irgendein fetch versucht wird`() {
        val token = registerAndGetToken()
        mockMvc.perform(
            post("/api/nutrition/recipes/import").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("url" to "http://127.0.0.1/actuator/env"))),
        ).andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.code").value("invalid_recipe_url"))
    }

    @Test
    fun `seite ohne json-ld-rezept liefert recipe_import_failed`() {
        fakeFetcher.html = "<html><body>Kein Rezept.</body></html>"
        val token = registerAndGetToken()
        mockMvc.perform(
            post("/api/nutrition/recipes/import").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("url" to "https://8.8.8.8/no-recipe"))),
        ).andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.code").value("recipe_import_failed"))
    }

    @Test
    fun `nicht erreichbare seite liefert ebenfalls recipe_import_failed`() {
        fakeFetcher.html = null
        val token = registerAndGetToken()
        mockMvc.perform(
            post("/api/nutrition/recipes/import").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("url" to "https://8.8.8.8/down"))),
        ).andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.code").value("recipe_import_failed"))
    }
}
