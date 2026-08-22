package de.optadata.odil.onshape.barcode

import de.optadata.odil.onshape.foodimport.FoodImportRepository
import de.optadata.odil.onshape.foodimport.FoodSource
import de.optadata.odil.onshape.foodimport.ImportedFood
import de.optadata.odil.onshape.foodimport.TrustAssigner
import org.springframework.stereotype.Service
import java.util.UUID

data class ManualProductInput(
    val barcode: String,
    val nameDe: String,
    val nameEn: String?,
    val brand: String?,
    val category: String?,
    val kcal: Double,
    val proteinG: Double,
    val fatG: Double,
    val carbsG: Double,
    val sugarG: Double?,
    val fiberG: Double?,
    val saltG: Double?,
    val allergens: List<String>,
    val isLiquid: Boolean,
)

/**
 * FR-49: Produkt nicht gefunden -> Nutzer legt es manuell an (Quelle 'user', Vertrauensstufe
 * 'community', siehe [TrustAssigner]). Die Etikett-OCR aus der Spec ("Foto des Etiketts + OCR")
 * ist NICHT umgesetzt -- braucht eine Vision-/OCR-Anbindung, fuer die es (wie bei Google/Apple-
 * OAuth in Epic Onboarding) noch keine konfigurierten Credentials gibt. Der Nutzer traegt die
 * Werte stattdessen manuell ein; das Foto wird nicht gespeichert (keine Blob-Storage-Anbindung
 * vorhanden). Die Rueckmeldung an Open Food Facts ("mit Einwilligung") ist ebenfalls nicht
 * umgesetzt -- das waere ein Schreibzugriff auf eine fremde API mit eigenem Auth-Flow.
 */
@Service
class ManualProductService(private val foodImportRepository: FoodImportRepository) {

    fun create(input: ManualProductInput): UUID {
        val importedFood = ImportedFood(
            source = FoodSource.USER,
            sourceId = null,
            barcode = input.barcode,
            brand = input.brand,
            nameDe = input.nameDe,
            nameEn = input.nameEn ?: input.nameDe,
            category = input.category,
            kcal = input.kcal,
            proteinG = input.proteinG,
            fatG = input.fatG,
            carbsG = input.carbsG,
            sugarG = input.sugarG,
            fiberG = input.fiberG,
            saltG = input.saltG,
            allergens = input.allergens,
            isLiquid = input.isLiquid,
        )
        return foodImportRepository.insert(importedFood, TrustAssigner.assign(FoodSource.USER))
    }
}
