package com.gardenworkanalyzer.data.model

data class GardenWorkSuggestionDto(
    val type: String,
    val description: String,
    val confidence: Double,
    val detailedGuidance: String
)
