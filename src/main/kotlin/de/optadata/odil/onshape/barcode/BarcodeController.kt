package de.optadata.odil.onshape.barcode

import de.optadata.odil.onshape.security.currentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** FR-40..FR-49: Barcode-Scan, Fit-Score, Alternativ-Empfehlung, manuelles Nacherfassen. */
@RestController
@RequestMapping("/api/barcode")
class BarcodeController(
    private val barcodeScanService: BarcodeScanService,
    private val manualProductService: ManualProductService,
) {

    @PostMapping("/scan")
    fun scan(@Valid @RequestBody request: BarcodeScanRequest, authentication: Authentication): BarcodeScanResponse =
        barcodeScanService.scan(authentication.currentUserId(), request.barcode, request.date).toResponse()

    /** FR-49: Produkt nicht gefunden -> manuell anlegen. */
    @PostMapping("/products")
    fun createProduct(@Valid @RequestBody request: ManualProductRequest): ResponseEntity<ManualProductResponse> {
        val id = manualProductService.create(
            ManualProductInput(
                barcode = request.barcode, nameDe = request.nameDe, nameEn = request.nameEn, brand = request.brand,
                category = request.category, kcal = request.kcal, proteinG = request.proteinG, fatG = request.fatG,
                carbsG = request.carbsG, sugarG = request.sugarG, fiberG = request.fiberG, saltG = request.saltG,
                allergens = request.allergens, isLiquid = request.isLiquid,
            ),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ManualProductResponse(id))
    }
}
