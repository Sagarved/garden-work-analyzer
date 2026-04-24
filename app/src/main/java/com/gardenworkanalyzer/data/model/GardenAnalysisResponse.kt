package com.gardenworkanalyzer.data.model

data class GardenAnalysisResponse(
    val suggestions: List<GardenWorkSuggestionDto>,
    val gardenContentDetected: Boolean
)
