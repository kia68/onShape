package de.optadata.odil.onshape.foodimport

/**
 * Spiegelt den Postgres-Enum-Typ `trust_t` aus V2__foods.sql.
 * Ordinal-Reihenfolge bildet die Rangfolge ab ("bei Konflikt gewinnt die
 * hoehere Vertrauensstufe", KONZEPT.md §10.4 Schritt 4) — VERIFIED > COMMUNITY > ESTIMATED.
 */
enum class TrustLevel(val dbValue: String) {
    ESTIMATED("estimated"),
    COMMUNITY("community"),
    VERIFIED("verified"),
}
