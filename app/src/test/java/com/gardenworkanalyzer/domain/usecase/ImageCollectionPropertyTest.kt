package com.gardenworkanalyzer.domain.usecase

import android.content.ContentResolver
import android.net.Uri
import com.gardenworkanalyzer.data.repository.ImageRepository
import com.gardenworkanalyzer.domain.model.GardenImage
import com.gardenworkanalyzer.domain.model.SUPPORTED_MIME_TYPES
import com.gardenworkanalyzer.domain.model.SequencedImage
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

// Feature: garden-work-analyzer, Property 1: Adding images grows the collection

class ImageCollectionPropertyTest : FreeSpec({

    "Property 1 - adding images grows the collection" {
        // **Validates: Requirements 1.3, 2.2**
        checkAll(PropTestConfig(iterations = 100), Arb.int(0..9)) { n ->
            // Determine k: number of images to add (at least 1, at most 10 - n)
            val maxK = 10 - n
            if (maxK < 1) return@checkAll

            val kArb = Arb.int(1..maxK)
            checkAll(PropTestConfig(iterations = 5), kArb) { k ->
                // In-memory collection to simulate repository state
                val inMemoryCollection = mutableListOf<GardenImage>()

                // Pre-populate with n existing images
                for (i in 0 until n) {
                    val uri = mockk<Uri>(name = "existing-$i")
                    inMemoryCollection.add(
                        GardenImage(uri, "image/jpeg", System.currentTimeMillis() + i)
                    )
                }

                // Generate k new URIs to add
                val newUris = (0 until k).map { mockk<Uri>(name = "new-$it") }

                // Mock ImageRepository
                val imageRepository = mockk<ImageRepository>()
                every { imageRepository.getSequencedImages() } answers {
                    inMemoryCollection.mapIndexed { idx, img ->
                        SequencedImage(img, idx + 1)
                    }
                }

                val urisSlot = slot<List<Uri>>()
                every { imageRepository.addImages(capture(urisSlot)) } answers {
                    val urisToAdd = urisSlot.captured
                    val newImages = urisToAdd.map { uri ->
                        GardenImage(uri, "image/jpeg", System.currentTimeMillis())
                    }
                    inMemoryCollection.addAll(newImages)
                    Result.success(inMemoryCollection.toList())
                }

                // Mock ContentResolver to return valid MIME type for all new URIs
                val contentResolver = mockk<ContentResolver>()
                for (uri in newUris) {
                    every { contentResolver.getType(uri) } returns "image/jpeg"
                }

                val useCase = ManageImageCollectionUseCase(imageRepository, contentResolver)

                // Act
                val result = useCase.addImages(newUris)

                // Assert: result is success
                result.isSuccess shouldBe true

                val resultImages = result.getOrThrow()

                // Collection size should be n + k
                resultImages.size shouldBe (n + k)

                // All added URIs should be present in the collection
                val resultUris = resultImages.map { it.uri }
                resultUris shouldContainAll newUris
            }
        }
    }

    // Feature: garden-work-analyzer, Property 3: Removing an image shrinks the collection
    "Property 3 - removing an image shrinks the collection" {
        // **Validates: Requirements 3.3**
        checkAll(PropTestConfig(iterations = 100), Arb.int(1..10)) { n ->
            // Generate a random valid index in [0, n-1]
            checkAll(PropTestConfig(iterations = 5), Arb.int(0 until n)) { removeIndex ->
                // In-memory collection to simulate repository state
                val inMemoryCollection = mutableListOf<GardenImage>()

                // Pre-populate with n images
                val uris = (0 until n).map { mockk<Uri>(name = "img-$it") }
                for (i in 0 until n) {
                    inMemoryCollection.add(
                        GardenImage(uris[i], "image/jpeg", System.currentTimeMillis() + i)
                    )
                }

                // Capture the image that will be removed
                val removedImage = inMemoryCollection[removeIndex]

                // Mock ImageRepository
                val imageRepository = mockk<ImageRepository>()
                every { imageRepository.getSequencedImages() } answers {
                    inMemoryCollection.mapIndexed { idx, img ->
                        SequencedImage(img, idx + 1)
                    }
                }

                every { imageRepository.removeImage(removeIndex) } answers {
                    inMemoryCollection.removeAt(removeIndex)
                    Result.success(inMemoryCollection.toList())
                }

                // ContentResolver is not used by removeImage, but required by constructor
                val contentResolver = mockk<ContentResolver>()

                val useCase = ManageImageCollectionUseCase(imageRepository, contentResolver)

                // Act
                val result = useCase.removeImage(removeIndex)

                // Assert: result is success
                result.isSuccess shouldBe true

                val resultImages = result.getOrThrow()

                // Collection size should be n - 1
                resultImages.size shouldBe (n - 1)

                // The removed image should no longer be present
                resultImages shouldNotContain removedImage
            }
        }
    }

    // Feature: garden-work-analyzer, Property 4: Collection size invariant
    "Property 4 - collection size invariant" {
        // **Validates: Requirements 3.5, 3.6**

        // Operation encoded as Pair<String, Int>: ("add", count) or ("remove", 0)
        val opsArb: Arb<List<Pair<String, Int>>> = Arb.list(
            Arb.element(listOf("add", "remove")).map { opType ->
                if (opType == "add") Pair("add", (1..3).random())
                else Pair("remove", 0)
            },
            range = 1..30
        )

        checkAll(PropTestConfig(iterations = 100), opsArb) { ops ->
            // In-memory collection to simulate repository state
            val inMemoryCollection = mutableListOf<GardenImage>()
            var uriCounter = 0

            // Mock ImageRepository
            val imageRepository = mockk<ImageRepository>()
            every { imageRepository.getSequencedImages() } answers {
                inMemoryCollection.mapIndexed { idx, img ->
                    SequencedImage(img, idx + 1)
                }
            }

            val urisSlot = slot<List<Uri>>()
            every { imageRepository.addImages(capture(urisSlot)) } answers {
                val urisToAdd = urisSlot.captured
                val newImages = urisToAdd.map { uri ->
                    GardenImage(uri, "image/jpeg", System.currentTimeMillis())
                }
                inMemoryCollection.addAll(newImages)
                Result.success(inMemoryCollection.toList())
            }

            val indexSlot = slot<Int>()
            every { imageRepository.removeImage(capture(indexSlot)) } answers {
                val idx = indexSlot.captured
                inMemoryCollection.removeAt(idx)
                Result.success(inMemoryCollection.toList())
            }

            // Mock ContentResolver
            val contentResolver = mockk<ContentResolver>()
            every { contentResolver.getType(any()) } returns "image/jpeg"

            val useCase = ManageImageCollectionUseCase(imageRepository, contentResolver)

            for (op in ops) {
                val sizeBefore = inMemoryCollection.size

                when (op.first) {
                    "add" -> {
                        val addCount = op.second
                        val newUris = (0 until addCount).map {
                            mockk<Uri>(name = "uri-${uriCounter++}")
                        }
                        val result = useCase.addImages(newUris)

                        if (sizeBefore + addCount > 10) {
                            // Add should be rejected — collection unchanged
                            result.isFailure shouldBe true
                            inMemoryCollection.size shouldBe sizeBefore
                        } else {
                            result.isSuccess shouldBe true
                        }
                    }
                    "remove" -> {
                        if (sizeBefore > 0) {
                            val removeIndex = (0 until sizeBefore).random()
                            val result = useCase.removeImage(removeIndex)
                            result.isSuccess shouldBe true
                        }
                        // If empty, skip remove (nothing to remove)
                    }
                }

                // Invariant: collection size is always in [0, 10]
                inMemoryCollection.size shouldBeInRange (0..10)
            }
        }
    }
})
