package de.optadata.odil.onshape.foodimport

/** Gemeinsame Schnittstelle aller Quellen-Clients (BLS/USDA/OFF/...). */
interface FoodSourceClient {
    val source: FoodSource

    /** Liefert alle bzw. seit dem letzten Lauf neuen/geaenderten Eintraege, normalisiert. */
    fun fetchDelta(): List<ImportedFood>
}
