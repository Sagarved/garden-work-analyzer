package com.gardenworkanalyzer.domain.usecase

import android.net.Uri
import com.gardenworkanalyzer.data.repository.ImageRepository
import com.gardenworkanalyzer.domain.model.GardenImage
import com.gardenworkanalyzer.domain.model.SequencedImage
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

// Feature: garden-work-analyzer, Property 6: Default ordering matches insertion order

class SequencePropertyTest : FreeSpec({

    "Property 6 - default ordering matches insertion order" {
        // **Validates: Requirements 4.1**
        val uriArb: Arb<Uri> = Arb.int(1..10000).map { id -> mockk<Uri>(name = "uri-$id") }
        val uriListArb: Arb<List<Uri>> = Arb.list(uriArb, range = 1..10)

        checkAll(PropTestConfig(iterations = 100), uriListArb) { uris ->
            // In-memory collection preserving insertion order
            val inMemoryCollection = mutableListOf<GardenImage>()

            val imageRepository = mockk<ImageRepository>()

            val urisSlot = slot<List<Uri>>()
            every { imageRepository.addImages(capture(urisSlot)) } answers {
                val captured = urisSlot.captured
                val newImages = captured.map { uri ->
                    GardenImage(uri, "image/jpeg", System.nanoTime())
                }
                inMemoryCollection.addAll(newImages)
                Result.success(inMemoryCollection.toList())
            }

            every { imageRepository.getSequencedImages() } answers {
                inMemoryCollection.mapIndexed { idx, img ->
                    SequencedImage(img, idx + 1)
                }
            }

            // Add images sequentially (one at a time to test insertion order)
            for (uri in uris) {
                imageRepository.addImages(listOf(uri))
            }

            val useCase = SequenceImagesUseCase(imageRepository)

            // Finalize and verify ordering
            val result = useCase.finalizeSequence()

            result.size shouldBe uris.size

            // Each image should appear in the exact insertion order
            for (i in uris.indices) {
                result[i].image.uri shouldBe uris[i]
                result[i].sequenceIndex shouldBe (i + 1)
            }
        }
    }

    // Feature: garden-work-analyzer, Property 7: Reorder correctly moves elements
    "Property 7 - reorder correctly moves elements" {
        // **Validates: Requirements 4.2**
        checkAll(PropTestConfig(iterations = 100), Arb.int(2..10)) { n ->
            // Generate a collection of n images
            val images = (1..n).map { id ->
                GardenImage(mockk<Uri>(name = "uri-$id"), "image/jpeg", id.toLong())
            }

            // Generate random valid fromIndex and toIndex in [0, n)
            val fromIndex = (0 until n).random()
            val toIndex = (0 until n).random()

            // Simulate the reorder operation: remove from fromIndex, insert at toIndex
            val expectedList = images.toMutableList()
            val movedElement = expectedList.removeAt(fromIndex)
            expectedList.add(toIndex, movedElement)

            val imageRepository = mockk<ImageRepository>()
            every { imageRepository.reorder(fromIndex, toIndex) } returns expectedList.toList()

            val useCase = SequenceImagesUseCase(imageRepository)
            val result = useCase.reorder(fromIndex, toIndex)

            // Verify the collection size is unchanged
            result.size shouldBe n

            // Verify the element that was at fromIndex is now at toIndex
            result[toIndex] shouldBe images[fromIndex]

            // Verify no images were lost (all original images are still present)
            result.toSet() shouldBe images.toSet()
        }
    }

    // Feature: garden-work-analyzer, Property 8: Finalize produces sequential 1-based indices
    "Property 8 - finalize produces sequential 1-based indices" {
        // **Validates: Requirements 4.3, 4.4**
        checkAll(PropTestConfig(iterations = 100), Arb.int(1..10)) { n ->
            // Generate n random GardenImages
            val images = (1..n).map { id ->
                GardenImage(mockk<Uri>(name = "uri-$id"), "image/jpeg", id.toLong())
            }

            // Build expected SequencedImages with 1-based indices
            val expectedSequenced = images.mapIndexed { idx, img ->
                SequencedImage(img, idx + 1)
            }

            val imageRepository = mockk<ImageRepository>()
            every { imageRepository.getSequencedImages() } returns expectedSequenced

            val useCase = SequenceImagesUseCase(imageRepository)
            val result = useCase.finalizeSequence()

            // Verify the result has exactly n items
            result.size shouldBe n

            // Verify indices are exactly 1, 2, 3, ..., n
            val indices = result.map { it.sequenceIndex }
            indices shouldBe (1..n).toList()

            // Verify each SequencedImage references the correct original GardenImage
            for (i in images.indices) {
                result[i].image shouldBe images[i]
                result[i].sequenceIndex shouldBe (i + 1)
            }
        }
    }
})
