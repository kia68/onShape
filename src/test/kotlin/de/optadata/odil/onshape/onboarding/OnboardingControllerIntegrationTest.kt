package de.optadata.odil.onshape.onboarding

import de.optadata.odil.onshape.AbstractIntegrationTest
import de.optadata.odil.onshape.auth.AuthResponse
import de.optadata.odil.onshape.auth.RegisterRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

/** Bewusst OHNE @Transactional, siehe [de.optadata.odil.onshape.auth.AuthControllerIntegrationTest]. */
@SpringBootTest
@AutoConfigureMockMvc
class OnboardingControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun registerAndGetToken(): String {
        val email = "onboarding-test-${System.nanoTime()}@example.test"
        val response = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        ).andReturn().response.contentAsString
        return objectMapper.readValue(response, AuthResponse::class.java).token
    }

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
}
