package de.optadata.odil.onshape.onboarding

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID

/** Schreibt/liest `profiles` (V1). FR-02..FR-09 landen alle in einem Datensatz pro Nutzer --
 * das Onboarding ist ein einziger kombinierter Schritt (FR-10: <=90 Sekunden). */
@Repository
class ProfileRepository(private val jdbcTemplate: JdbcTemplate) {

    fun findByUserId(userId: UUID): Profile? =
        jdbcTemplate.query(SELECT_SQL, { rs, _ -> rs.toProfile() }, userId).firstOrNull()

    fun upsert(profile: Profile) {
        jdbcTemplate.execute(org.springframework.jdbc.core.ConnectionCallback { connection ->
            connection.prepareStatement(UPSERT_SQL).use { ps ->
                profile.bindTo(ps, connection)
                ps.executeUpdate()
            }
        })
    }

    private fun ResultSet.toProfile() = Profile(
        userId = getObject("user_id", UUID::class.java),
        sex = Sex.entries.first { it.dbValue == getString("sex") },
        birthDate = getObject("birth_date", java.time.LocalDate::class.java),
        heightCm = getDouble("height_cm"),
        experience = Experience.entries.first { it.dbValue == getString("experience") },
        activityPal = getDouble("activity_pal"),
        goal = Goal.entries.first { it.dbValue == getString("goal") },
        goalRatePctWeek = getDouble("goal_rate_pct_week"),
        targetWeightKg = getBigDecimal("target_weight_kg")?.toDouble(),
        bodyFatPct = getBigDecimal("body_fat_pct")?.toDouble(),
        dietaryPrefs = (getArray("dietary_prefs")?.array as Array<*>?)?.map { it.toString() } ?: emptyList(),
        allergens = (getArray("allergens")?.array as Array<*>?)?.map { it.toString() } ?: emptyList(),
        injuries = (getArray("injuries")?.array as Array<*>?)?.map { it.toString() } ?: emptyList(),
        equipment = (getArray("equipment")?.array as Array<*>?)?.map { it.toString() } ?: emptyList(),
        trainingDaysWeek = getInt("training_days_week"),
        sessionMinutes = getInt("session_minutes"),
    )

    private fun Profile.bindTo(ps: java.sql.PreparedStatement, connection: Connection) {
        var i = 1
        ps.setObject(i++, userId)
        ps.setString(i++, sex.dbValue)
        ps.setObject(i++, birthDate)
        ps.setDouble(i++, heightCm)
        ps.setString(i++, experience.dbValue)
        ps.setDouble(i++, activityPal)
        ps.setString(i++, goal.dbValue)
        ps.setDouble(i++, goalRatePctWeek)
        ps.setObject(i++, targetWeightKg)
        ps.setObject(i++, bodyFatPct)
        ps.setArray(i++, connection.createArrayOf("text", dietaryPrefs.toTypedArray()))
        ps.setArray(i++, connection.createArrayOf("text", allergens.toTypedArray()))
        ps.setArray(i++, connection.createArrayOf("text", injuries.toTypedArray()))
        ps.setArray(i++, connection.createArrayOf("text", equipment.toTypedArray()))
        ps.setInt(i++, trainingDaysWeek)
        ps.setInt(i, sessionMinutes)
    }

    private companion object {
        const val SELECT_SQL = """
            SELECT user_id, sex, birth_date, height_cm, experience, activity_pal, goal,
                   goal_rate_pct_week, target_weight_kg, body_fat_pct, dietary_prefs, allergens,
                   injuries, equipment, training_days_week, session_minutes
            FROM profiles WHERE user_id = ?
        """

        const val UPSERT_SQL = """
            INSERT INTO profiles (
                user_id, sex, birth_date, height_cm, experience, activity_pal, goal,
                goal_rate_pct_week, target_weight_kg, body_fat_pct, dietary_prefs, allergens,
                injuries, equipment, training_days_week, session_minutes
            ) VALUES (?, ?::sex_t, ?, ?, ?::experience_t, ?, ?::goal_t, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (user_id) DO UPDATE SET
                sex = EXCLUDED.sex, birth_date = EXCLUDED.birth_date, height_cm = EXCLUDED.height_cm,
                experience = EXCLUDED.experience, activity_pal = EXCLUDED.activity_pal,
                goal = EXCLUDED.goal, goal_rate_pct_week = EXCLUDED.goal_rate_pct_week,
                target_weight_kg = EXCLUDED.target_weight_kg, body_fat_pct = EXCLUDED.body_fat_pct,
                dietary_prefs = EXCLUDED.dietary_prefs, allergens = EXCLUDED.allergens,
                injuries = EXCLUDED.injuries, equipment = EXCLUDED.equipment,
                training_days_week = EXCLUDED.training_days_week, session_minutes = EXCLUDED.session_minutes,
                updated_at = now()
        """
    }
}
