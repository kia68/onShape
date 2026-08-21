package de.optadata.odil.onshape.onboarding

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

/** FR-30 (Epic Ernaehrungstracking): Gewicht, Koerpermasse, Koerperfett. */
data class BodyMeasurementInput(
    val measuredOn: LocalDate,
    val weightKg: Double?,
    val bodyFatPct: Double?,
    val waistCm: Double?,
    val hipCm: Double?,
    val chestCm: Double?,
    val armCm: Double?,
    val thighCm: Double?,
)

data class BodyMeasurement(
    val id: UUID,
    val measuredOn: LocalDate,
    val weightKg: Double?,
    val bodyFatPct: Double?,
    val waistCm: Double?,
    val hipCm: Double?,
    val chestCm: Double?,
    val armCm: Double?,
    val thighCm: Double?,
    val source: String,
)

/** Schreibt/liest `body_measurements` (V6) -- der erste Messpunkt entsteht beim Onboarding
 * (FR-02 "Gewicht", siehe [Profile]-Kommentar), spaetere Messpunkte ueber FR-30. */
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

    /** FR-30: voller Messpunkt (Taille/Huefte/Brust/Arm/Oberschenkel zusaetzlich zu Gewicht/KFA). */
    fun record(userId: UUID, input: BodyMeasurementInput): UUID =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO body_measurements (
                user_id, measured_on, weight_kg, body_fat_pct, waist_cm, hip_cm, chest_cm, arm_cm, thigh_cm, source
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'manual')
            ON CONFLICT (user_id, measured_on, source) DO UPDATE SET
                weight_kg = EXCLUDED.weight_kg, body_fat_pct = EXCLUDED.body_fat_pct,
                waist_cm = EXCLUDED.waist_cm, hip_cm = EXCLUDED.hip_cm, chest_cm = EXCLUDED.chest_cm,
                arm_cm = EXCLUDED.arm_cm, thigh_cm = EXCLUDED.thigh_cm
            RETURNING id
            """.trimIndent(),
            UUID::class.java,
            userId, input.measuredOn, input.weightKg, input.bodyFatPct, input.waistCm, input.hipCm,
            input.chestCm, input.armCm, input.thighCm,
        ) ?: error("Insert/Update in body_measurements returned no id")

    fun findHistory(userId: UUID, from: LocalDate, to: LocalDate): List<BodyMeasurement> =
        jdbcTemplate.query(
            """
            SELECT id, measured_on, weight_kg, body_fat_pct, waist_cm, hip_cm, chest_cm, arm_cm, thigh_cm, source
            FROM body_measurements WHERE user_id = ? AND measured_on BETWEEN ? AND ? ORDER BY measured_on DESC
            """.trimIndent(),
            { rs, _ ->
                BodyMeasurement(
                    id = rs.getObject("id", UUID::class.java),
                    measuredOn = rs.getObject("measured_on", LocalDate::class.java),
                    weightKg = rs.getBigDecimal("weight_kg")?.toDouble(),
                    bodyFatPct = rs.getBigDecimal("body_fat_pct")?.toDouble(),
                    waistCm = rs.getBigDecimal("waist_cm")?.toDouble(),
                    hipCm = rs.getBigDecimal("hip_cm")?.toDouble(),
                    chestCm = rs.getBigDecimal("chest_cm")?.toDouble(),
                    armCm = rs.getBigDecimal("arm_cm")?.toDouble(),
                    thighCm = rs.getBigDecimal("thigh_cm")?.toDouble(),
                    source = rs.getString("source"),
                )
            },
            userId, from, to,
        )

    fun findLatestWeight(userId: UUID): Double? =
        jdbcTemplate.query(
            "SELECT weight_kg FROM body_measurements WHERE user_id = ? AND weight_kg IS NOT NULL ORDER BY measured_on DESC LIMIT 1",
            { rs, _ -> rs.getBigDecimal("weight_kg").toDouble() },
            userId,
        ).firstOrNull()
}
