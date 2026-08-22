package de.optadata.odil.onshape.movement

import java.util.UUID

data class LocalizedExerciseContent(
    val videoFrontUrl: String?,
    val videoSideUrl: String?,
    val thumbnailUrl: String?,
    val setupSteps: List<String>,
    val executionSteps: List<String>,
    val cues: List<String>,
    val breathing: String?,
    val tempo: String?,
    val whatIsNormal: String?,
    val regressionOf: UUID?,
    val progressionTo: UUID?,
)

data class ExerciseMistake(
    val id: UUID,
    val title: String,
    val whyBad: String,
    val fix: String,
    val imageUrl: String?,
    val severity: Int,
)
