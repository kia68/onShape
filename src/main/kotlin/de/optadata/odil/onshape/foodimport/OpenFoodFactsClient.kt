package de.optadata.odil.onshape.foodimport

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Open Food Facts (KONZEPT.md §10.4/§11.1): oeffentlich, kostenlos, kein API-Key noetig.
 * ODbL-lizenziert — Daten bleiben ausschliesslich in der `source = 'off'`-Partition, siehe
 * [Deduplicator]. Fuer den vollen Bestandsimport (2,5 Mio. Produkte) ist der taegliche
 * Bulk-Export (JSONL/CSV) die richtige Quelle, nicht Einzel-Requests; dieser Client deckt
 * den Barcode-Lookup ab, wie ihn der Barcode-Scanner (Epic "barcode") zur Laufzeit braucht.
 */
@Component
class OpenFoodFactsClient(
    @Value("\${off.api-base-url:https://world.openfoodfacts.org}") baseUrl: String,
) : FoodSourceClient {

    override val source = FoodSource.OFF

    private val restClient = RestClient.builder().baseUrl(baseUrl).build()

    fun fetchByBarcode(barcode: String): ImportedFood? {
        val response = restClient.get()
            .uri("/api/v2/product/{barcode}.json", barcode)
            .retrieve()
            .body(OffProductResponse::class.java)

        val product = response?.takeIf { it.status == 1 }?.product ?: return null
        return product.toImportedFood(barcode)
    }

    /** Bulk-/Delta-Import laeuft ueber den taeglichen OFF-Export, nicht ueber diesen Client. */
    override fun fetchDelta(): List<ImportedFood> = emptyList()
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OffProductResponse(
    val status: Int? = null,
    val product: OffProduct? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OffProduct(
    val code: String? = null,
    val brands: String? = null,
    @JsonAlias("product_name_de") val productNameDe: String? = null,
    @JsonAlias("product_name_en") val productNameEn: String? = null,
    @JsonAlias("product_name") val productName: String? = null,
    val categories: String? = null,
    @JsonAlias("nova_group") val novaGroup: Int? = null,
    @JsonAlias("nutriscore_grade") val nutriscoreGrade: String? = null,
    @JsonAlias("allergens_tags") val allergensTags: List<String>? = null,
    @JsonAlias("additives_tags") val additivesTags: List<String>? = null,
    val nutriments: OffNutriments? = null,
) {
    fun toImportedFood(barcode: String): ImportedFood? {
        val n = nutriments ?: return null
        val kcal = n.energyKcal100g ?: return null
        val nameDe = productNameDe ?: productName ?: return null
        val nameEn = productNameEn ?: productName ?: nameDe
        return ImportedFood(
            source = FoodSource.OFF,
            sourceId = code ?: barcode,
            barcode = code ?: barcode,
            brand = brands,
            nameDe = nameDe,
            nameEn = nameEn,
            category = categories?.substringBefore(','),
            novaGroup = novaGroup,
            nutriscore = nutriscoreGrade?.uppercase()?.firstOrNull(),
            kcal = kcal,
            proteinG = n.proteins100g ?: 0.0,
            fatG = n.fat100g ?: 0.0,
            saturatedFatG = n.saturatedFat100g,
            carbsG = n.carbohydrates100g ?: 0.0,
            sugarG = n.sugars100g,
            fiberG = n.fiber100g,
            saltG = n.salt100g,
            allergens = allergensTags.orEmpty().map { it.substringAfter(':') },
            additives = additivesTags.orEmpty().map { it.substringAfter(':') },
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OffNutriments(
    @JsonAlias("energy-kcal_100g") val energyKcal100g: Double? = null,
    @JsonAlias("proteins_100g") val proteins100g: Double? = null,
    @JsonAlias("fat_100g") val fat100g: Double? = null,
    @JsonAlias("saturated-fat_100g") val saturatedFat100g: Double? = null,
    @JsonAlias("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @JsonAlias("sugars_100g") val sugars100g: Double? = null,
    @JsonAlias("fiber_100g") val fiber100g: Double? = null,
    @JsonAlias("salt_100g") val salt100g: Double? = null,
)
