package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.onboarding.EnumWithDbValue
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

/** Spiegelt `exercise_swap_reason_t` aus V12__program_generation.sql. */
enum class SwapReason(override val dbValue: String) : EnumWithDbValue {
    TOO_HARD("too_hard"), EQUIPMENT_OCCUPIED("equipment_occupied"), PAIN("pain"), DISLIKE("dislike"), OTHER("other"),
}

/** FR-74: "fliesst ins Nutzermodell ein" -- die Ablehnungshistorie speist [ExerciseScorer]s
 * w8-Term bei kuenftigen Plangenerierungen. Muss innerhalb von `RlsSession.asUser` laufen
 * (owner_only, V12). */
@Repository
class ExerciseFeedbackRepository(private val jdbcTemplate: JdbcTemplate) {

    fun record(userId: UUID, exerciseId: UUID, reason: SwapReason, replacementId: UUID?) {
        jdbcTemplate.update(
            "INSERT INTO exercise_feedback (user_id, exercise_id, reason, replacement_id) VALUES (?, ?, ?::exercise_swap_reason_t, ?)",
            userId, exerciseId, reason.dbValue, replacementId,
        )
    }

    /** Anzahl vergangener Ablehnungen je Uebung fuer diesen Nutzer (ExerciseScorer w8). */
    fun rejectionCounts(userId: UUID): Map<UUID, Int> =
        jdbcTemplate.query(
            "SELECT exercise_id, COUNT(*) AS n FROM exercise_feedback WHERE user_id = ? GROUP BY exercise_id",
            { rs, _ -> rs.getObject("exercise_id", UUID::class.java) to rs.getInt("n") },
            userId,
        ).toMap()
}
