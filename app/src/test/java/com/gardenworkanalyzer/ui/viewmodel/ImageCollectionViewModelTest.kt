package com.gardenworkanalyzer.ui.viewmodel

import android.net.Uri
import com.gardenworkanalyzer.data.repository.ImageRepository
import com.gardenworkanalyzer.domain.model.GardenImage
import com.gardenworkanalyzer.domain.model.MAX_IMAGE_COUNT
import com.gardenworkanalyzer.domain.usecase.ManageImageCollectionUseCase
import com.gardenworkanalyzer.domain.usecase.SequenceImagesUseCase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ImageCollectionViewModelTest : DescribeSpec({

    val testDispatcher = UnconfinedTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    describe("ImageCollectionViewModel") {
        val manageUseCase = mockk<ManageImageCollectionUseCase>(relaxed = true)
        val sequenceUseCase = mockk<SequenceImagesUseCase>(relaxed = true)
        val imageRepository = mockk<ImageRepository>()
        val imagesFlow = MutableStateFlow<List<GardenImage>>(emptyList())

        every { imageRepository.getImages() } returns imagesFlow

        val viewModel = ImageCollectionViewModel(manageUseCase, sequenceUseCase, imageRepository)

        describe("imageCollection") {
            it("starts with empty list") {
                viewModel.imageCollection.value shouldBe emptyList()
            }

            it("reflects repository updates") {
                val uri = mockk<Uri>()
                val image = GardenImage(uri, "image/jpeg", 1000L)

                // Need an active collector for WhileSubscribed to work
                val job = launch(testDispatcher) {
                    viewModel.imageCollection.collect {}
                }

                imagesFlow.value = listOf(image)
                viewModel.imageCollection.value shouldBe listOf(image)

                // Reset for other tests
                imagesFlow.value = emptyList()
                job.cancel()
            }
        }

        describe("imageCount") {
            it("starts at zero") {
                viewModel.imageCount.value shouldBe 0
            }

            it("reflects the number of images") {
                val images = (1..3).map {
                    GardenImage(mockk<Uri>(), "image/jpeg", it.toLong())
                }

                val job = launch(testDispatcher) {
                    viewModel.imageCount.collect {}
                }

                imagesFlow.value = images
                viewModel.imageCount.value shouldBe 3

                imagesFlow.value = emptyList()
                job.cancel()
            }
        }

        describe("addImages") {
            it("delegates to ManageImageCollectionUseCase") {
                val uris = listOf(mockk<Uri>())
                every { manageUseCase.addImages(uris) } returns Result.success(emptyList())

                viewModel.addImages(uris)

                verify { manageUseCase.addImages(uris) }
            }
        }

        describe("removeImage") {
            it("delegates to ManageImageCollectionUseCase") {
                every { manageUseCase.removeImage(2) } returns Result.success(emptyList())

                viewModel.removeImage(2)

                verify { manageUseCase.removeImage(2) }
            }
        }

        describe("reorderImages") {
            it("delegates to SequenceImagesUseCase") {
                every { sequenceUseCase.reorder(0, 2) } returns emptyList()

                viewModel.reorderImages(0, 2)

                verify { sequenceUseCase.reorder(0, 2) }
            }
        }

        describe("canAddImages") {
            it("returns true when collection has room") {
                every { manageUseCase.isCollectionFull(0, 5) } returns false

                viewModel.canAddImages(5) shouldBe true
            }

            it("returns false when collection would exceed max") {
                val images = (1..MAX_IMAGE_COUNT).map {
                    GardenImage(mockk<Uri>(), "image/jpeg", it.toLong())
                }

                val job = launch(testDispatcher) {
                    viewModel.imageCollection.collect {}
                }

                imagesFlow.value = images
                every { manageUseCase.isCollectionFull(MAX_IMAGE_COUNT, 1) } returns true

                viewModel.canAddImages(1) shouldBe false

                imagesFlow.value = emptyList()
                job.cancel()
            }

            it("allows adding exactly 10 images to an empty collection") {
                every { manageUseCase.isCollectionFull(0, MAX_IMAGE_COUNT) } returns false

                viewModel.canAddImages(MAX_IMAGE_COUNT) shouldBe true
            }

            it("rejects adding 11th image when collection is full") {
                val images = (1..MAX_IMAGE_COUNT).map {
                    GardenImage(mockk<Uri>(), "image/jpeg", it.toLong())
                }

                val job = launch(testDispatcher) {
                    viewModel.imageCollection.collect {}
                }

                imagesFlow.value = images
                every { manageUseCase.isCollectionFull(MAX_IMAGE_COUNT, 1) } returns true

                viewModel.canAddImages(1) shouldBe false

                imagesFlow.value = emptyList()
                job.cancel()
            }
        }

        describe("empty collection behavior") {
            it("has zero image count when collection is empty") {
                val job = launch(testDispatcher) {
                    viewModel.imageCount.collect {}
                }

                imagesFlow.value = emptyList()
                viewModel.imageCount.value shouldBe 0

                job.cancel()
            }

            it("empty collection means analyze should be disabled") {
                // When collection is empty, imageCount is 0.
                // The UI uses imageCount in [1..10] to enable the analyze button.
                // With 0 images, analyze should be disabled.
                val job = launch(testDispatcher) {
                    viewModel.imageCollection.collect {}
                }

                imagesFlow.value = emptyList()
                viewModel.imageCollection.value.size shouldBe 0
                viewModel.imageCount.value shouldBe 0

                job.cancel()
            }
        }
    }
})
