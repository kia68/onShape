package de.optadata.odil.onshape.nutrition

import java.util.UUID

data class SavedMealItem(val foodId: UUID, val grams: Double)
data class SavedMeal(val id: UUID, val userId: UUID, val name: String, val items: List<SavedMealItem>)
