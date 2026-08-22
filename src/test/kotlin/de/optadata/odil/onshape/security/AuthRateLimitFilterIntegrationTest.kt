package de.optadata.odil.onshape.security

import de.optadata.odil.onshape.AbstractIntegrationTest
import de.optadata.odil.onshape.auth.RegisterRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals

/** NFR-08. Eigene, sehr niedrige Limit-Konfiguration nur fuer diese Testklasse (via
 * @TestPropertySource -- Spring erzeugt dafuer einen eigenen ApplicationContext, der NICHT mit
 * dem Standard-@SpringBootTest-Kontext geteilt wird, den alle anderen Integrationstests
 * verwenden). So kann diese Klasse das Limit real ausloesen, ohne dass das grosszuegige
 * Produktions-/Test-Default (30/60s, siehe application.properties) fuer den Rest der Suite
 * verschaerft werden muss. Bewusst OHNE @Transactional, siehe AuthControllerIntegrationTest. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = ["app.security.auth-rate-limit.max-requests=3", "app.security.auth-rate-limit.window-seconds=60"])
class AuthRateLimitFilterIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @Test
    fun `nach dem konfigurierten limit antwortet die registrierung mit 429`() {
        fun register(email: String) = mockMvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        )

        repeat(3) { i -> register("rate-limit-test-$i-${System.nanoTime()}@example.test").andExpect(status().isCreated) }
        val overflow = register("rate-limit-test-overflow-${System.nanoTime()}@example.test").andReturn()
        assertEquals(429, overflow.response.status)
    }
}
