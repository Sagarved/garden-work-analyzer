package com.gardenworkanalyzer.domain.model

data class GardenAnalysisResult(
    val suggestions: List<GardenWorkSuggestion>,
    val gardenContentDetected: Boolean
)
