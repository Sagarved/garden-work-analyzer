package com.gardenworkanalyzer.data.repository

import android.net.Uri
import com.gardenworkanalyzer.data.compressor.ImageCompressor
import com.gardenworkanalyzer.data.model.GardenAnalysisResponse
import com.gardenworkanalyzer.data.model.GardenWorkSuggestionDto
import com.gardenworkanalyzer.data.network.GardenAnalyzerApiService
import com.gardenworkanalyzer.domain.model.GardenImage
import com.gardenworkanalyzer.domain.model.GardenWorkType
import com.gardenworkanalyzer.domain.model.SequencedImage
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

class GardenAnalysisRepositoryImplTest : DescribeSpec({

    fun makeImages(count: Int): List<SequencedImage> = (1..count).map { i ->
        SequencedImage(
            image = GardenImage(
                uri = mockk<Uri>(name = "uri-$i"),
                mimeType = "image/jpeg",
                addedTimestamp = System.currentTimeMillis()
            ),
            sequenceIndex = i
        )
    }

    fun createRepo(): Triple<GardenAnalyzerApiService, ImageCompressor, GardenAnalysisRepositoryImpl> {
        val apiService = mockk<GardenAnalyzerApiService>()
        val imageCompressor = mockk<ImageCompressor>()
        val repository = GardenAnalysisRepositoryImpl(apiService, imageCompressor)
        coEvery { imageCompressor.compress(any()) } returns ByteArray(100)
        return Triple(apiService, imageCompressor, repository)
    }

    describe("analyze - successful response") {
        it("maps suggestions from API response to domain models") {
            val (apiService, _, repository) = createRepo()
            val dto = GardenWorkSuggestionDto(
                type = "PRUNING",
                description = "Trim the hedges",
                confidence = 0.85,
                detailedGuidance = "Use sharp shears"
            )
            val apiResponse = GardenAnalysisResponse(
                suggestions = listOf(dto),
                gardenContentDetected = true
            )
            coEvery {
                apiService.analyzeImages(any<List<MultipartBody.Part>>(), any<RequestBody>())
            } returns Response.success(apiResponse)

            val result = repository.analyze(makeImages(1))

            result.isSuccess shouldBe true
            val analysisResult = result.getOrThrow()
            analysisResult.gardenContentDetected shouldBe true
            analysisResult.suggestions.size shouldBe 1
            analysisResult.suggestions[0].type shouldBe GardenWorkType.PRUNING
            analysisResult.suggestions[0].description shouldBe "Trim the hedges"
            analysisResult.suggestions[0].confidence shouldBe 0.85
        }

        it("returns empty suggestions when no garden content detected") {
            val (apiService, _, repository) = createRepo()
            val apiResponse = GardenAnalysisResponse(
                suggestions = emptyList(),
                gardenContentDetected = false
            )
            coEvery {
                apiService.analyzeImages(any<List<MultipartBody.Part>>(), any<RequestBody>())
            } returns Response.success(apiResponse)

            val result = repository.analyze(makeImages(1))

            result.isSuccess shouldBe true
            val analysisResult = result.getOrThrow()
            analysisResult.gardenContentDetected shouldBe false
            analysisResult.suggestions shouldBe emptyList()
        }
    }

    describe("analyze - error handling") {
        it("returns failure on UnknownHostException (unreachable endpoint)") {
            val (apiService, _, repository) = createRepo()
            coEvery {
                apiService.analyzeImages(any<List<MultipartBody.Part>>(), any<RequestBody>())
            } throws UnknownHostException("no such host")

            val result = repository.analyze(makeImages(1))

            result.isFailure shouldBe true
            val exception = result.exceptionOrNull()
            exception.shouldBeInstanceOf<IOException>()
            exception!!.message shouldBe "Service temporarily unavailable"
        }

        it("returns failure on ConnectException (unreachable endpoint)") {
            val (apiService, _, repository) = createRepo()
            coEvery {
                apiService.analyzeImages(any<List<MultipartBody.Part>>(), any<RequestBody>())
            } throws ConnectException("Connection refused")

            val result = repository.analyze(makeImages(1))

            result.isFailure shouldBe true
            val exception = result.exceptionOrNull()
            exception.shouldBeInstanceOf<IOException>()
            exception!!.message shouldBe "Service temporarily unavailable"
        }

        it("returns failure on generic IOException (network error)") {
            val (apiService, _, repository) = createRepo()
            coEvery {
                apiService.analyzeImages(any<List<MultipartBody.Part>>(), any<RequestBody>())
            } throws IOException("Network failure")

            val result = repository.analyze(makeImages(1))

            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<IOException>()
        }

        it("returns failure on non-successful HTTP response") {
            val (apiService, _, repository) = createRepo()
            coEvery {
                apiService.analyzeImages(any<List<MultipartBody.Part>>(), any<RequestBody>())
            } returns Response.error(500, "Server Error".toResponseBody(null))

            val result = repository.analyze(makeImages(1))

            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<IOException>()
        }
    }
})
