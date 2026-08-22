package de.optadata.odil.onshape.barcode

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

/** Bewusst OHNE @Transactional, siehe AuthControllerIntegrationTest-Kommentar (Epic Onboarding). */
@SpringBootTest
@AutoConfigureMockMvc
class BarcodeControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    private fun registerAndGetToken(): String {
        val email = "barcode-test-${System.nanoTime()}@example.test"
        val response = mockMvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        ).andReturn().response.contentAsString
        return objectMapper.readValue(response, AuthResponse::class.java).token
    }

    private fun bearer(token: String) = "Bearer $token"

    /** Vervollstaendigt das Onboarding, damit Profil-abhaengige Fit-Score-Komponenten
     * (Allergene, Ernaehrungspraeferenzen, Ziel, Tagesziel) real greifen. */
    private fun completeOnboarding(token: String, goal: String = "maintain", allergens: List<String> = emptyList(), dietaryPrefs: List<String> = emptyList()) {
        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/onboarding/profile")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "sex" to "unspecified", "birthDate" to "1996-01-01", "heightCm" to 180.0, "weightKg" to 80.0,
                    "experience" to "beginner", "activityPal" to 1.4, "goal" to goal, "goalRatePctWeek" to 0.0,
                    "dietaryPrefs" to dietaryPrefs, "allergens" to allergens, "injuries" to emptyList<String>(),
                    "equipment" to listOf("bodyweight"), "trainingDaysWeek" to 3, "sessionMinutes" to 45,
                    "healthScreening" to mapOf("heartCondition" to false, "pregnancy" to false, "recentInjury" to false, "medication" to false),
                ))),
        ).andExpect(status().isOk)
    }

    private fun seedFood(
        barcode: String,
        nameDe: String,
        kcal: Double = 200.0,
        sugar: Double = 5.0,
        nova: Int = 2,
        category: String = "snacks-${UUID.randomUUID()}",
        allergens: List<String> = emptyList(),
    ): UUID =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO foods (source, trust, barcode, name_de, name_en, category, nova_group, kcal, protein_g, fat_g, carbs_g, sugar_g, allergens)
            VALUES ('user'::food_source_t, 'community'::trust_t, ?, ?, ?, ?, ?, ?, 5, 5, 20, ?, ?::text[])
            RETURNING id
            """.trimIndent(),
            UUID::class.java,
            barcode, nameDe, nameDe, category, nova, kcal, sugar, allergens.joinToString(",", prefix = "{", postfix = "}"),
        )!!

    @Test
    fun `scan eines bekannten barcodes liefert fit-score und produktdaten`() {
        val token = registerAndGetToken()
        completeOnboarding(token)
        val barcode = "5000000000${(10000..99999).random()}"
        seedFood(barcode, "Scan-Test-Produkt")

        mockMvc.perform(
            post("/api/barcode/scan").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("barcode" to barcode, "date" to "2026-03-01"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.found").value(true))
            .andExpect(jsonPath("$.product.name").value("Scan-Test-Produkt"))
            .andExpect(jsonPath("$.score").isNumber)
    }

    @Test
    fun `scan eines unbekannten barcodes liefert found false ohne fehler`() {
        val token = registerAndGetToken()
        completeOnboarding(token)
        val unknownBarcode = "5999999999${(10000..99999).random()}"

        mockMvc.perform(
            post("/api/barcode/scan").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("barcode" to unknownBarcode, "date" to "2026-03-01"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.found").value(false))
            .andExpect(jsonPath("$.product").doesNotExist())
    }

    @Test
    fun `allergen im nutzerprofil wird beim scan erkannt und score auf 0 gesetzt`() {
        val token = registerAndGetToken()
        completeOnboarding(token, allergens = listOf("milk"))
        val barcode = "5100000000${(10000..99999).random()}"
        seedFood(barcode, "Milchprodukt", allergens = listOf("milk"))

        mockMvc.perform(
            post("/api/barcode/scan").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("barcode" to barcode, "date" to "2026-03-01"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.score").value(0))
            .andExpect(jsonPath("$.allergenMatches[0]").value("milk"))
    }

    @Test
    fun `deutlich besseres produkt derselben kategorie wird als alternative vorgeschlagen`() {
        val token = registerAndGetToken()
        completeOnboarding(token)
        val category = "vergleichskategorie-${UUID.randomUUID()}"
        val worseBarcode = "5200000000${(10000..99999).random()}"
        seedFood(worseBarcode, "Schlechte Wahl", kcal = 500.0, sugar = 40.0, nova = 4, category = category)
        seedFood("5200000001${(10000..99999).random()}", "Bessere Wahl", kcal = 150.0, sugar = 2.0, nova = 1, category = category)

        mockMvc.perform(
            post("/api/barcode/scan").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("barcode" to worseBarcode, "date" to "2026-03-01"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.alternatives[0].product.name").value("Bessere Wahl"))
    }

    @Test
    fun `manuell angelegtes produkt ist danach per barcode scannbar (FR-49)`() {
        val token = registerAndGetToken()
        completeOnboarding(token)
        val barcode = "5300000000${(10000..99999).random()}"

        mockMvc.perform(
            post("/api/barcode/products").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "barcode" to barcode, "nameDe" to "Selbst angelegt", "kcal" to 300.0,
                    "proteinG" to 8.0, "fatG" to 10.0, "carbsG" to 40.0,
                ))),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/barcode/scan").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("barcode" to barcode, "date" to "2026-03-01"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.found").value(true))
            .andExpect(jsonPath("$.product.name").value("Selbst angelegt"))
            .andExpect(jsonPath("$.product.trust").value("community"))
    }

    @Test
    fun `scan ohne abgeschlossenes onboarding funktioniert mit neutralem kontext`() {
        val token = registerAndGetToken()
        val barcode = "5400000000${(10000..99999).random()}"
        seedFood(barcode, "Ohne Profil Testprodukt")

        mockMvc.perform(
            post("/api/barcode/scan").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("barcode" to barcode, "date" to "2026-03-01"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.found").value(true))
            .andExpect(jsonPath("$.score").isNumber)
    }
}
