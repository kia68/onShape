package de.optadata.odil.onshape.foodimport

/** Vertrauensstufe je Quelle, KONZEPT.md §10.4 Schritt 8. */
object TrustAssigner {

    fun assign(source: FoodSource, manuallyVerified: Boolean = false): TrustLevel {
        if (manuallyVerified) return TrustLevel.VERIFIED
        return when (source) {
            FoodSource.BLS, FoodSource.USDA, FoodSource.BRAND_VERIFIED -> TrustLevel.VERIFIED
            FoodSource.OFF, FoodSource.USER -> TrustLevel.COMMUNITY
            FoodSource.AI_ESTIMATE -> TrustLevel.ESTIMATED
        }
    }
}
