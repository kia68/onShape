package de.optadata.odil.onshape.partnerapi

import de.optadata.odil.onshape.barcode.FoodDetails
import de.optadata.odil.onshape.barcode.FoodDetailsRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** SCALE-03: reine Produkt-/Naehrwertdaten fuer Partner, siehe dto.kt-KDoc zur bewussten
 * Auslassung des personalisierten Fit-Score. `foods` traegt keine RLS (siehe
 * FoodDetailsRepository-Kommentar), der Endpunkt braucht daher keinen `RlsSession`-Kontext. */
@RestController
@RequestMapping("/api/partner/v1/foods")
class PartnerFoodController(private val foodDetailsRepository: FoodDetailsRepository) {

    @GetMapping("/barcode/{barcode}")
    fun byBarcode(@PathVariable barcode: String): PartnerFoodResponse =
        foodDetailsRepository.findByBarcode(barcode)
            ?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Kein Produkt fuer diesen Barcode gefunden")

    private fun FoodDetails.toResponse() = PartnerFoodResponse(
        barcode = barcode,
        brand = brand,
        name = name,
        category = category,
        novaGroup = novaGroup,
        nutriscore = nutriscore,
        kcalPer100g = kcalPer100g,
        proteinGPer100g = proteinGPer100g,
        fatGPer100g = fatGPer100g,
        carbsGPer100g = carbsGPer100g,
        sugarGPer100g = sugarGPer100g,
        fiberGPer100g = fiberGPer100g,
        allergens = allergens,
    )
}
