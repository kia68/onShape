package de.optadata.odil.onshape.nutrition

import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.net.UnknownHostException

class InvalidRecipeUrlException(message: String) : RuntimeException(message)

/**
 * FR-27 ruft erstmals eine vom NUTZER angegebene URL vom Backend aus auf (jeder andere HTTP-
 * Client dieser App -- OFF/USDA/Stripe -- ruft feste, selbst konfigurierte Hosts auf, kein
 * SSRF-Risiko). Validiert VOR jedem Netzwerkzugriff: nur http(s), und die aufgeloeste IP darf
 * nicht loopback/link-local/site-local(privat)/multicast/"any local" sein -- verhindert, dass
 * die URL interne Dienste (z.B. `http://localhost:8080/actuator/...` oder ein Cloud-Metadaten-
 * Endpunkt) erreicht. Reine Validierung, kein eigentlicher Fetch (der lebt in
 * [RecipeUrlFetcher]) -- macht diesen Teil ohne echtes Netzwerk testbar.
 */
object SafeUrlValidator {

    fun validate(rawUrl: String): URL {
        val url = try {
            URI(rawUrl.trim()).toURL()
        } catch (e: Exception) {
            throw InvalidRecipeUrlException("Ungueltige URL")
        }
        if (url.protocol != "http" && url.protocol != "https") {
            throw InvalidRecipeUrlException("Nur http/https-URLs sind erlaubt")
        }
        if (url.host.isNullOrBlank()) {
            throw InvalidRecipeUrlException("Ungueltige URL")
        }

        val address = try {
            InetAddress.getByName(url.host)
        } catch (e: UnknownHostException) {
            throw InvalidRecipeUrlException("Host nicht aufloesbar")
        }
        if (address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress ||
            address.isMulticastAddress || address.isAnyLocalAddress
        ) {
            throw InvalidRecipeUrlException("Diese Adresse ist nicht erlaubt")
        }
        return url
    }
}
