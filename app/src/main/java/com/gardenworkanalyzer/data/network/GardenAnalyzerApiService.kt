package com.gardenworkanalyzer.data.network

import com.gardenworkanalyzer.data.model.GardenAnalysisResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface GardenAnalyzerApiService {
    @Multipart
    @POST("/analyze")
    suspend fun analyzeImages(
        @Part images: List<MultipartBody.Part>,
        @Part("sequence") sequenceMetadata: RequestBody
    ): Response<GardenAnalysisResponse>
}
