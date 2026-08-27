package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.security.currentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** FR-26: eigene Rezepte, Naehrwerte pro Portion skaliert aus den Zutaten. */
@RestController
@RequestMapping("/api/nutrition/recipes")
class RecipeController(
    private val recipeService: RecipeService,
    private val recipeImportService: RecipeImportService,
) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateRecipeRequest, authentication: Authentication): ResponseEntity<RecipeResponse> {
        val result = recipeService.create(
            authentication.currentUserId(), request.name, request.servings, request.instructions,
            request.items.map { RecipeItemInput(it.foodId, it.grams) },
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(result.toResponse())
    }

    /** FR-27: liefert einen Entwurf zum Nachbearbeiten, siehe [RecipeImportDraftResponse]-KDoc --
     * kein POST-Redirect auf [create], der Nutzer legt das Rezept bewusst separat an. */
    @PostMapping("/import")
    fun import(
        @Valid @RequestBody request: RecipeImportRequest,
        @RequestParam(defaultValue = "de") locale: String,
        authentication: Authentication,
    ): RecipeImportDraftResponse = recipeImportService.import(authentication.currentUserId(), request.url, locale)

    @GetMapping
    fun list(authentication: Authentication): List<RecipeResponse> =
        recipeService.listOwnAndPublic(authentication.currentUserId()).map { it.toResponse() }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID, authentication: Authentication): RecipeResponse =
        recipeService.get(authentication.currentUserId(), id).toResponse()
}
