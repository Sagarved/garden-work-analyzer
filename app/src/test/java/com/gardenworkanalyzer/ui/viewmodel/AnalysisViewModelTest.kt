package com.gardenworkanalyzer.ui.viewmodel

import android.net.Uri
import com.gardenworkanalyzer.data.repository.GardenAnalysisRepository
import com.gardenworkanalyzer.domain.model.AnalysisUiState
import com.gardenworkanalyzer.domain.model.GardenAnalysisResult
import com.gardenworkanalyzer.domain.model.GardenImage
import com.gardenworkanalyzer.domain.model.GardenWorkSuggestion
import com.gardenworkanalyzer.domain.model.GardenWorkType
import com.gardenworkanalyzer.domain.model.MAX_RETRY_ATTEMPTS
import com.gardenworkanalyzer.domain.model.SequencedImage
import com.gardenworkanalyzer.domain.usecase.SequenceImagesUseCase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModelTest : DescribeSpec({

    val testDispatcher = UnconfinedTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    describe("AnalysisViewModel") {

        describe("initial state") {
            it("starts as Idle") {
                val sequenceUseCase = mockk<SequenceImagesUseCase>()
                val repository = mockk<GardenAnalysisRepository>()
                val viewModel = AnalysisViewModel(sequenceUseCase, repository)

                viewModel.analysisState.value shouldBe AnalysisUiState.Idle
            }
        }

        describe("analyze") {
            it("sets Success state on successful analysis") {
                val sequenceUseCase = mockk<SequenceImagesUseCase>()
                val repository = mockk<GardenAnalysisRepository>()
                val uri = mockk<Uri>()
                val images = listOf(
                    SequencedImage(GardenImage(uri, "image/jpeg", 1000L), 1)
                )
                val suggestions = listOf(
                    GardenWorkSuggestion(GardenWorkType.PRUNING, "Prune roses", 0.9, "Cut at 45 degrees")
                )
                val result = GardenAnalysisResult(suggestions, true)

                every { sequenceUseCase.finalizeSequence() } returns images
                coEvery { repository.analyze(images) } returns Result.success(result)

                val viewModel = AnalysisViewModel(sequenceUseCase, repository)
                viewModel.analyze()

                val state = viewModel.analysisState.value
                state.shouldBeInstanceOf<AnalysisUiState.Success>()
                state.result shouldBe result
            }

            it("sets Error state on network failure") {
                val sequenceUseCase = mockk<SequenceImagesUseCase>()
                val repository = mockk<GardenAnalysisRepository>()
                val uri = mockk<Uri>()
                val images = listOf(
                    SequencedImage(GardenImage(uri, "image/jpeg", 1000L), 1)
                )

                every { sequenceUseCase.finalizeSequence() } returns images
                coEvery { repository.analyze(images) } returns Result.failure(IOException("Network error"))

                val viewModel = AnalysisViewModel(sequenceUseCase, repository)
                viewModel.analyze()

                val state = viewModel.analysisState.value
                state.shouldBeInstanceOf<AnalysisUiState.Error>()
                state.message shouldBe "Network error"
                state.retryCount shouldBe 0
            }

            it("handles no garden content detected") {
                val sequenceUseCase = mockk<SequenceImagesUseCase>()
                val repository = mockk<GardenAnalysisRepository>()
                val uri = mockk<Uri>()
                val images = listOf(
                    SequencedImage(GardenImage(uri, "image/jpeg", 1000L), 1)
                )
                val result = GardenAnalysisResult(emptyList(), false)

                every { sequenceUseCase.finalizeSequence() } returns images
                coEvery { repository.analyze(images) } returns Result.success(result)

                val viewModel = AnalysisViewModel(sequenceUseCase, repository)
                viewModel.analyze()

                val state = viewModel.analysisState.value
                state.shouldBeInstanceOf<AnalysisUiState.Success>()
                state.result.gardenContentDetected shouldBe false
                state.result.suggestions shouldBe emptyList()
            }
        }

        describe("retry") {
            it("retries analysis and increments retry count") {
                val sequenceUseCase = mockk<SequenceImagesUseCase>()
                val repository = mockk<GardenAnalysisRepository>()
                val uri = mockk<Uri>()
                val images = listOf(
                    SequencedImage(GardenImage(uri, "image/jpeg", 1000L), 1)
                )

                every { sequenceUseCase.finalizeSequence() } returns images
                coEvery { repository.analyze(images) } returns Result.failure(IOException("Network error"))

                val viewModel = AnalysisViewModel(sequenceUseCase, repository)
                viewModel.analyze()
                viewModel.retry()

                val state = viewModel.analysisState.value
                state.shouldBeInstanceOf<AnalysisUiState.Error>()
                state.retryCount shouldBe 1
            }

            it("shows network check message after max retries exhausted") {
                val sequenceUseCase = mockk<SequenceImagesUseCase>()
                val repository = mockk<GardenAnalysisRepository>()
                val uri = mockk<Uri>()
                val images = listOf(
                    SequencedImage(GardenImage(uri, "image/jpeg", 1000L), 1)
                )

                every { sequenceUseCase.finalizeSequence() } returns images
                coEvery { repository.analyze(images) } returns Result.failure(IOException("Network error"))

                val viewModel = AnalysisViewModel(sequenceUseCase, repository)
                viewModel.analyze()

                // Exhaust all retries
                repeat(MAX_RETRY_ATTEMPTS) {
                    viewModel.retry()
                }

                val state = viewModel.analysisState.value
                state.shouldBeInstanceOf<AnalysisUiState.Error>()
                state.message shouldBe "Check your network connection"
                state.retryCount shouldBe MAX_RETRY_ATTEMPTS
            }

            it("resets retry count on successful analysis") {
                val sequenceUseCase = mockk<SequenceImagesUseCase>()
                val repository = mockk<GardenAnalysisRepository>()
                val uri = mockk<Uri>()
                val images = listOf(
                    SequencedImage(GardenImage(uri, "image/jpeg", 1000L), 1)
                )
                val result = GardenAnalysisResult(
                    listOf(GardenWorkSuggestion(GardenWorkType.WATERING, "Water plants", 0.8, "Daily")),
                    true
                )

                every { sequenceUseCase.finalizeSequence() } returns images
                // First call fails, second succeeds
                coEvery { repository.analyze(images) } returns Result.failure(IOException("Network error")) andThen Result.success(result)

                val viewModel = AnalysisViewModel(sequenceUseCase, repository)
                viewModel.analyze() // fails
                viewModel.retry()   // succeeds

                val state = viewModel.analysisState.value
                state.shouldBeInstanceOf<AnalysisUiState.Success>()
            }
        }
    }
})
