package de.optadata.odil.onshape.nutrition

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper

data class ParsedRecipe(
    val name: String,
    val servings: Double?,
    val instructions: String?,
    val imageUrl: String?,
    val ingredientLines: List<String>,
)

/**
 * FR-27 ("JSON-LD `Recipe` Schema parsen, Fallback auf LLM-Extraktion" -- nur der JSON-LD-Pfad
 * ist hier umgesetzt, siehe RecipeImportService-KDoc fuer die Begruendung). Reine, netzwerkfreie
 * Kernlogik (NFR-13 testbar): Eingabe ist bereits geladenes HTML, keine eigene HTTP-Logik hier.
 *
 * Kein HTML-Parser als neue Abhaengigkeit (gleiches Minimalismus-Prinzip wie [CsvReader]/
 * `HevyCsvParser`) -- `<script type="application/ld+json">`-Bloecke werden per Regex
 * herausgeschnitten, das reicht fuer diesen einen Zweck (im Gegensatz zu allgemeinem HTML-
 * Scraping, das einen echten Parser braeuchte). Ein Block kann laut Schema.org-Spec sein:
 * - ein einzelnes `Recipe`-Objekt,
 * - ein JSON-ARRAY mehrerer Objekte (haeufig bei WordPress/Yoast: Recipe neben WebSite,
 *   BreadcrumbList, ...),
 * - ein Objekt mit `@graph` (Array mehrerer Objekte, Google-eigene Erweiterung, sehr verbreitet).
 * Alle drei Formen werden rekursiv nach dem ersten Objekt mit `@type == "Recipe"` durchsucht
 * (oder `@type` als Array, das "Recipe" enthaelt).
 */
object RecipeJsonLdParser {

    private val SCRIPT_REGEX = Regex(
        """<script[^>]*type\s*=\s*["']application/ld\+json["'][^>]*>(.*?)</script>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val mapper: ObjectMapper = JsonMapper.builder().build()

    fun parse(html: String): ParsedRecipe? {
        for (match in SCRIPT_REGEX.findAll(html)) {
            val raw = match.groupValues[1].trim()
            if (raw.isEmpty()) continue
            val root = try { mapper.readTree(raw) } catch (_: Exception) { continue }
            findRecipeNode(root)?.let { return it.toParsedRecipe() }
        }
        return null
    }

    private fun findRecipeNode(node: JsonNode): JsonNode? {
        if (node.isObject) {
            val type = node.get("@type")
            val isRecipe = type != null && (
                (type.isTextual && type.asString() == "Recipe") ||
                    (type.isArray && type.any { it.isTextual && it.asString() == "Recipe" })
                )
            if (isRecipe) return node
            node.get("@graph")?.let { graph -> findRecipeNode(graph)?.let { return it } }
            return null
        }
        if (node.isArray) {
            for (child in node) {
                findRecipeNode(child)?.let { return it }
            }
        }
        return null
    }

    private fun JsonNode.toParsedRecipe(): ParsedRecipe? {
        val name = get("name")?.takeIf { it.isTextual }?.asString() ?: return null
        val ingredientLines = (get("recipeIngredient") ?: get("ingredients"))
            ?.takeIf { it.isArray }
            ?.mapNotNull { it.takeIf { n -> n.isTextual }?.asString()?.trim()?.takeIf { s -> s.isNotEmpty() } }
            .orEmpty()
        if (ingredientLines.isEmpty()) return null

        return ParsedRecipe(
            name = name.trim(),
            servings = parseYield(get("recipeYield")),
            instructions = parseInstructions(get("recipeInstructions")),
            imageUrl = parseImage(get("image")),
            ingredientLines = ingredientLines,
        )
    }

    /** `recipeYield` ist laut Spec entweder eine Zahl oder ein Freitext wie "4 servings" /
     * "Serves 4-6" -- die erste in der Zeichenkette gefundene Ganzzahl wird verwendet. */
    private fun parseYield(node: JsonNode?): Double? {
        if (node == null) return null
        val text = if (node.isArray) node.firstOrNull()?.asString() else node.asString()
        return text?.let { Regex("""\d+""").find(it)?.value?.toDoubleOrNull() }
    }

    private fun parseImage(node: JsonNode?): String? = when {
        node == null -> null
        node.isTextual -> node.asString()
        node.isArray -> node.firstOrNull()?.let { parseImage(it) }
        node.isObject -> node.get("url")?.takeIf { it.isTextual }?.asString()
        else -> null
    }

    /** `recipeInstructions`: Freitext, Array aus Freitext, oder Array aus `HowToStep`/
     * `HowToSection` (mit verschachteltem `itemListElement`) -- alle Formen werden zu einem
     * einzigen, zeilenweise nummerierten Text zusammengefuehrt (das App-Feld `instructions` ist
     * reiner Text, siehe `Recipe.kt`). */
    private fun parseInstructions(node: JsonNode?): String? {
        if (node == null) return null
        val steps = mutableListOf<String>()
        collectSteps(node, steps)
        if (steps.isEmpty()) return null
        return steps.mapIndexed { i, step -> "${i + 1}. $step" }.joinToString("\n")
    }

    private fun collectSteps(node: JsonNode, out: MutableList<String>) {
        when {
            node.isTextual -> node.asString().trim().takeIf { it.isNotEmpty() }?.let { out += it }
            node.isArray -> node.forEach { collectSteps(it, out) }
            node.isObject -> {
                val type = node.get("@type")?.asString()
                if (type == "HowToSection") {
                    node.get("itemListElement")?.let { collectSteps(it, out) }
                } else {
                    val text = node.get("text")?.takeIf { it.isTextual }?.asString()
                        ?: node.get("name")?.takeIf { it.isTextual }?.asString()
                    text?.trim()?.takeIf { it.isNotEmpty() }?.let { out += it }
                }
            }
        }
    }
}
