package de.optadata.odil.onshape.partnerapi

import de.optadata.odil.onshape.AbstractIntegrationTest
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

/** SCALE-03. Diese Klasse ruft `/api/partner/v1/register` mehrfach auf (je Testmethode eine neue
 * Partnerregistrierung, fuer Testisolation) -- das produktive Registrierungs-Limit (5/Stunde,
 * application.properties) wuerde das ueberschreiten, daher hier grosszuegig ueberschrieben, gleiches
 * Muster wie [de.optadata.odil.onshape.security.AuthRateLimitFilterIntegrationTest] fuer
 * `/api/auth/register`. Das eigene, isolierte Property-Set erzeugt einen eigenen Spring-Kontext.
 * Bewusst OHNE @Transactional, siehe AuthControllerIntegrationTest-Kommentar (Epic Onboarding). */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = ["app.partner-api.registration-rate-limit.max-requests=1000"])
class PartnerApiControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    private fun registerPartnerAndGetKey(): String {
        val response = mockMvc.perform(
            post("/api/partner/v1/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterPartnerRequest("Test-Partner GmbH", "partner-${System.nanoTime()}@example.test"))),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        return objectMapper.readValue(response, RegisterPartnerResponse::class.java).apiKey
    }

    private fun seedFood(barcode: String): Unit {
        jdbcTemplate.update(
            """
            INSERT INTO foods (source, trust, barcode, name_de, name_en, category, nova_group, kcal, protein_g, fat_g, carbs_g, sugar_g, allergens)
            VALUES ('user'::food_source_t, 'community'::trust_t, ?, 'Partner-Test-Produkt', 'Partner-Test-Produkt', 'snacks', 2, 250, 6, 8, 30, 12, '{}'::text[])
            """.trimIndent(),
            barcode,
        )
    }

    @Test
    fun `register liefert einen einmalig sichtbaren key`() {
        val response = mockMvc.perform(
            post("/api/partner/v1/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterPartnerRequest("Studio X", "studio-${System.nanoTime()}@example.test"))),
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val body = objectMapper.readValue(response, RegisterPartnerResponse::class.java)
        assert(body.apiKey.startsWith("pak_live_"))
        assert(body.keyPrefix.isNotBlank())
    }

    @Test
    fun `aufruf ohne key wird abgelehnt`() {
        mockMvc.perform(get("/api/partner/v1/exercises"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("missing_api_key"))
    }

    @Test
    fun `aufruf mit unbekanntem key wird abgelehnt`() {
        mockMvc.perform(get("/api/partner/v1/exercises").header(HttpHeaders.AUTHORIZATION, "Bearer pak_live_doesnotexist"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("invalid_api_key"))
    }

    @Test
    fun `barcode lookup liefert naehrwertdaten ohne personalisierten fit-score`() {
        val key = registerPartnerAndGetKey()
        val barcode = "4000000000${(10000..99999).random()}"
        seedFood(barcode)

        mockMvc.perform(get("/api/partner/v1/foods/barcode/$barcode").header(HttpHeaders.AUTHORIZATION, "Bearer $key"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Partner-Test-Produkt"))
            .andExpect(jsonPath("$.kcalPer100g").value(250.0))
    }

    @Test
    fun `unbekannter barcode liefert 404`() {
        val key = registerPartnerAndGetKey()
        mockMvc.perform(get("/api/partner/v1/foods/barcode/0000000000000").header(HttpHeaders.AUTHORIZATION, "Bearer $key"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `uebungskatalog ist ueber den partner-key erreichbar`() {
        val key = registerPartnerAndGetKey()

        val listResponse = mockMvc.perform(get("/api/partner/v1/exercises").header(HttpHeaders.AUTHORIZATION, "Bearer $key"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        val exercises = objectMapper.readValue(listResponse, Array<PartnerExerciseSummary>::class.java)
        assert(exercises.isNotEmpty())

        mockMvc.perform(
            get("/api/partner/v1/exercises/${exercises.first().id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $key"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.slug").value(exercises.first().slug))
    }

    @Test
    fun `unbekannte uebungs-id liefert 404`() {
        val key = registerPartnerAndGetKey()
        mockMvc.perform(get("/api/partner/v1/exercises/${UUID.randomUUID()}").header(HttpHeaders.AUTHORIZATION, "Bearer $key"))
            .andExpect(status().isNotFound)
    }
}
