package de.optadata.odil.onshape.nutrition

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

/** FR-26. `recipes`/`recipe_items` haben eine `owner_or_public`/`via_recipe`-RLS-Policy (V8):
 * eigene Rezepte UND kuratierte (user_id NULL) bzw. oeffentliche sind sichtbar. Muss trotzdem
 * innerhalb von `RlsSession.asUser` laufen, damit "eigene" ueberhaupt aufgeloest werden kann. */
@Repository
class RecipeRepository(private val jdbcTemplate: JdbcTemplate) {

    fun insert(userId: UUID, name: String, servings: Double, instructions: String?, items: List<RecipeItemInput>): UUID {
        val recipeId = jdbcTemplate.queryForObject(
            "INSERT INTO recipes (user_id, name_de, servings, instructions) VALUES (?, ?, ?, ?) RETURNING id",
            UUID::class.java,
            userId, name, servings, instructions,
        ) ?: error("Insert into recipes returned no id")

        items.forEachIndexed { index, item ->
            jdbcTemplate.update(
                "INSERT INTO recipe_items (recipe_id, food_id, grams, sort_order) VALUES (?, ?, ?, ?)",
                recipeId, item.foodId, item.grams, index,
            )
        }
        return recipeId
    }

    fun findById(id: UUID): Recipe? {
        val header = jdbcTemplate.query(HEADER_SQL + " WHERE id = ?", headerMapper, id).firstOrNull() ?: return null
        val items = findItems(id)
        return header.copy(items = items)
    }

    fun findOwnAndPublic(userId: UUID): List<Recipe> {
        val headers = jdbcTemplate.query(
            "$HEADER_SQL WHERE user_id = ? OR user_id IS NULL OR is_public ORDER BY created_at DESC",
            headerMapper,
            userId,
        )
        return headers.map { it.copy(items = findItems(it.id)) }
    }

    private fun findItems(recipeId: UUID): List<RecipeItem> =
        jdbcTemplate.query(
            "SELECT food_id, grams FROM recipe_items WHERE recipe_id = ? ORDER BY sort_order",
            { rs, _ -> RecipeItem(rs.getObject("food_id", UUID::class.java), rs.getDouble("grams")) },
            recipeId,
        )

    private val headerMapper = RowMapper { rs, _ ->
        Recipe(
            id = rs.getObject("id", UUID::class.java),
            userId = rs.getObject("user_id", UUID::class.java),
            name = rs.getString("name_de"),
            servings = rs.getDouble("servings"),
            instructions = rs.getString("instructions"),
            sourceUrl = rs.getString("source_url"),
            isPublic = rs.getBoolean("is_public"),
            items = emptyList(),
        )
    }

    private companion object {
        const val HEADER_SQL =
            "SELECT id, user_id, name_de, servings, instructions, source_url, is_public FROM recipes"
    }
}
