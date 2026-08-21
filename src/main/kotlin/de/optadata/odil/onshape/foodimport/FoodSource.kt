package de.optadata.odil.onshape.foodimport

/** Spiegelt den Postgres-Enum-Typ `food_source_t` aus V2__foods.sql. */
enum class FoodSource(val dbValue: String) {
    BLS("bls"),
    USDA("usda"),
    OFF("off"),
    BRAND_VERIFIED("brand_verified"),
    USER("user"),
    AI_ESTIMATE("ai_estimate"),
}
