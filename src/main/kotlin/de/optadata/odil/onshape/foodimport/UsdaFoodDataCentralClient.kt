package de.optadata.odil.onshape.foodimport

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * USDA FoodData Central (KONZEPT.md §10.4/§11.1): Public Domain, kostenlos, aber API-Key
 * noetig (kostenlose Registrierung; `DEMO_KEY` funktioniert nur stark ratelimitiert fuer
 * Tests). Key ueber env `USDA_API_KEY` -> Property `usda.api-key` setzen.
 *
 * Der volle Bestand (~250.000 Eintraege) wird ueber die monatlichen Bulk-Downloads
 * importiert, nicht ueber Einzel-Suchanfragen; [searchByQuery] deckt den interaktiven
 * Fall (z. B. manuelle Nachpflege einzelner Eintraege) ab.
 */
@Component
class UsdaFoodDataCentralClient(
    @Value("\${usda.api-base-url:https://api.nal.usda.gov/fdc/v1}") baseUrl: String,
    @Value("\${usda.api-key:DEMO_KEY}") private val apiKey: String,
) : FoodSourceClient {

    override val source = FoodSource.USDA

    private val restClient = RestClient.builder().baseUrl(baseUrl).build()

    fun searchByQuery(query: String, pageSize: Int = 25): List<ImportedFood> {
        val response = restClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/foods/search")
                    .queryParam("query", query)
                    .queryParam("pageSize", pageSize)
                    .queryParam("api_key", apiKey)
                    .build()
            }
            .retrieve()
            .body(UsdaSearchResponse::class.java)

        return response?.foods.orEmpty().mapNotNull { it.toImportedFood() }
    }

    /** Delta-Import laeuft ueber den monatlichen USDA-Bulk-Export, nicht ueber diesen Client. */
    override fun fetchDelta(): List<ImportedFood> = emptyList()
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class UsdaSearchResponse(val foods: List<UsdaFood>? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class UsdaFood(
    val fdcId: Long? = null,
    val description: String? = null,
    val brandOwner: String? = null,
    val gtinUpc: String? = null,
    val foodNutrients: List<UsdaNutrient>? = null,
) {
    fun toImportedFood(): ImportedFood? {
        val id = fdcId ?: return null
        val name = description ?: return null
        val kcal = nutrientValue("Energy") ?: return null
        return ImportedFood(
            source = FoodSource.USDA,
            sourceId = id.toString(),
            barcode = gtinUpc,
            brand = brandOwner,
            nameDe = name,
            nameEn = name,
            kcal = kcal,
            proteinG = nutrientValue("Protein") ?: 0.0,
            fatG = nutrientValue("Total lipid (fat)") ?: 0.0,
            carbsG = nutrientValue("Carbohydrate, by difference") ?: 0.0,
            sugarG = nutrientValue("Sugars, total including NLEA") ?: nutrientValue("Total Sugars"),
            fiberG = nutrientValue("Fiber, total dietary"),
            saltG = nutrientValue("Sodium, Na")?.let { sodiumMgTo100g -> sodiumMgTo100g * 2.5 / 1000 },
        )
    }

    private fun nutrientValue(nutrientName: String): Double? =
        foodNutrients?.firstOrNull { it.nutrientName == nutrientName }?.value
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class UsdaNutrient(
    val nutrientName: String? = null,
    val value: Double? = null,
    val unitName: String? = null,
)
