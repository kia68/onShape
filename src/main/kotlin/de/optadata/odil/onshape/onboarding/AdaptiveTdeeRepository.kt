package de.optadata.odil.onshape.onboarding

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

data class TdeeEstimate(
    val computedOn: LocalDate,
    val windowDays: Int,
    val avgIntakeKcal: Double,
    val weightDeltaKg: Double,
    val tdeeObservedKcal: Double,
    val tdeeSmoothedKcal: Int,
    val logAdherence: Double,
    val applied: Boolean,
)

/**
 * Nutzt `tdee_estimates` (V6__measurements.sql) -- das Grundlagenschema hat diese Tabelle von
 * Anfang an fuer FR-134 vorgesehen (Kommentar dort: "§8.6 Messwerte und adaptives Modell"), bis
 * jetzt aber nirgends befuellt. `applied` ist bewusst immer `false`: dieser Durchgang schreibt
 * den adaptiven Wert NICHT in `nutrition_targets` zurueck (siehe AdaptiveTdeeService-KDoc) --
 * das Flag existiert fuer eine spaetere Session, die diese Rueckkopplung tatsaechlich umsetzt.
 * Muss innerhalb von `RlsSession.asUser` laufen (owner_only + FORCE, V8).
 */
@Repository
class AdaptiveTdeeRepository(private val jdbcTemplate: JdbcTemplate) {

    fun findLatest(userId: UUID): TdeeEstimate? =
        jdbcTemplate.query(
            "$SELECT_SQL WHERE user_id = ? ORDER BY computed_on DESC LIMIT 1",
            { rs, _ ->
                TdeeEstimate(
                    computedOn = rs.getObject("computed_on", LocalDate::class.java),
                    windowDays = rs.getInt("window_days"),
                    avgIntakeKcal = rs.getDouble("avg_intake"),
                    weightDeltaKg = rs.getDouble("weight_delta_kg"),
                    tdeeObservedKcal = rs.getDouble("tdee_observed"),
                    tdeeSmoothedKcal = rs.getBigDecimal("tdee_smoothed").toInt(),
                    logAdherence = rs.getDouble("log_adherence"),
                    applied = rs.getBoolean("applied"),
                )
            },
            userId,
        ).firstOrNull()

    fun insert(userId: UUID, estimate: TdeeEstimate) {
        jdbcTemplate.update(
            """
            INSERT INTO tdee_estimates
                (user_id, computed_on, window_days, avg_intake, weight_delta_kg, tdee_observed, tdee_smoothed, log_adherence, applied)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, false)
            ON CONFLICT (user_id, computed_on) DO UPDATE SET
                window_days = EXCLUDED.window_days, avg_intake = EXCLUDED.avg_intake,
                weight_delta_kg = EXCLUDED.weight_delta_kg, tdee_observed = EXCLUDED.tdee_observed,
                tdee_smoothed = EXCLUDED.tdee_smoothed, log_adherence = EXCLUDED.log_adherence
            """.trimIndent(),
            userId, estimate.computedOn, estimate.windowDays, estimate.avgIntakeKcal, estimate.weightDeltaKg,
            estimate.tdeeObservedKcal, estimate.tdeeSmoothedKcal, estimate.logAdherence,
        )
    }

    private companion object {
        const val SELECT_SQL =
            "SELECT computed_on, window_days, avg_intake, weight_delta_kg, tdee_observed, tdee_smoothed, log_adherence, applied FROM tdee_estimates"
    }
}
