package com.gardenworkanalyzer.data.repository

import com.gardenworkanalyzer.domain.model.GardenAnalysisResult
import com.gardenworkanalyzer.domain.model.SequencedImage

interface GardenAnalysisRepository {
    suspend fun analyze(images: List<SequencedImage>): Result<GardenAnalysisResult>
}
