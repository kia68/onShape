package de.optadata.odil.onshape.nutrition

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Spiegelt `food_entries` aus V3__nutrition_log.sql. Naehrwerte sind bewusst denormalisiert
 * (siehe Migrations-Kommentar): historische Eintraege duerfen sich nicht rueckwirkend
 * aendern, wenn Quelldaten korrigiert werden. */
data class FoodEntry(
    val id: UUID,
    val userId: UUID,
    val foodId: UUID?,
    val recipeId: UUID?,
    val loggedDate: LocalDate,
    val slot: MealSlot,
    val grams: Double,
    val servingId: UUID?,
    val method: EntryMethod,
    val kcal: Double,
    val proteinG: Double,
    val fatG: Double,
    val carbsG: Double,
    val micros: Map<String, Double>,
    val clientId: String?,
    val createdAt: Instant,
)

/** Fuer die Tagesansicht (FR-20): `foods.name_de`/`recipes.name_de` (bzw. _en) dazugejoint,
 * rein fuers Anzeigen -- die in [FoodEntry] gespeicherten Naehrwerte bleiben die alleinige
 * Quelle der Wahrheit und aendern sich nie rueckwirkend, auch wenn der Name spaeter korrigiert
 * wird. */
data class FoodEntryWithName(val entry: FoodEntry, val name: String?)

/** FR-131: taegliche Summe fuer den Kalorien-/Makro-Verlauf, siehe [FoodEntryRepository.findDailyTotals]. */
data class DailyNutritionTotal(val date: LocalDate, val kcal: Double, val proteinG: Double, val fatG: Double, val carbsG: Double)

data class NewFoodEntry(
    val foodId: UUID?,
    val recipeId: UUID?,
    val loggedDate: LocalDate,
    val slot: MealSlot,
    val grams: Double,
    val servingId: UUID?,
    val method: EntryMethod,
    val kcal: Double,
    val proteinG: Double,
    val fatG: Double,
    val carbsG: Double,
    val micros: Map<String, Double>,
    val clientId: String?,
)
