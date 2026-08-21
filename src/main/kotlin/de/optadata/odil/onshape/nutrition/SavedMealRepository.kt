package de.optadata.odil.onshape.nutrition

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

/** FR-25. Muss innerhalb von `RlsSession.asUser` laufen (owner_only, V10). */
@Repository
class SavedMealRepository(private val jdbcTemplate: JdbcTemplate) {

    fun insert(userId: UUID, name: String, items: List<SavedMealItem>): UUID {
        val mealId = jdbcTemplate.queryForObject(
            "INSERT INTO saved_meals (user_id, name) VALUES (?, ?) RETURNING id",
            UUID::class.java,
            userId, name,
        ) ?: error("Insert into saved_meals returned no id")

        items.forEachIndexed { index, item ->
            jdbcTemplate.update(
                "INSERT INTO saved_meal_items (saved_meal_id, food_id, grams, sort_order) VALUES (?, ?, ?, ?)",
                mealId, item.foodId, item.grams, index,
            )
        }
        return mealId
    }

    fun findByUser(userId: UUID): List<SavedMeal> {
        val meals = jdbcTemplate.query(
            "SELECT id, user_id, name FROM saved_meals WHERE user_id = ? ORDER BY created_at DESC",
            { rs, _ -> SavedMeal(rs.getObject("id", UUID::class.java), rs.getObject("user_id", UUID::class.java), rs.getString("name"), emptyList()) },
            userId,
        )
        return meals.map { it.copy(items = findItems(it.id)) }
    }

    fun findById(userId: UUID, id: UUID): SavedMeal? {
        val meal = jdbcTemplate.query(
            "SELECT id, user_id, name FROM saved_meals WHERE id = ? AND user_id = ?",
            { rs, _ -> SavedMeal(rs.getObject("id", UUID::class.java), rs.getObject("user_id", UUID::class.java), rs.getString("name"), emptyList()) },
            id, userId,
        ).firstOrNull() ?: return null
        return meal.copy(items = findItems(meal.id))
    }

    fun delete(userId: UUID, id: UUID): Boolean =
        jdbcTemplate.update("DELETE FROM saved_meals WHERE id = ? AND user_id = ?", id, userId) > 0

    private fun findItems(mealId: UUID): List<SavedMealItem> =
        jdbcTemplate.query(
            "SELECT food_id, grams FROM saved_meal_items WHERE saved_meal_id = ? ORDER BY sort_order",
            { rs, _ -> SavedMealItem(rs.getObject("food_id", UUID::class.java), rs.getDouble("grams")) },
            mealId,
        )
}
