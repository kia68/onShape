package de.optadata.odil.onshape.nutrition

data class ParsedIngredientLine(
    val rawText: String,
    val quantity: Double?,
    val unit: String?,
    /** Der Zutatenname OHNE Menge/Einheit -- Grundlage fuer die Lebensmittelsuche. */
    val ingredientName: String,
    /** Nur gesetzt, wenn [unit] eine der Masseinheiten g/kg/ml/l ist (siehe Klassen-KDoc) --
     * sonst muss der Nutzer die Menge manuell eintragen (`RecipeItem` braucht zwingend Gramm,
     * V3__nutrition_log.sql). */
    val gramsResolved: Double?,
)

/**
 * FR-27: zerlegt eine rohe Zutatenzeile ("400ml pineapple juice", "1 1/2 cups flour",
 * "3 very ripe bananas, mashed") in Menge/Einheit/Name. Reine, netzwerkfreie Kernlogik
 * (NFR-13 testbar).
 *
 * NUR Masseinheiten mit einer festen, verlaesslichen Gramm-Umrechnung werden automatisch
 * aufgeloest: g/kg direkt, ml/l unter der Annahme Dichte ~1 g/ml (gleiche pragmatische
 * Naeherung wie schon bei der Gewichtseinheit im Strong-CSV-Import, Epic #10 -- "ein Rohwert
 * bleibt naeherungsweise brauchbar"). Volumen-/Stueck-Einheiten wie cup/tbsp/tsp/oz/piece/clove
 * haben KEINE feste Dichte (1 cup Mehl != 1 cup Zucker in Gramm) -- eine automatische
 * Umrechnung waere Scheinpraezision und wird bewusst NICHT versucht (gleiche Haltung wie bei
 * der Strong-Distanz-Warnung: "nicht importieren plus Warnung ist ehrlicher als ein falscher
 * Wert"). Diese Zutaten bleiben mit erkannter Menge/Einheit, aber ohne [ParsedIngredientLine.gramsResolved]
 * -- der Nutzer traegt die Gramm-Menge manuell ein.
 */
object IngredientLineParser {

    private val UNICODE_FRACTIONS = mapOf(
        '½' to 0.5, '⅓' to 1.0 / 3, '⅔' to 2.0 / 3, '¼' to 0.25, '¾' to 0.75,
        '⅕' to 0.2, '⅖' to 0.4, '⅗' to 0.6, '⅘' to 0.8, '⅙' to 1.0 / 6, '⅚' to 5.0 / 6,
        '⅛' to 0.125, '⅜' to 0.375, '⅝' to 0.625, '⅞' to 0.875,
    )

    /** Nach Gramm umrechenbare Einheiten -> Faktor auf Gramm. Absichtlich NICHT hier: cup, tbsp,
     * tsp, oz, lb, piece, clove, slice, whole, can, pinch, bunch, stalk, head (siehe Klassen-KDoc). */
    private val GRAM_CONVERTIBLE_UNITS = mapOf(
        "g" to 1.0, "gram" to 1.0, "grams" to 1.0, "gramm" to 1.0,
        "kg" to 1000.0, "kilogram" to 1000.0, "kilograms" to 1000.0, "kilogramm" to 1000.0,
        "ml" to 1.0, "milliliter" to 1.0, "milliliters" to 1.0, "millilitre" to 1.0, "millilitres" to 1.0,
        "l" to 1000.0, "liter" to 1000.0, "liters" to 1000.0, "litre" to 1000.0, "litres" to 1000.0,
    )

    /** Alle erkannten (nicht nur umrechenbaren) Einheitswoerter -- laenger zuerst, damit z.B.
     * "tablespoons" vor "tablespoon" vor "tbsp" geprueft wird. */
    private val ALL_UNITS = (
        GRAM_CONVERTIBLE_UNITS.keys + listOf(
            "cups", "cup", "tablespoons", "tablespoon", "tbsp", "teaspoons", "teaspoon", "tsp",
            "ounces", "ounce", "oz", "pounds", "pound", "lb", "lbs", "cloves", "clove", "slices", "slice",
            "pieces", "piece", "cans", "can", "pinches", "pinch", "bunches", "bunch", "stalks", "stalk",
            "heads", "head", "whole", "large", "medium", "small",
        )
        ).sortedByDescending { it.length }

    private val QUANTITY_REGEX = Regex(
        """^\s*(\d+\s+\d+/\d+|\d+/\d+|\d+[.,]\d+|\d+|[${UNICODE_FRACTIONS.keys.joinToString("")}])(?:\s*[-–]\s*(?:\d+\s+\d+/\d+|\d+/\d+|\d+[.,]\d+|\d+))?\s*""",
    )

    fun parse(rawText: String): ParsedIngredientLine {
        var remainder = rawText.trim()

        val quantity = QUANTITY_REGEX.find(remainder)?.let { match ->
            remainder = remainder.substring(match.range.last + 1).trim()
            parseQuantityToken(match.groupValues[1])
        }

        var unit: String? = null
        for (candidate in ALL_UNITS) {
            if (remainder.startsWith("$candidate ", ignoreCase = true) || remainder.equals(candidate, ignoreCase = true)) {
                unit = candidate.lowercase()
                remainder = remainder.removeRange(0, candidate.length).trim()
                break
            }
        }

        val name = remainder.trim().trim(',', '.').trim().ifBlank { rawText.trim() }
        val gramsResolved = if (quantity != null && unit != null) {
            GRAM_CONVERTIBLE_UNITS[unit]?.let { factor -> quantity * factor }
        } else {
            null
        }

        return ParsedIngredientLine(rawText.trim(), quantity, unit, name, gramsResolved)
    }

    /** "1 1/2" (gemischte Zahl), "1/2" (Bruch), "1,5"/"1.5" (Dezimalzahl), "2" (Ganzzahl), oder
     * ein einzelnes Unicode-Bruchzeichen. */
    private fun parseQuantityToken(token: String): Double? {
        val t = token.trim()
        if (t.length == 1 && UNICODE_FRACTIONS.containsKey(t[0])) return UNICODE_FRACTIONS[t[0]]
        if (" " in t) {
            val (whole, frac) = t.split(" ", limit = 2)
            return (whole.toDoubleOrNull() ?: return null) + (parseFraction(frac) ?: 0.0)
        }
        if ("/" in t) return parseFraction(t)
        return t.replace(',', '.').toDoubleOrNull()
    }

    private fun parseFraction(s: String): Double? {
        val parts = s.split("/")
        if (parts.size != 2) return null
        val num = parts[0].toDoubleOrNull() ?: return null
        val den = parts[1].toDoubleOrNull() ?: return null
        if (den == 0.0) return null
        return num / den
    }
}
