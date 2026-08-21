package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.AbstractIntegrationTest
import de.optadata.odil.onshape.auth.AuthResponse
import de.optadata.odil.onshape.auth.RegisterRequest
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

/** Wasser (FR-29), Koerpermasse (FR-30), eigene Meals (FR-25), Rezepte (FR-26). Bewusst OHNE
 * @Transactional, siehe AuthControllerIntegrationTest-Kommentar (Epic Onboarding). */
@SpringBootTest
@AutoConfigureMockMvc
class NutritionAncillaryControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    private fun registerAndGetToken(): String {
        val email = "nutrition-anc-test-${System.nanoTime()}@example.test"
        val response = mockMvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        ).andReturn().response.contentAsString
        return objectMapper.readValue(response, AuthResponse::class.java).token
    }

    private fun seedFood(nameDe: String, kcal: Double = 100.0, protein: Double = 10.0, fat: Double = 5.0, carbs: Double = 8.0): UUID =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO foods (source, trust, name_de, name_en, kcal, protein_g, fat_g, carbs_g)
            VALUES ('user'::food_source_t, 'estimated'::trust_t, ?, ?, ?, ?, ?, ?) RETURNING id
            """.trimIndent(),
            UUID::class.java,
            nameDe, nameDe, kcal, protein, fat, carbs,
        )!!

    private fun bearer(token: String) = "Bearer $token"

    @Test
    fun `wasser loggen und tagesziel abfragen`() {
        val token = registerAndGetToken()

        mockMvc.perform(
            post("/api/nutrition/water").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("loggedDate" to "2026-02-01", "amountMl" to 500))),
        ).andExpect(status().isCreated)
        mockMvc.perform(
            post("/api/nutrition/water").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("loggedDate" to "2026-02-01", "amountMl" to 250))),
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/api/nutrition/water").param("date", "2026-02-01").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalMl").value(750))
    }

    @Test
    fun `koerpermasse aufzeichnen und historie abrufen`() {
        val token = registerAndGetToken()

        mockMvc.perform(
            post("/api/measurements").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "measuredOn" to "2026-02-02", "weightKg" to 79.5, "waistCm" to 84.0, "hipCm" to 98.0,
                ))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.weightKg").value(79.5))
            .andExpect(jsonPath("$.waistCm").value(84.0))

        mockMvc.perform(
            get("/api/measurements").param("from", "2026-01-01").param("to", "2026-12-31")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].weightKg").value(79.5))
    }

    @Test
    fun `eigenes meal speichern und komplett verbuchen`() {
        val token = registerAndGetToken()
        val food1 = seedFood("Meal-Zutat-A-${System.nanoTime()}", kcal = 200.0)
        val food2 = seedFood("Meal-Zutat-B-${System.nanoTime()}", kcal = 50.0)

        val createResponse = mockMvc.perform(
            post("/api/nutrition/meals").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "name" to "Mein Fruehstueck",
                    "items" to listOf(mapOf("foodId" to food1, "grams" to 100.0), mapOf("foodId" to food2, "grams" to 100.0)),
                ))),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val mealId = objectMapper.readTree(createResponse).get("id").asText()

        mockMvc.perform(
            post("/api/nutrition/meals/$mealId/log").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("loggedDate" to "2026-02-03", "slot" to "breakfast"))),
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/api/nutrition/day").param("date", "2026-02-03").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(jsonPath("$.totalKcal").value(250.0))
    }

    @Test
    fun `rezept anlegen berechnet naehrwerte pro portion aus den zutaten`() {
        val token = registerAndGetToken()
        val flour = seedFood("Mehl-${System.nanoTime()}", kcal = 364.0, protein = 10.0, fat = 1.0, carbs = 76.0)

        val response = mockMvc.perform(
            post("/api/nutrition/recipes").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "name" to "Testbrot", "servings" to 4.0,
                    "items" to listOf(mapOf("foodId" to flour, "grams" to 400.0)),
                ))),
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        // 400g Mehl bei 364 kcal/100g = 1456 kcal gesamt, / 4 Portionen = 364 kcal/Portion
        val json = objectMapper.readTree(response)
        kotlin.test.assertEquals(364.0, json.get("perServingKcal").asDouble(), 0.01)
    }

    @Test
    fun `rezept verbuchen erzeugt einen food_entries eintrag proportional zur grammzahl`() {
        val token = registerAndGetToken()
        val flour = seedFood("Mehl2-${System.nanoTime()}", kcal = 364.0, protein = 10.0, fat = 1.0, carbs = 76.0)

        val createResponse = mockMvc.perform(
            post("/api/nutrition/recipes").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "name" to "Testbrot2", "servings" to 4.0,
                    "items" to listOf(mapOf("foodId" to flour, "grams" to 400.0)),
                ))),
        ).andReturn().response.contentAsString
        val recipeId = objectMapper.readTree(createResponse).get("id").asText()

        mockMvc.perform(
            post("/api/nutrition/entries").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "recipeId" to recipeId, "loggedDate" to "2026-02-04", "slot" to "dinner", "grams" to 100.0, "method" to "recipe",
                ))),
        )
            .andExpect(status().isCreated)
            // Rezept gesamt: 1456 kcal / 400g = 3.64 kcal/g * 100g gegessen = 364 kcal
            .andExpect(jsonPath("$.kcal").value(364.0))
    }
}
