package de.optadata.odil.onshape.barcode

import de.optadata.odil.onshape.foodimport.TrustLevel
import de.optadata.odil.onshape.nutrition.MicroNutrients
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.util.UUID

/** `foods`/`food_servings` tragen keine RLS (oeffentliche Referenzdaten, siehe
 * V8__row_level_security.sql-Kommentar) -- diese Methoden laufen unabhaengig von
 * [de.optadata.odil.onshape.security.RlsSession]. */
@Repository
class FoodDetailsRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) {

    /** FR-40: bester Treffer ueber alle Quellen, hoechste Vertrauensstufe zuerst (gleiche
     * Priorisierung wie [de.optadata.odil.onshape.foodimport.FoodImportRepository.findByBarcode]). */
    fun findByBarcode(barcode: String): FoodDetails? =
        jdbcTemplate.query(
            "$SELECT_SQL WHERE barcode = ? " +
                "ORDER BY CASE trust WHEN 'verified' THEN 2 WHEN 'community' THEN 1 ELSE 0 END DESC LIMIT 1",
            { rs, _ -> rs.toFoodDetails() },
            barcode,
        ).firstOrNull()

    fun findById(id: UUID): FoodDetails? =
        jdbcTemplate.query("$SELECT_SQL WHERE id = ?", { rs, _ -> rs.toFoodDetails() }, id).firstOrNull()

    /** FR-45/§7.7: Kandidaten derselben Kategorie fuer die Alternativ-Empfehlung. */
    fun findByCategory(category: String, excludeId: UUID, limit: Int = 30): List<FoodDetails> =
        jdbcTemplate.query(
            "$SELECT_SQL WHERE category = ? AND id != ? ORDER BY trust DESC LIMIT ?",
            { rs, _ -> rs.toFoodDetails() },
            category, excludeId, limit,
        )

    private fun ResultSet.toFoodDetails(): FoodDetails {
        val id = getObject("id", UUID::class.java)
        val defaultServing = getBigDecimal("default_serving_grams")?.toDouble() ?: 100.0
        return FoodDetails(
            id = id,
            barcode = getString("barcode"),
            brand = getString("brand"),
            name = getString("name_de"),
            category = getString("category"),
            novaGroup = getObject("nova_group", Integer::class.java) as Int?,
            nutriscore = getString("nutriscore")?.firstOrNull(),
            kcalPer100g = getDouble("kcal"),
            proteinGPer100g = getDouble("protein_g"),
            fatGPer100g = getDouble("fat_g"),
            saturatedFatGPer100g = getBigDecimal("saturated_fat_g")?.toDouble(),
            transFatGPer100g = getBigDecimal("trans_fat_g")?.toDouble(),
            carbsGPer100g = getDouble("carbs_g"),
            sugarGPer100g = getBigDecimal("sugar_g")?.toDouble(),
            fiberGPer100g = getBigDecimal("fiber_g")?.toDouble(),
            saltGPer100g = getBigDecimal("salt_g")?.toDouble(),
            micros = MicroNutrients.parse(objectMapper, getString("micros")),
            allergens = (getArray("allergens")?.array as Array<*>?)?.map { it.toString() } ?: emptyList(),
            additives = (getArray("additives")?.array as Array<*>?)?.map { it.toString() } ?: emptyList(),
            trust = TrustLevel.entries.first { it.dbValue == getString("trust") },
            defaultServingGrams = defaultServing,
        )
    }

    private companion object {
        const val SELECT_SQL = """
            SELECT f.id, f.barcode, f.brand, f.name_de, f.category, f.nova_group, f.nutriscore,
                   f.kcal, f.protein_g, f.fat_g, f.saturated_fat_g, f.trans_fat_g, f.carbs_g,
                   f.sugar_g, f.fiber_g, f.salt_g, f.micros, f.allergens, f.additives, f.trust,
                   (SELECT s.grams FROM food_servings s WHERE s.food_id = f.id AND s.is_default LIMIT 1) AS default_serving_grams
            FROM foods f
        """
    }
}
