package com.gardenworkanalyzer.domain.model

sealed class AnalysisUiState {
    object Idle : AnalysisUiState()
    data class Uploading(val progress: Float) : AnalysisUiState()
    data class Success(val result: GardenAnalysisResult) : AnalysisUiState()
    data class Error(val message: String, val retryCount: Int) : AnalysisUiState()
}
