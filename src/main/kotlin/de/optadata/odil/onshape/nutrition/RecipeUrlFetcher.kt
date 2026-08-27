package de.optadata.odil.onshape.nutrition

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URL
import java.net.http.HttpClient
import java.time.Duration

/** Trennt das eigentliche Netzwerk vom Rest von FR-27 -- Tests koennen eine Fake-Implementierung
 * einsetzen, gleiches Muster wie [de.optadata.odil.onshape.foodimport.FoodSourceClient]. */
interface RecipeUrlFetcher {
    /** @return das rohe HTML, oder null bei jedem Fehler (Timeout, 4xx/5xx, Redirect -- siehe
     * [HttpRecipeUrlFetcher]-KDoc). */
    fun fetchHtml(url: URL): String?
}

/**
 * [SafeUrlValidator] validiert VOR dem Aufruf hier -- diese Klasse macht nur noch den Request.
 * Redirects werden bewusst NICHT automatisch verfolgt (`Redirect.NEVER`): ein Redirect koennte
 * sonst nach der SSRF-Pruefung auf eine interne Adresse zeigen, ohne erneut geprueft zu werden.
 * Eine Recipe-Seite, die redirectet, liefert dadurch schlicht kein Ergebnis -- kein
 * automatisches Nachverfolgen fuer diesen einen, sicherheitskritischen Fall.
 */
@Component
class HttpRecipeUrlFetcher(
    @Value("\${recipeimport.max-response-bytes:5000000}") private val maxResponseBytes: Long,
) : RecipeUrlFetcher {

    private val restClient = RestClient.builder()
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build(),
            ).apply { setReadTimeout(Duration.ofSeconds(10)) },
        )
        .build()

    override fun fetchHtml(url: URL): String? {
        val body = try {
            restClient.get().uri(url.toURI())
                .header("User-Agent", "OnShapeBot/1.0 (+recipe-import)")
                .retrieve()
                .body(String::class.java)
        } catch (e: Exception) {
            null
        } ?: return null
        return if (body.length.toLong() > maxResponseBytes) body.substring(0, maxResponseBytes.toInt()) else body
    }
}
