package de.optadata.odil.onshape.onboarding

import de.optadata.odil.onshape.AbstractIntegrationTest
import de.optadata.odil.onshape.auth.AuthResponse
import de.optadata.odil.onshape.auth.RegisterRequest
import de.optadata.odil.onshape.billing.BillingPeriod
import de.optadata.odil.onshape.billing.SubscriptionRepository
import de.optadata.odil.onshape.billing.SubscriptionStatus
import de.optadata.odil.onshape.billing.Tier
import de.optadata.odil.onshape.security.RlsSession
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Bewusst OHNE @Transactional, siehe [de.optadata.odil.onshape.auth.AuthControllerIntegrationTest]. */
@SpringBootTest
@AutoConfigureMockMvc
class OnboardingControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    @Autowired lateinit var subscriptionRepository: SubscriptionRepository
    @Autowired lateinit var rlsSession: RlsSession

    private fun registerAndGetToken(): String = registerReturningAuth().token

    private fun registerReturningAuth(): AuthResponse {
        val email = "onboarding-test-${System.nanoTime()}@example.test"
        val response = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        ).andReturn().response.contentAsString
        return objectMapper.readValue(response, AuthResponse::class.java)
    }

    private fun grantPlus(userId: UUID) {
        rlsSession.asUser(userId) {
            subscriptionRepository.upsert(
                userId, Tier.PLUS, BillingPeriod.MONTHLY, SubscriptionStatus.ACTIVE,
                isLifetime = false, stripeCustomerId = null, stripeSubscriptionId = null,
                currentPeriodEnd = null, at = Instant.now(),
            )
        }
    }

    private fun seedFood(nameDe: String, kcal: Double): UUID =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO foods (source, trust, name_de, name_en, kcal, protein_g, fat_g, carbs_g)
            VALUES ('user'::food_source_t, 'estimated'::trust_t, ?, ?, ?, 10, 5, 8) RETURNING id
            """.trimIndent(),
            UUID::class.java, nameDe, nameDe, kcal,
        )!!

    private fun validRequest(birthDate: LocalDate = LocalDate.of(1996, 1, 1)) = OnboardingRequest(
        sex = "male",
        birthDate = birthDate,
        heightCm = 180.0,
        weightKg = 80.0,
        experience = "beginner",
        activityPal = 1.40,
        goal = "lose",
        goalRatePctWeek = 0.5,
        dietaryPrefs = listOf("omnivore"),
        allergens = emptyList(),
        injuries = emptyList(),
        equipment = listOf("bodyweight"),
        trainingDaysWeek = 3,
        sessionMinutes = 45,
        healthScreening = HealthScreeningAnswers(false, false, false, false),
    )

    @Test
    fun `profil einreichen liefert tagesziel mit herleitung`() {
        val token = registerAndGetToken()
        mockMvc.perform(
            put("/api/onboarding/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.kcal").isNumber)
            .andExpect(jsonPath("$.proteinG").isNumber)
            .andExpect(jsonPath("$.calculation.bmr").exists())
            .andExpect(jsonPath("$.healthAdvisory.needsMedicalAdvice").value(false))
    }

    @Test
    fun `ergebnis abrufen liefert das zuletzt berechnete ziel`() {
        val token = registerAndGetToken()
        mockMvc.perform(
            put("/api/onboarding/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())),
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/onboarding/result").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.kcal").isNumber)
    }

    @Test
    fun `ohne token wird der zugriff verweigert`() {
        mockMvc.perform(
            put("/api/onboarding/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `ein nutzer sieht nie das ergebnis eines anderen nutzers (row level security)`() {
        val tokenA = registerAndGetToken()
        val tokenB = registerAndGetToken()

        mockMvc.perform(
            put("/api/onboarding/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())),
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/onboarding/result").header(HttpHeaders.AUTHORIZATION, "Bearer $tokenB"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `unter 16-jaehrige werden blockiert`() {
        val token = registerAndGetToken()
        mockMvc.perform(
            put("/api/onboarding/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest(birthDate = LocalDate.now().minusYears(15)))),
        ).andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `zielrate ausserhalb medizinischer grenzen wird blockiert`() {
        val token = registerAndGetToken()
        mockMvc.perform(
            put("/api/onboarding/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest().copy(goal = "lose", goalRatePctWeek = 1.5))),
        ).andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `adaptives tdee ist im free-tier komplett gesperrt`() {
        val token = registerAndGetToken()
        mockMvc.perform(
            put("/api/onboarding/profile").header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(validRequest())),
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/onboarding/adaptive-tdee").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.code").value("adaptive_tdee_requires_upgrade"))
    }

    @Test
    fun `adaptives tdee ist ohne genug messpunkte nicht anwendbar, zeigt aber die formel als fallback`() {
        val auth = registerReturningAuth()
        grantPlus(auth.userId)
        mockMvc.perform(
            put("/api/onboarding/profile").header(HttpHeaders.AUTHORIZATION, "Bearer ${auth.token}")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(validRequest())),
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/onboarding/adaptive-tdee").header(HttpHeaders.AUTHORIZATION, "Bearer ${auth.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.eligible").value(false))
            .andExpect(jsonPath("$.reason").value("INSUFFICIENT_WEIGH_INS"))
            .andExpect(jsonPath("$.adaptiveTdeeKcal").doesNotExist())
            .andExpect(jsonPath("$.formulaTdeeKcal").isNumber)
    }

    /** Direktes SQL fuer die Gewichtshistorie (wie schon in ProgressControllerIntegrationTest fuer
     * workout_sessions) -- die Messungen-API selbst loggt immer mit dem realen Tagesdatum, hier
     * werden bewusst mehrere Tage im rollierenden 14-Tage-Fenster gebraucht. */
    @Test
    fun `adaptives tdee wird bei genug daten berechnet`() {
        val auth = registerReturningAuth()
        grantPlus(auth.userId)
        mockMvc.perform(
            put("/api/onboarding/profile").header(HttpHeaders.AUTHORIZATION, "Bearer ${auth.token}")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(validRequest())),
        ).andExpect(status().isOk)

        val today = LocalDate.now()
        val windowStart = today.minusDays(13)
        // 9 zusaetzliche Messpunkte -- das Onboarding selbst hat bereits einen bei "heute" gesetzt,
        // zusammen genau 10 im Fenster.
        for (i in 0 until 9) {
            jdbcTemplate.update(
                "INSERT INTO body_measurements (user_id, measured_on, weight_kg, source) VALUES (?, ?, ?, 'user')",
                auth.userId, windowStart.plusDays(i.toLong()), 80.0 - i * 0.1,
            )
        }

        val foodId = seedFood("Adaptives-TDEE-Testfood-${System.nanoTime()}", 200.0)
        // 12 von 14 Tagen geloggt (>= 80% Adhaerenz).
        for (i in 0 until 12) {
            mockMvc.perform(
                post("/api/nutrition/entries").header(HttpHeaders.AUTHORIZATION, "Bearer ${auth.token}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "foodId" to foodId, "loggedDate" to windowStart.plusDays(i.toLong()).toString(),
                                "slot" to "breakfast", "grams" to 1000.0, "method" to "search",
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)
        }

        mockMvc.perform(get("/api/onboarding/adaptive-tdee").header(HttpHeaders.AUTHORIZATION, "Bearer ${auth.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.eligible").value(true))
            .andExpect(jsonPath("$.adaptiveTdeeKcal").isNumber)
            .andExpect(jsonPath("$.windowDays").value(14))
            .andExpect(jsonPath("$.weighInsInWindow").value(10))
            .andExpect(jsonPath("$.nutritionAdherence").value(org.hamcrest.Matchers.closeTo(12.0 / 14.0, 0.0001)))
    }

    @Test
    fun `ein zweiter aufruf am selben tag liefert denselben persistierten wert ohne erneutes blenden`() {
        val auth = registerReturningAuth()
        grantPlus(auth.userId)
        mockMvc.perform(
            put("/api/onboarding/profile").header(HttpHeaders.AUTHORIZATION, "Bearer ${auth.token}")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(validRequest())),
        ).andExpect(status().isOk)

        val today = LocalDate.now()
        val windowStart = today.minusDays(13)
        for (i in 0 until 9) {
            jdbcTemplate.update(
                "INSERT INTO body_measurements (user_id, measured_on, weight_kg, source) VALUES (?, ?, ?, 'user')",
                auth.userId, windowStart.plusDays(i.toLong()), 80.0,
            )
        }
        val foodId = seedFood("Adaptives-TDEE-Testfood-2-${System.nanoTime()}", 200.0)
        for (i in 0 until 12) {
            mockMvc.perform(
                post("/api/nutrition/entries").header(HttpHeaders.AUTHORIZATION, "Bearer ${auth.token}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "foodId" to foodId, "loggedDate" to windowStart.plusDays(i.toLong()).toString(),
                                "slot" to "breakfast", "grams" to 1000.0, "method" to "search",
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)
        }

        val first = objectMapper.readTree(
            mockMvc.perform(get("/api/onboarding/adaptive-tdee").header(HttpHeaders.AUTHORIZATION, "Bearer ${auth.token}"))
                .andExpect(status().isOk).andReturn().response.contentAsString,
        ).get("adaptiveTdeeKcal").asInt()
        val second = objectMapper.readTree(
            mockMvc.perform(get("/api/onboarding/adaptive-tdee").header(HttpHeaders.AUTHORIZATION, "Bearer ${auth.token}"))
                .andExpect(status().isOk).andReturn().response.contentAsString,
        ).get("adaptiveTdeeKcal").asInt()

        org.junit.jupiter.api.Assertions.assertEquals(first, second)
    }
}
