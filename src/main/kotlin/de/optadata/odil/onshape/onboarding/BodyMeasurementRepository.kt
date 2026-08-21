package de.optadata.odil.onshape.onboarding

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

/** Schreibt den ersten Messpunkt in `body_measurements` (V6) beim Onboarding (FR-02 "Gewicht").
 * Kanonische Gewichtshistorie fuer Fortschrittsauswertung, siehe [Profile]-Kommentar. */
@Repository
class BodyMeasurementRepository(private val jdbcTemplate: JdbcTemplate) {

    fun recordWeight(userId: UUID, measuredOn: LocalDate, weightKg: Double, bodyFatPct: Double?) {
        jdbcTemplate.update(
            """
            INSERT INTO body_measurements (user_id, measured_on, weight_kg, body_fat_pct, source)
            VALUES (?, ?, ?, ?, 'manual')
            ON CONFLICT (user_id, measured_on, source) DO UPDATE SET
                weight_kg = EXCLUDED.weight_kg, body_fat_pct = EXCLUDED.body_fat_pct
            """.trimIndent(),
            userId, measuredOn, weightKg, bodyFatPct,
        )
    }

    fun findLatestWeight(userId: UUID): Double? =
        jdbcTemplate.query(
            "SELECT weight_kg FROM body_measurements WHERE user_id = ? AND weight_kg IS NOT NULL ORDER BY measured_on DESC LIMIT 1",
            { rs, _ -> rs.getBigDecimal("weight_kg").toDouble() },
            userId,
        ).firstOrNull()
}
