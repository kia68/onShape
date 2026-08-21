package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.foodimport.TrustLevel
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

/**
 * FR-22 (Quick-Add): Volltextsuche ueber die GIN-Indizes aus V2__foods.sql
 * (`foods_search_de_idx`/`foods_search_en_idx` fuer Wortsuche, `foods_trgm_idx` fuer
 * Tippfehlertoleranz auf name_de). `foods`/`food_servings` tragen keine RLS (oeffentliche
 * Referenzdaten), `food_entries` (fuer [findLastUsedGrams]) dagegen schon (owner_only, V8) --
 * [search] muss deshalb IMMER innerhalb von `RlsSession.asUser(userId) { ... }` aufgerufen
 * werden, sonst liefert die "zuletzt genutzte Menge" (FR-22) still leer statt einen Fehler.
 */
@Repository
class FoodSearchRepository(private val jdbcTemplate: JdbcTemplate) {

    fun search(query: String, locale: String, userId: UUID, limit: Int): List<FoodSearchResult> {
        val nameColumn = if (locale == "en") "name_en" else "name_de"
        val tsConfig = if (locale == "en") "english" else "german"
        val sql = """
            SELECT id, $nameColumn AS name, brand, kcal, protein_g, fat_g, carbs_g, trust,
                   GREATEST(
                       ts_rank(to_tsvector('$tsConfig', coalesce(brand,'') || ' ' || $nameColumn), plainto_tsquery('$tsConfig', ?)),
                       similarity($nameColumn, ?)
                   ) AS rank
            FROM foods
            WHERE to_tsvector('$tsConfig', coalesce(brand,'') || ' ' || $nameColumn) @@ plainto_tsquery('$tsConfig', ?)
               OR similarity($nameColumn, ?) > 0.3
            ORDER BY rank DESC
            LIMIT ?
        """.trimIndent()

        val rows = jdbcTemplate.query(sql, { rs, _ -> rs.toRow() }, query, query, query, query, limit)
        if (rows.isEmpty()) return emptyList()

        val foodIds = rows.map { it.id }
        val servingsByFood = findServings(foodIds, locale)
        val lastUsedByFood = findLastUsedGrams(userId, foodIds)

        return rows.map { row ->
            FoodSearchResult(
                id = row.id,
                name = row.name,
                brand = row.brand,
                kcalPer100g = row.kcal,
                proteinGPer100g = row.proteinG,
                fatGPer100g = row.fatG,
                carbsGPer100g = row.carbsG,
                trust = row.trust,
                servings = servingsByFood[row.id] ?: emptyList(),
                lastUsedGrams = lastUsedByFood[row.id],
            )
        }
    }

    fun findById(foodId: UUID): FoodNutrition? =
        jdbcTemplate.query(
            "SELECT id, kcal, protein_g, fat_g, carbs_g, micros FROM foods WHERE id = ?",
            { rs, _ ->
                FoodNutrition(
                    id = rs.getObject("id", UUID::class.java),
                    kcalPer100g = rs.getDouble("kcal"),
                    proteinGPer100g = rs.getDouble("protein_g"),
                    fatGPer100g = rs.getDouble("fat_g"),
                    carbsGPer100g = rs.getDouble("carbs_g"),
                    microsPer100g = rs.getString("micros"),
                )
            },
            foodId,
        ).firstOrNull()

    private fun findServings(foodIds: List<UUID>, locale: String): Map<UUID, List<ServingOption>> {
        val labelColumn = if (locale == "en") "label_en" else "label_de"
        val placeholders = foodIds.joinToString(",") { "?" }
        return jdbcTemplate.query(
            "SELECT food_id, id, $labelColumn AS label, grams, is_default FROM food_servings WHERE food_id IN ($placeholders)",
            { rs, _ ->
                rs.getObject("food_id", UUID::class.java) to ServingOption(
                    id = rs.getObject("id", UUID::class.java),
                    label = rs.getString("label"),
                    grams = rs.getDouble("grams"),
                    isDefault = rs.getBoolean("is_default"),
                )
            },
            *foodIds.toTypedArray(),
        ).groupBy({ it.first }, { it.second })
    }

    private fun findLastUsedGrams(userId: UUID, foodIds: List<UUID>): Map<UUID, Double> {
        val placeholders = foodIds.joinToString(",") { "?" }
        return jdbcTemplate.query(
            """
            SELECT DISTINCT ON (food_id) food_id, grams
            FROM food_entries
            WHERE user_id = ? AND food_id IN ($placeholders)
            ORDER BY food_id, created_at DESC
            """.trimIndent(),
            { rs, _ -> rs.getObject("food_id", UUID::class.java) to rs.getDouble("grams") },
            userId,
            *foodIds.toTypedArray(),
        ).toMap()
    }

    private fun ResultSet.toRow() = SearchRow(
        id = getObject("id", UUID::class.java),
        name = getString("name"),
        brand = getString("brand"),
        kcal = getDouble("kcal"),
        proteinG = getDouble("protein_g"),
        fatG = getDouble("fat_g"),
        carbsG = getDouble("carbs_g"),
        trust = TrustLevel.entries.first { it.dbValue == getString("trust") },
    )

    private data class SearchRow(
        val id: UUID, val name: String, val brand: String?, val kcal: Double,
        val proteinG: Double, val fatG: Double, val carbsG: Double, val trust: TrustLevel,
    )
}

data class FoodNutrition(
    val id: UUID,
    val kcalPer100g: Double,
    val proteinGPer100g: Double,
    val fatGPer100g: Double,
    val carbsGPer100g: Double,
    /** Rohe jsonb-Zeichenkette aus `foods.micros`, siehe [MicroNutrients.parse]. */
    val microsPer100g: String,
)
