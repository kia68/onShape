package de.optadata.odil.onshape.nutrition

import org.postgresql.util.PGobject
import tools.jackson.databind.ObjectMapper

/** FR-28: Mikronaehrstoff-Tracking. `foods.micros`/`food_entries.micros` sind jsonb-Maps
 * mit variablen Schluesseln (z. B. "iron_mg", "vitamin_d_ug") -- siehe V2__foods.sql. */
object MicroNutrients {

    fun parse(objectMapper: ObjectMapper, jsonOrNull: String?): Map<String, Double> {
        if (jsonOrNull.isNullOrBlank()) return emptyMap()
        @Suppress("UNCHECKED_CAST")
        val raw = objectMapper.readValue(jsonOrNull, Map::class.java) as Map<String, Any?>
        return raw.mapValues { (_, v) -> (v as Number).toDouble() }
    }

    fun scale(micros: Map<String, Double>, grams: Double): Map<String, Double> =
        micros.mapValues { (_, per100g) -> per100g * grams / 100.0 }

    fun sum(entries: List<Map<String, Double>>): Map<String, Double> {
        val totals = mutableMapOf<String, Double>()
        for (entry in entries) {
            for ((key, value) in entry) {
                totals[key] = (totals[key] ?: 0.0) + value
            }
        }
        return totals
    }

    fun toJsonb(objectMapper: ObjectMapper, micros: Map<String, Double>): PGobject = PGobject().apply {
        type = "jsonb"
        value = objectMapper.writeValueAsString(micros)
    }
}
