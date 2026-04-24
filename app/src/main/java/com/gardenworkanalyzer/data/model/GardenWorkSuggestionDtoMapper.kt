package com.gardenworkanalyzer.data.model

import com.gardenworkanalyzer.domain.model.GardenWorkSuggestion
import com.gardenworkanalyzer.domain.model.GardenWorkType

/**
 * Maps a [GardenWorkSuggestionDto] to a [GardenWorkSuggestion] domain model.
 *
 * - Unknown type strings are mapped to [GardenWorkType.GENERAL_MAINTENANCE].
 * - Confidence values are clamped to [0.0, 1.0].
 */
fun GardenWorkSuggestionDto.toDomain(): GardenWorkSuggestion {
    val workType = try {
        GardenWorkType.valueOf(type.uppercase())
    } catch (_: IllegalArgumentException) {
        GardenWorkType.GENERAL_MAINTENANCE
    }

    val clampedConfidence = confidence.coerceIn(0.0, 1.0)

    return GardenWorkSuggestion(
        type = workType,
        description = description,
        confidence = clampedConfidence,
        detailedGuidance = detailedGuidance
    )
}
