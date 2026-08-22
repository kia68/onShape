package de.optadata.odil.onshape.movement

import java.util.UUID

data class ExerciseMistakeResponse(val id: UUID, val title: String, val whyBad: String, val fix: String, val imageUrl: String?, val severity: Int)

fun ExerciseMistake.toResponse() = ExerciseMistakeResponse(id, title, whyBad, fix, imageUrl, severity)

data class ProgressionLinkResponse(val id: UUID, val slug: String, val name: String)

fun ProgressionLink.toResponse() = ProgressionLinkResponse(id, slug, name)

data class StartingWeightResponse(val weightKg: Double?, val reasonCode: String)

fun StartingWeightRecommendation.toResponse() = StartingWeightResponse(weightKg, reasonCode)

data class ExerciseDetailResponse(
    val id: UUID,
    val slug: String,
    val name: String,
    val pattern: String,
    val mechanic: String,
    val equipment: List<String>,
    val difficulty: String,
    val primaryMuscles: List<String>,
    val videoFrontUrl: String?,
    val videoSideUrl: String?,
    val thumbnailUrl: String?,
    val setupSteps: List<String>,
    val executionSteps: List<String>,
    val cues: List<String>,
    val breathing: String?,
    val tempo: String?,
    val whatIsNormal: String?,
    val mistakes: List<ExerciseMistakeResponse>,
    val regressionOf: ProgressionLinkResponse?,
    val progressionTo: ProgressionLinkResponse?,
    val startingWeight: StartingWeightResponse?,
    val showBeginnerIntro: Boolean,
    val hasContent: Boolean,
)

fun ExerciseDetailResult.toResponse() = ExerciseDetailResponse(
    id = exercise.id,
    slug = exercise.slug,
    name = exercise.name,
    pattern = exercise.pattern.dbValue,
    mechanic = exercise.mechanic.dbValue,
    equipment = exercise.equipment,
    difficulty = exercise.difficulty.dbValue,
    primaryMuscles = exercise.primaryMuscles,
    videoFrontUrl = content.videoFrontUrl,
    videoSideUrl = content.videoSideUrl,
    thumbnailUrl = content.thumbnailUrl,
    setupSteps = content.setupSteps,
    executionSteps = content.executionSteps,
    cues = content.cues,
    breathing = content.breathing,
    tempo = content.tempo,
    whatIsNormal = content.whatIsNormal,
    mistakes = mistakes.map { it.toResponse() },
    regressionOf = regressionOf?.toResponse(),
    progressionTo = progressionTo?.toResponse(),
    startingWeight = startingWeight?.toResponse(),
    showBeginnerIntro = showBeginnerIntro,
    // FR-110 Redaktions-Umfang (KONZEPT.md 12.3) ist fuer 120 Uebungen nicht leistbar (siehe
    // V15-Migrationskommentar) -- das Frontend zeigt fuer Uebungen ohne Content einen "folgt
    // noch"-Platzhalter statt leerer Abschnitte.
    hasContent = content.setupSteps.isNotEmpty() || content.executionSteps.isNotEmpty() || content.cues.isNotEmpty(),
)
