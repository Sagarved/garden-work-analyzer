package com.gardenworkanalyzer.data.repository

import com.gardenworkanalyzer.data.compressor.ImageCompressor
import com.gardenworkanalyzer.data.model.GardenWorkMapper
import com.gardenworkanalyzer.data.network.HuggingFaceApiService
import com.gardenworkanalyzer.data.network.HuggingFaceClassification
import com.gardenworkanalyzer.domain.model.GardenAnalysisResult
import com.gardenworkanalyzer.domain.model.SequencedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.logging.Level
import java.util.logging.Logger

class GardenAnalysisRepositoryImpl(
    private val huggingFaceApiService: HuggingFaceApiService,
    private val imageCompressor: ImageCompressor,
    private val apiToken: String
) : GardenAnalysisRepository {

    private val logger = Logger.getLogger(TAG)

    companion object {
        private const val TAG = "GardenAnalysisRepo"
    }

    override suspend fun analyze(images: List<SequencedImage>): Result<GardenAnalysisResult> =
        withContext(Dispatchers.IO) {
            try {
                // Classify each image and aggregate results
                val allClassifications = mutableListOf<HuggingFaceClassification>()

                for (sequencedImage in images) {
                    val compressed = imageCompressor.compress(sequencedImage.image.uri)
                    val requestBody = compressed.toRequestBody("application/octet-stream".toMediaType())

                    val response = huggingFaceApiService.classifyImage(
                        authorization = "Bearer $apiToken",
                        imageData = requestBody
                    )

                    if (response.isSuccessful) {
                        response.body()?.let { allClassifications.addAll(it) }
                    } else {
                        logger.log(Level.WARNING, "HF API error for image ${sequencedImage.sequenceIndex}: ${response.code()}")
                    }
                }

                if (allClassifications.isEmpty()) {
                    return@withContext Result.success(
                        GardenAnalysisResult(
                            suggestions = emptyList(),
                            gardenContentDetected = false
                        )
                    )
                }

                val gardenDetected = GardenWorkMapper.isGardenRelated(allClassifications)
                val suggestions = GardenWorkMapper.mapClassificationsToSuggestions(allClassifications)

                Result.success(
                    GardenAnalysisResult(
                        suggestions = suggestions,
                        gardenContentDetected = gardenDetected || suggestions.isNotEmpty()
                    )
                )
            } catch (e: UnknownHostException) {
                logger.log(Level.SEVERE, "Service unreachable", e)
                Result.failure(IOException("Service temporarily unavailable", e))
            } catch (e: ConnectException) {
                logger.log(Level.SEVERE, "Service unreachable", e)
                Result.failure(IOException("Service temporarily unavailable", e))
            } catch (e: IOException) {
                logger.log(Level.SEVERE, "Network error during analysis", e)
                Result.failure(e)
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Unexpected error during analysis", e)
                Result.failure(e)
            }
        }
}
