package de.optadata.odil.onshape.foodimport

import org.postgresql.util.PGobject
import org.springframework.jdbc.core.ConnectionCallback
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.sql.Connection
import java.sql.Statement
import java.util.UUID

/**
 * Datenzugriff fuer den Lebensmittel-Import (`foods`/`food_servings`, V2__foods.sql).
 * Bewusst mit rohem JDBC statt JPA: Row-Level Security greift ueber die
 * `app.current_user_id`-Session-Variable (V8__row_level_security.sql), die `foods`-Tabelle
 * ist aber keine Nutzertabelle — hier zaehlt vor allem Kontrolle ueber Arrays/JSONB, die
 * ein einfacher JdbcTemplate-Zugriff am direktesten abbildet.
 */
@Repository
class FoodImportRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) {

    fun findBySameSource(source: FoodSource, sourceId: String): ExistingFood? =
        jdbcTemplate.query(
            "SELECT id, source, source_id, trust FROM foods WHERE source = ?::food_source_t AND source_id = ?",
            { rs, _ -> rs.toExistingFood() },
            source.dbValue,
            sourceId,
        ).firstOrNull()

    /** Bester Barcode-Treffer ueber alle Quellen hinweg, hoechste Vertrauensstufe zuerst. */
    fun findByBarcode(barcode: String): ExistingFood? =
        jdbcTemplate.query(
            "SELECT id, source, source_id, trust FROM foods WHERE barcode = ? " +
                "ORDER BY CASE trust WHEN 'verified' THEN 2 WHEN 'community' THEN 1 ELSE 0 END DESC LIMIT 1",
            { rs, _ -> rs.toExistingFood() },
            barcode,
        ).firstOrNull()

    /** Trigram-Fuzzy-Match auf name_de (nutzt den GIN-Trigram-Index aus V2__foods.sql). */
    fun findBestFuzzyMatch(
        nameDe: String,
        threshold: Double = Deduplicator.DEFAULT_FUZZY_SIMILARITY_THRESHOLD,
    ): ExistingFood? =
        jdbcTemplate.query(
            "SELECT id, source, source_id, trust FROM foods " +
                "WHERE similarity(name_de, ?) > ? " +
                "ORDER BY similarity(name_de, ?) DESC, " +
                "CASE trust WHEN 'verified' THEN 2 WHEN 'community' THEN 1 ELSE 0 END DESC LIMIT 1",
            { rs, _ -> rs.toExistingFood() },
            nameDe,
            threshold,
            nameDe,
        ).firstOrNull()

    fun insert(food: ImportedFood, trust: TrustLevel): UUID =
        jdbcTemplate.execute(ConnectionCallback { connection ->
            connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS).use { ps ->
                food.bindTo(ps, connection, trust)
                ps.executeUpdate()
                ps.generatedKeys.use { keys ->
                    keys.next()
                    UUID.fromString(keys.getString(1))
                }
            }
        }) ?: error("Insert into foods returned no id")

    fun update(id: UUID, food: ImportedFood, trust: TrustLevel) {
        jdbcTemplate.execute(ConnectionCallback { connection ->
            connection.prepareStatement(UPDATE_SQL).use { ps ->
                food.bindTo(ps, connection, trust)
                ps.setObject(food.paramCount() + 1, id)
                ps.executeUpdate()
            }
        })
    }

    private fun java.sql.ResultSet.toExistingFood() = ExistingFood(
        id = UUID.fromString(getString("id")),
        source = FoodSource.entries.first { it.dbValue == getString("source") },
        sourceId = getString("source_id"),
        trust = TrustLevel.entries.first { it.dbValue == getString("trust") },
    )

    private fun ImportedFood.paramCount() = 22

    private fun ImportedFood.bindTo(ps: java.sql.PreparedStatement, connection: Connection, trust: TrustLevel) {
        var i = 1
        ps.setString(i++, source.dbValue)
        ps.setString(i++, sourceId)
        ps.setString(i++, trust.dbValue)
        ps.setString(i++, barcode)
        ps.setString(i++, brand)
        ps.setString(i++, nameDe)
        ps.setString(i++, nameEn)
        ps.setString(i++, category)
        ps.setObject(i++, novaGroup)
        ps.setString(i++, nutriscore?.toString())
        ps.setDouble(i++, kcal)
        ps.setDouble(i++, proteinG)
        ps.setDouble(i++, fatG)
        ps.setObject(i++, saturatedFatG)
        ps.setObject(i++, carbsG)
        ps.setObject(i++, sugarG)
        ps.setObject(i++, fiberG)
        // Rest (salt, micros, allergens, additives, is_liquid) folgt unten ueber erweiterte Bindung.
        bindExtra(ps, connection, i)
    }

    private fun ImportedFood.bindExtra(ps: java.sql.PreparedStatement, connection: Connection, startIndex: Int) {
        var i = startIndex
        ps.setObject(i++, saltG)
        ps.setObject(i++, micros.toJsonb())
        ps.setArray(i++, connection.createArrayOf("text", allergens.toTypedArray()))
        ps.setArray(i++, connection.createArrayOf("text", additives.toTypedArray()))
        ps.setBoolean(i, isLiquid)
    }

    private fun Map<String, Double>.toJsonb(): PGobject = PGobject().apply {
        type = "jsonb"
        value = objectMapper.writeValueAsString(this@toJsonb)
    }

    private companion object {
        const val INSERT_SQL = """
            INSERT INTO foods (
                source, source_id, trust, barcode, brand, name_de, name_en, category,
                nova_group, nutriscore, kcal, protein_g, fat_g, saturated_fat_g, carbs_g,
                sugar_g, fiber_g, salt_g, micros, allergens, additives, is_liquid
            ) VALUES (
                ?::food_source_t, ?, ?::trust_t, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
        """

        const val UPDATE_SQL = """
            UPDATE foods SET
                source = ?::food_source_t, source_id = ?, trust = ?::trust_t, barcode = ?,
                brand = ?, name_de = ?, name_en = ?, category = ?, nova_group = ?,
                nutriscore = ?, kcal = ?, protein_g = ?, fat_g = ?, saturated_fat_g = ?,
                carbs_g = ?, sugar_g = ?, fiber_g = ?, salt_g = ?, micros = ?, allergens = ?,
                additives = ?, is_liquid = ?, updated_at = now()
            WHERE id = ?
        """
    }
}
