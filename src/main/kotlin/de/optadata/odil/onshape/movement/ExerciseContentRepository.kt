package de.optadata.odil.onshape.movement

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

/** `exercises`/`exercise_mistakes` tragen keine RLS (oeffentliche Referenzdaten, wie in
 * [de.optadata.odil.onshape.training.ExerciseRepository]). [locale] wird NIE direkt in SQL
 * interpoliert -- nur zur Auswahl eines von zwei fest verdrahteten Spaltennamen (gleiches Muster
 * wie [de.optadata.odil.onshape.nutrition.FoodSearchRepository]). */
@Repository
class ExerciseContentRepository(private val jdbcTemplate: JdbcTemplate) {

    fun findContent(exerciseId: UUID, locale: String): LocalizedExerciseContent? {
        val setupCol = if (locale == "en") "setup_steps_en" else "setup_steps_de"
        val execCol = if (locale == "en") "execution_steps_en" else "execution_steps_de"
        val cuesCol = if (locale == "en") "cues_en" else "cues_de"
        val breathingCol = if (locale == "en") "breathing_en" else "breathing_de"
        val normalCol = if (locale == "en") "what_is_normal_en" else "what_is_normal_de"

        return jdbcTemplate.query(
            """
            SELECT video_front_url, video_side_url, thumbnail_url,
                   $setupCol AS setup_steps, $execCol AS execution_steps, $cuesCol AS cues,
                   $breathingCol AS breathing, tempo, $normalCol AS what_is_normal,
                   regression_of, progression_to
            FROM exercises WHERE id = ?
            """.trimIndent(),
            { rs, _ ->
                LocalizedExerciseContent(
                    videoFrontUrl = rs.getString("video_front_url"),
                    videoSideUrl = rs.getString("video_side_url"),
                    thumbnailUrl = rs.getString("thumbnail_url"),
                    setupSteps = rs.textArray("setup_steps"),
                    executionSteps = rs.textArray("execution_steps"),
                    cues = rs.textArray("cues"),
                    breathing = rs.getString("breathing"),
                    tempo = rs.getString("tempo"),
                    whatIsNormal = rs.getString("what_is_normal"),
                    regressionOf = rs.getObject("regression_of", UUID::class.java),
                    progressionTo = rs.getObject("progression_to", UUID::class.java),
                )
            },
            exerciseId,
        ).firstOrNull()
    }

    fun findMistakes(exerciseId: UUID, locale: String): List<ExerciseMistake> {
        val titleCol = if (locale == "en") "title_en" else "title_de"
        val whyCol = if (locale == "en") "why_bad_en" else "why_bad_de"
        val fixCol = if (locale == "en") "fix_en" else "fix_de"

        return jdbcTemplate.query(
            """
            SELECT id, $titleCol AS title, $whyCol AS why_bad, $fixCol AS fix, image_url, severity
            FROM exercise_mistakes WHERE exercise_id = ? ORDER BY severity DESC, title
            """.trimIndent(),
            { rs, _ ->
                ExerciseMistake(
                    id = rs.getObject("id", UUID::class.java),
                    title = rs.getString("title"),
                    whyBad = rs.getString("why_bad"),
                    fix = rs.getString("fix"),
                    imageUrl = rs.getString("image_url"),
                    severity = rs.getInt("severity"),
                )
            },
            exerciseId,
        )
    }

    private fun java.sql.ResultSet.textArray(column: String): List<String> =
        (getArray(column)?.array as Array<*>?)?.map { it.toString() } ?: emptyList()
}
