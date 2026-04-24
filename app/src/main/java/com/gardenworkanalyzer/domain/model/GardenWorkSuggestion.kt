package com.gardenworkanalyzer.domain.model

data class GardenWorkSuggestion(
    val type: GardenWorkType,
    val description: String,
    val confidence: Double, // 0.0 to 1.0
    val detailedGuidance: String
)
