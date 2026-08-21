package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.AbstractIntegrationTest
import de.optadata.odil.onshape.auth.AuthResponse
import de.optadata.odil.onshape.auth.RegisterRequest
import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

/** Bewusst OHNE @Transactional, siehe AuthControllerIntegrationTest-Kommentar (Epic Onboarding). */
@SpringBootTest
@AutoConfigureMockMvc
class NutritionEntryControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    private fun registerAndGetToken(): String {
        val email = "nutrition-test-${System.nanoTime()}@example.test"
        val response = mockMvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        ).andReturn().response.contentAsString
        return objectMapper.readValue(response, AuthResponse::class.java).token
    }

    private fun seedFood(nameDe: String, kcal: Double = 100.0, protein: Double = 10.0, fat: Double = 5.0, carbs: Double = 8.0, micros: String = """{"iron_mg":2.0}"""): UUID =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO foods (source, trust, name_de, name_en, kcal, protein_g, fat_g, carbs_g, micros)
            VALUES ('user'::food_source_t, 'estimated'::trust_t, ?, ?, ?, ?, ?, ?, ?::jsonb)
            RETURNING id
            """.trimIndent(),
            UUID::class.java,
            nameDe, nameDe, kcal, protein, fat, carbs, micros,
        )!!

    private fun auth(token: String) = HttpHeaders.AUTHORIZATION to "Bearer $token"

    @Test
    fun `eintrag loggen berechnet naehrwerte serverseitig aus grammzahl`() {
        val token = registerAndGetToken()
        val foodId = seedFood("Testhafer-${System.nanoTime()}", kcal = 370.0, protein = 13.0, fat = 7.0, carbs = 61.0)

        mockMvc.perform(
            post("/api/nutrition/entries")
                .header(auth(token).first, auth(token).second)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "foodId" to foodId, "loggedDate" to "2026-01-15", "slot" to "breakfast",
                    "grams" to 50.0, "method" to "quick_add",
                ))),
        )
            .andExpect(status().isCreated)
            // 370 kcal/100g * 50g = 185
            .andExpect(jsonPath("$.kcal").value(185.0))
            .andExpect(jsonPath("$.proteinG").value(6.5))

        mockMvc.perform(get("/api/nutrition/day").param("date", "2026-01-15").header(auth(token).first, auth(token).second))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalKcal").value(185.0))
            .andExpect(jsonPath("$.totalMicros.iron_mg").value(1.0)) // 2.0 mg/100g * 50g
    }

    @Test
    fun `tagesansicht zeigt den lebensmittelnamen zu jedem eintrag`() {
        val token = registerAndGetToken()
        val name = "Anzeigename-${System.nanoTime()}"
        val foodId = seedFood(name)

        mockMvc.perform(
            post("/api/nutrition/entries").header(auth(token).first, auth(token).second)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "foodId" to foodId, "loggedDate" to "2026-01-23", "slot" to "breakfast", "grams" to 50.0, "method" to "search",
                ))),
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/api/nutrition/day").param("date", "2026-01-23").header(auth(token).first, auth(token).second))
            .andExpect(jsonPath("$.slots[0].entries[0].name").value(name))
    }

    @Test
    fun `multi-select loggt mehrere eintraege in einem aufruf`() {
        val token = registerAndGetToken()
        val food1 = seedFood("Multi-A-${System.nanoTime()}")
        val food2 = seedFood("Multi-B-${System.nanoTime()}")

        val body = listOf(
            mapOf("foodId" to food1, "loggedDate" to "2026-01-16", "slot" to "lunch", "grams" to 100.0, "method" to "search"),
            mapOf("foodId" to food2, "loggedDate" to "2026-01-16", "slot" to "lunch", "grams" to 100.0, "method" to "search"),
        )

        mockMvc.perform(
            post("/api/nutrition/entries/batch").header(auth(token).first, auth(token).second)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)),
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/api/nutrition/day").param("date", "2026-01-16").header(auth(token).first, auth(token).second))
            .andExpect(jsonPath("$.totalKcal").value(200.0))
    }

    @Test
    fun `eintrag loeschen entfernt ihn aus der tagesansicht`() {
        val token = registerAndGetToken()
        val foodId = seedFood("Loeschbar-${System.nanoTime()}")

        val response = mockMvc.perform(
            post("/api/nutrition/entries").header(auth(token).first, auth(token).second)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "foodId" to foodId, "loggedDate" to "2026-01-17", "slot" to "snack", "grams" to 100.0, "method" to "search",
                ))),
        ).andReturn().response.contentAsString
        val entryId = objectMapper.readTree(response).get("id").asText()

        mockMvc.perform(delete("/api/nutrition/entries/$entryId").header(auth(token).first, auth(token).second))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/nutrition/day").param("date", "2026-01-17").header(auth(token).first, auth(token).second))
            .andExpect(jsonPath("$.totalKcal").value(0.0))
    }

    @Test
    fun `tag kopieren dupliziert alle eintraege auf ein neues datum`() {
        val token = registerAndGetToken()
        val foodId = seedFood("Kopierbar-${System.nanoTime()}")

        mockMvc.perform(
            post("/api/nutrition/entries").header(auth(token).first, auth(token).second)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "foodId" to foodId, "loggedDate" to "2026-01-18", "slot" to "dinner", "grams" to 100.0, "method" to "search",
                ))),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/nutrition/entries/copy").header(auth(token).first, auth(token).second)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("fromDate" to "2026-01-18", "toDate" to "2026-01-19"))),
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/nutrition/day").param("date", "2026-01-19").header(auth(token).first, auth(token).second))
            .andExpect(jsonPath("$.totalKcal").value(100.0))
    }

    @Test
    fun `wiederholtes senden derselben client_id erzeugt keinen doppelten eintrag (offline-sync)`() {
        val token = registerAndGetToken()
        val foodId = seedFood("Idempotent-${System.nanoTime()}")
        val clientId = "offline-${UUID.randomUUID()}"
        val requestBody = objectMapper.writeValueAsString(mapOf(
            "foodId" to foodId, "loggedDate" to "2026-01-20", "slot" to "snack", "grams" to 100.0,
            "method" to "quick_add", "clientId" to clientId,
        ))

        repeat(2) {
            mockMvc.perform(
                post("/api/nutrition/entries").header(auth(token).first, auth(token).second)
                    .contentType(MediaType.APPLICATION_JSON).content(requestBody),
            ).andExpect(status().isCreated)
        }

        mockMvc.perform(get("/api/nutrition/day").param("date", "2026-01-20").header(auth(token).first, auth(token).second))
            .andExpect(jsonPath("$.totalKcal").value(100.0))
    }

    @Test
    fun `ein nutzer sieht nie die tagesansicht eines anderen nutzers (row level security)`() {
        val tokenA = registerAndGetToken()
        val tokenB = registerAndGetToken()
        val foodId = seedFood("RLS-Test-${System.nanoTime()}")

        mockMvc.perform(
            post("/api/nutrition/entries").header(auth(tokenA).first, auth(tokenA).second)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "foodId" to foodId, "loggedDate" to "2026-01-21", "slot" to "breakfast", "grams" to 100.0, "method" to "search",
                ))),
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/api/nutrition/day").param("date", "2026-01-21").header(auth(tokenB).first, auth(tokenB).second))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalKcal").value(0.0))
    }

    @Test
    fun `lebensmittelsuche findet gesaeten testartikel und zeigt zuletzt genutzte menge`() {
        val token = registerAndGetToken()
        val unique = "Quinoasalat${System.nanoTime()}"
        val foodId = seedFood(unique)

        mockMvc.perform(
            post("/api/nutrition/entries").header(auth(token).first, auth(token).second)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "foodId" to foodId, "loggedDate" to "2026-01-22", "slot" to "lunch", "grams" to 137.0, "method" to "search",
                ))),
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/api/foods/search").param("q", unique).header(auth(token).first, auth(token).second))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(foodId.toString()))
            .andExpect(jsonPath("$[0].lastUsedGrams").value(137.0))
    }
}
