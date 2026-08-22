package de.optadata.odil.onshape.nutrition

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

/** Muss innerhalb von `RlsSession.asUser(userId) { ... }` laufen (owner_only-Policy, V8). */
@Repository
class FoodEntryRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) {

    /** FR-31 (Offline-Sync): bei gesetzter `clientId` idempotent -- ein Retry desselben
     * Offline-Eintrags erzeugt keinen Duplikat-Datensatz (Unique-Index in V3). */
    fun insert(userId: UUID, entry: NewFoodEntry): FoodEntry {
        val insertedId = jdbcTemplate.query(
            """
            INSERT INTO food_entries (
                user_id, food_id, recipe_id, logged_date, slot, grams, serving_id, method,
                kcal, protein_g, fat_g, carbs_g, micros, client_id
            ) VALUES (?, ?, ?, ?, ?::meal_slot_t, ?, ?, ?::entry_method_t, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (user_id, client_id) WHERE client_id IS NOT NULL DO NOTHING
            RETURNING id
            """.trimIndent(),
            { rs, _ -> rs.getObject("id", UUID::class.java) },
            userId, entry.foodId, entry.recipeId, entry.loggedDate, entry.slot.dbValue, entry.grams,
            entry.servingId, entry.method.dbValue, entry.kcal, entry.proteinG, entry.fatG, entry.carbsG,
            MicroNutrients.toJsonb(objectMapper, entry.micros), entry.clientId,
        ).firstOrNull()

        val id = insertedId
            ?: (entry.clientId?.let { findByClientId(userId, it)?.id }
                ?: error("Insert into food_entries returned no id and no client_id conflict to recover from"))
        return findById(id) ?: error("Just-inserted food_entries row $id not found")
    }

    fun findById(id: UUID): FoodEntry? =
        jdbcTemplate.query(SELECT_SQL + " WHERE id = ?", { rs, _ -> rs.toEntry() }, id).firstOrNull()

    fun findByClientId(userId: UUID, clientId: String): FoodEntry? =
        jdbcTemplate.query(
            SELECT_SQL + " WHERE user_id = ? AND client_id = ?",
            { rs, _ -> rs.toEntry() },
            userId, clientId,
        ).firstOrNull()

    fun findByDate(userId: UUID, date: LocalDate): List<FoodEntry> =
        jdbcTemplate.query(
            SELECT_SQL + " WHERE user_id = ? AND logged_date = ? ORDER BY created_at",
            { rs, _ -> rs.toEntry() },
            userId, date,
        )

    /** FR-137 (Datenexport). */
    fun findAllForUser(userId: UUID): List<FoodEntry> =
        jdbcTemplate.query(SELECT_SQL + " WHERE user_id = ? ORDER BY logged_date, created_at", { rs, _ -> rs.toEntry() }, userId)

    /** FR-20: Tagesansicht mit lesbarem Namen statt nur `food_id`/`recipe_id`. */
    fun findByDateWithNames(userId: UUID, date: LocalDate, locale: String): List<FoodEntryWithName> {
        val nameColumn = if (locale == "en") "name_en" else "name_de"
        return jdbcTemplate.query(
            """
            SELECT fe.id, fe.user_id, fe.food_id, fe.recipe_id, fe.logged_date, fe.slot, fe.grams,
                   fe.serving_id, fe.method, fe.kcal, fe.protein_g, fe.fat_g, fe.carbs_g, fe.micros,
                   fe.client_id, fe.created_at, COALESCE(f.$nameColumn, r.$nameColumn) AS display_name
            FROM food_entries fe
            LEFT JOIN foods f ON f.id = fe.food_id
            LEFT JOIN recipes r ON r.id = fe.recipe_id
            WHERE fe.user_id = ? AND fe.logged_date = ?
            ORDER BY fe.created_at
            """.trimIndent(),
            { rs, _ -> FoodEntryWithName(rs.toEntry(), rs.getString("display_name")) },
            userId, date,
        )
    }

    fun delete(userId: UUID, id: UUID): Boolean =
        jdbcTemplate.update("DELETE FROM food_entries WHERE id = ? AND user_id = ?", id, userId) > 0

    /** FR-131: taegliche Summen fuer den Kalorien-/Makro-Verlauf. Nur Tage MIT mindestens einem
     * Eintrag werden zurueckgegeben (fehlende Tage = nicht geloggt, siehe [de.optadata.odil.onshape.progress.AdherenceCalculator]). */
    fun findDailyTotals(userId: UUID, from: LocalDate, to: LocalDate): List<DailyNutritionTotal> =
        jdbcTemplate.query(
            """
            SELECT logged_date, SUM(kcal) AS kcal, SUM(protein_g) AS protein_g, SUM(fat_g) AS fat_g, SUM(carbs_g) AS carbs_g
            FROM food_entries WHERE user_id = ? AND logged_date BETWEEN ? AND ?
            GROUP BY logged_date ORDER BY logged_date
            """.trimIndent(),
            { rs, _ ->
                DailyNutritionTotal(
                    date = rs.getObject("logged_date", LocalDate::class.java),
                    kcal = rs.getDouble("kcal"),
                    proteinG = rs.getDouble("protein_g"),
                    fatG = rs.getDouble("fat_g"),
                    carbsG = rs.getDouble("carbs_g"),
                )
            },
            userId, from, to,
        )

    private fun ResultSet.toEntry() = FoodEntry(
        id = getObject("id", UUID::class.java),
        userId = getObject("user_id", UUID::class.java),
        foodId = getObject("food_id", UUID::class.java),
        recipeId = getObject("recipe_id", UUID::class.java),
        loggedDate = getObject("logged_date", LocalDate::class.java),
        slot = MealSlot.entries.first { it.dbValue == getString("slot") },
        grams = getDouble("grams"),
        servingId = getObject("serving_id", UUID::class.java),
        method = EntryMethod.entries.first { it.dbValue == getString("method") },
        kcal = getDouble("kcal"),
        proteinG = getDouble("protein_g"),
        fatG = getDouble("fat_g"),
        carbsG = getDouble("carbs_g"),
        micros = MicroNutrients.parse(objectMapper, getString("micros")),
        clientId = getString("client_id"),
        createdAt = getTimestamp("created_at").toInstant(),
    )

    private companion object {
        const val SELECT_SQL = """
            SELECT id, user_id, food_id, recipe_id, logged_date, slot, grams, serving_id, method,
                   kcal, protein_g, fat_g, carbs_g, micros, client_id, created_at
            FROM food_entries
        """
    }
}
