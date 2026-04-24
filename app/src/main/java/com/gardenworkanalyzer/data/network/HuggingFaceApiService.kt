package com.gardenworkanalyzer.data.network

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * HuggingFace Inference API service for image classification.
 * Uses google/vit-base-patch16-224 model for plant/garden image analysis.
 */
interface HuggingFaceApiService {
    @POST("models/google/vit-base-patch16-224")
    suspend fun classifyImage(
        @Header("Authorization") authorization: String,
        @Body imageData: RequestBody
    ): Response<List<HuggingFaceClassification>>
}

data class HuggingFaceClassification(
    val label: String,
    val score: Double
)
