package de.optadata.odil.onshape.onboarding

import org.postgresql.util.PGobject
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

data class StoredNutritionTarget(val validFrom: LocalDate, val result: NutritionTargetResult)

/** Schreibt/liest `nutrition_targets` (V1). Jede Berechnung ist ein neuer Datensatz, nie ein
 * Update (siehe Migrations-Kommentar: "Zielhistorie: nie ueberschreiben"). */
@Repository
class NutritionTargetRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) {
    fun insert(userId: UUID, validFrom: LocalDate, result: NutritionTargetResult): UUID =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO nutrition_targets (
                user_id, valid_from, kcal, protein_g, fat_g, carbs_g, fiber_g, water_ml,
                bmr_kcal, tdee_kcal, tdee_source, calculation
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """.trimIndent(),
            UUID::class.java,
            userId, validFrom, result.kcal, result.proteinG, result.fatG, result.carbsG, result.fiberG,
            result.waterMl, result.bmrKcal, result.tdeeKcal, result.tdeeSource, result.calculation.toJsonb(),
        ) ?: error("Insert into nutrition_targets returned no id")

    fun findLatest(userId: UUID): StoredNutritionTarget? =
        jdbcTemplate.query(
            """
            SELECT valid_from, kcal, protein_g, fat_g, carbs_g, fiber_g, water_ml, bmr_kcal, tdee_kcal, tdee_source, calculation
            FROM nutrition_targets WHERE user_id = ? ORDER BY valid_from DESC, created_at DESC LIMIT 1
            """.trimIndent(),
            { rs, _ -> rs.toStoredTarget() },
            userId,
        ).firstOrNull()

    @Suppress("UNCHECKED_CAST")
    private fun ResultSet.toStoredTarget() = StoredNutritionTarget(
        validFrom = getObject("valid_from", LocalDate::class.java),
        result = NutritionTargetResult(
            kcal = getInt("kcal"),
            proteinG = getInt("protein_g"),
            fatG = getInt("fat_g"),
            carbsG = getInt("carbs_g"),
            fiberG = getInt("fiber_g"),
            waterMl = getInt("water_ml"),
            bmrKcal = getInt("bmr_kcal"),
            tdeeKcal = getInt("tdee_kcal"),
            tdeeSource = getString("tdee_source"),
            calculation = objectMapper.readValue(getString("calculation"), Map::class.java) as Map<String, Any?>,
        ),
    )

    private fun Map<String, Any?>.toJsonb(): PGobject = PGobject().apply {
        type = "jsonb"
        value = objectMapper.writeValueAsString(this@toJsonb)
    }
}
