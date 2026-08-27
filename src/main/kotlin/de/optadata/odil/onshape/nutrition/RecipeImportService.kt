package de.optadata.odil.onshape.nutrition

import org.springframework.stereotype.Service
import java.util.UUID

class RecipeImportFailedException(message: String) : RuntimeException(message)

/**
 * FR-27 ("JSON-LD `Recipe` Schema parsen, Fallback auf LLM-Extraktion"): nur der JSON-LD-Pfad
 * ist umgesetzt -- gleiches Vertagungsmuster wie OCR/Vision-AI in fruaheren Epics, hier aber
 * mit einer wichtigen Nuance: der Fallback ist NICHT der einzige Weg zum Ziel wie sonst, JSON-LD
 * ist bereits der von KONZEPT.md genannte PRIMAERE Weg und deckt einen grossen Teil real
 * existierender Rezeptseiten ab (Google verlangt es fuer Recipe-Rich-Snippets, siehe
 * developers.google.com/search/docs/appearance/structured-data/recipe -- ein starker Anreiz
 * fuer Foodblogs, es korrekt einzubinden).
 *
 * Orchestriert [SafeUrlValidator] -> [RecipeUrlFetcher] -> [RecipeJsonLdParser] ->
 * [IngredientLineParser] -> [FoodSearchService] (bestehende Volltextsuche aus FR-22, KEIN neuer
 * Matching-Algorithmus). Liefert einen Entwurf, keine gespeicherte Ressource, siehe
 * [RecipeImportDraftResponse]-KDoc.
 */
@Service
class RecipeImportService(
    private val recipeUrlFetcher: RecipeUrlFetcher,
    private val foodSearchService: FoodSearchService,
) {
    private companion object {
        const val SUGGESTIONS_PER_INGREDIENT = 3
    }

    fun import(userId: UUID, rawUrl: String, locale: String): RecipeImportDraftResponse {
        val url = SafeUrlValidator.validate(rawUrl)
        val html = recipeUrlFetcher.fetchHtml(url)
            ?: throw RecipeImportFailedException("Seite konnte nicht geladen werden")
        val parsed = RecipeJsonLdParser.parse(html)
            ?: throw RecipeImportFailedException("Kein Rezept (JSON-LD) auf dieser Seite gefunden")

        val ingredients = parsed.ingredientLines.map { line ->
            val parsedLine = IngredientLineParser.parse(line)
            val suggestions = if (parsedLine.ingredientName.isBlank()) {
                emptyList()
            } else {
                foodSearchService.search(userId, parsedLine.ingredientName, locale, SUGGESTIONS_PER_INGREDIENT)
            }
            ParsedIngredientResponse(
                rawText = parsedLine.rawText,
                quantity = parsedLine.quantity,
                unit = parsedLine.unit,
                ingredientName = parsedLine.ingredientName,
                gramsResolved = parsedLine.gramsResolved,
                suggestions = suggestions.map { it.toResponse() },
            )
        }

        return RecipeImportDraftResponse(
            name = parsed.name,
            servings = parsed.servings ?: 1.0,
            instructions = parsed.instructions,
            sourceUrl = url.toString(),
            ingredients = ingredients,
        )
    }
}
