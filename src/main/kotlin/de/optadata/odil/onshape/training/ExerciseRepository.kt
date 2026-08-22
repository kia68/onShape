package de.optadata.odil.onshape.training

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

/** `exercises`/`exercise_muscles` tragen keine RLS (oeffentliche Referenzdaten, wie `foods`).
 * Muskelbeteiligungen werden in einem zweiten Query batch-geladen statt per Uebung einzeln. */
@Repository
class ExerciseRepository(private val jdbcTemplate: JdbcTemplate) {

    fun findAll(): List<Exercise> {
        val headers = jdbcTemplate.query(HEADER_SQL, { rs, _ -> rs.toHeader() })
        if (headers.isEmpty()) return emptyList()
        val musclesByExercise = findMuscles(headers.map { it.id })
        return headers.map { it.toExercise(musclesByExercise[it.id] ?: emptyList()) }
    }

    fun findById(id: UUID): Exercise? {
        val header = jdbcTemplate.query("$HEADER_SQL WHERE id = ?", { rs, _ -> rs.toHeader() }, id).firstOrNull() ?: return null
        return header.toExercise(findMuscles(listOf(id))[id] ?: emptyList())
    }

    private fun findMuscles(exerciseIds: List<UUID>): Map<UUID, List<ExerciseMuscleFactor>> {
        val placeholders = exerciseIds.joinToString(",") { "?" }
        return jdbcTemplate.query(
            "SELECT exercise_id, muscle, factor FROM exercise_muscles WHERE exercise_id IN ($placeholders)",
            { rs, _ -> rs.getObject("exercise_id", UUID::class.java) to ExerciseMuscleFactor(rs.getString("muscle"), rs.getDouble("factor")) },
            *exerciseIds.toTypedArray(),
        ).groupBy({ it.first }, { it.second })
    }

    private fun ResultSet.toHeader() = ExerciseHeader(
        id = getObject("id", UUID::class.java),
        slug = getString("slug"),
        name = getString("name_de"),
        pattern = MovementPattern.entries.first { it.dbValue == getString("pattern") },
        mechanic = Mechanic.entries.first { it.dbValue == getString("mechanic") },
        equipment = (getArray("equipment")?.array as Array<*>?)?.map { it.toString() } ?: emptyList(),
        difficulty = ExerciseDifficulty.entries.first { it.dbValue == getString("difficulty") },
        unilateral = getBoolean("unilateral"),
        metValue = getBigDecimal("met_value")?.toDouble(),
        contraindications = (getArray("contraindications")?.array as Array<*>?)?.map { it.toString() } ?: emptyList(),
    )

    private data class ExerciseHeader(
        val id: UUID, val slug: String, val name: String, val pattern: MovementPattern, val mechanic: Mechanic,
        val equipment: List<String>, val difficulty: ExerciseDifficulty, val unilateral: Boolean,
        val metValue: Double?, val contraindications: List<String>,
    ) {
        fun toExercise(muscles: List<ExerciseMuscleFactor>) =
            Exercise(id, slug, name, pattern, mechanic, equipment, difficulty, unilateral, metValue, contraindications, muscles)
    }

    private companion object {
        const val HEADER_SQL = """
            SELECT id, slug, name_de, pattern, mechanic, equipment, difficulty, unilateral, met_value, contraindications
            FROM exercises
        """
    }
}
