package de.optadata.odil.onshape.auth

import de.optadata.odil.onshape.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

/**
 * Bewusst OHNE @Transactional: die Registrierungs-/Login-RLS-Policy (V9) und die
 * owner_only-Policy (V8) muessen ueber echte, abgeschlossene Transaktionen pro Request
 * funktionieren -- ein Test-Rollback wuerde `app.auth_lookup`/`app.current_user_id`
 * unrealistisch lange im selben Transaktionskontext offen halten.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun uniqueEmail() = "auth-test-${System.nanoTime()}@example.test"

    @Test
    fun `registrierung legt Nutzer an und liefert Token`() {
        val email = uniqueEmail()
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.email").value(email))
    }

    @Test
    fun `doppelte registrierung mit gleicher email wird abgelehnt`() {
        val email = uniqueEmail()
        val body = objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict)
    }

    @Test
    fun `login mit korrekten daten liefert token`() {
        val email = uniqueEmail()
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequest(email, "correct-horse-1"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").exists())
    }

    @Test
    fun `login mit falschem passwort wird abgelehnt`() {
        val email = uniqueEmail()
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequest(email, "wrong-password"))),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `login mit unbekannter email wird abgelehnt`() {
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequest(uniqueEmail(), "irrelevant1"))),
        ).andExpect(status().isUnauthorized)
    }
}
