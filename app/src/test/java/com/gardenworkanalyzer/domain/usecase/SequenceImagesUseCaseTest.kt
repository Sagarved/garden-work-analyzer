package com.gardenworkanalyzer.domain.usecase

import android.net.Uri
import com.gardenworkanalyzer.data.repository.ImageRepository
import com.gardenworkanalyzer.domain.model.GardenImage
import com.gardenworkanalyzer.domain.model.SequencedImage
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class SequenceImagesUseCaseTest : DescribeSpec({

    val imageRepository = mockk<ImageRepository>()
    val useCase = SequenceImagesUseCase(imageRepository)

    describe("reorder") {
        it("delegates to imageRepository.reorder and returns result") {
            val uri1 = mockk<Uri>()
            val uri2 = mockk<Uri>()
            val uri3 = mockk<Uri>()
            val img1 = GardenImage(uri1, "image/jpeg", 1L)
            val img2 = GardenImage(uri2, "image/png", 2L)
            val img3 = GardenImage(uri3, "image/webp", 3L)
            val reorderedList = listOf(img2, img1, img3)

            every { imageRepository.reorder(0, 1) } returns reorderedList

            val result = useCase.reorder(0, 1)

            result shouldBe reorderedList
            result shouldHaveSize 3
            verify { imageRepository.reorder(0, 1) }
        }

        it("handles moving last element to first position") {
            val uri1 = mockk<Uri>()
            val uri2 = mockk<Uri>()
            val img1 = GardenImage(uri1, "image/jpeg", 1L)
            val img2 = GardenImage(uri2, "image/png", 2L)
            val reorderedList = listOf(img2, img1)

            every { imageRepository.reorder(1, 0) } returns reorderedList

            val result = useCase.reorder(1, 0)

            result shouldBe reorderedList
            verify { imageRepository.reorder(1, 0) }
        }
    }

    describe("finalizeSequence") {
        it("returns sequenced images from repository") {
            val uri1 = mockk<Uri>()
            val uri2 = mockk<Uri>()
            val img1 = GardenImage(uri1, "image/jpeg", 1L)
            val img2 = GardenImage(uri2, "image/png", 2L)
            val sequenced = listOf(
                SequencedImage(img1, 1),
                SequencedImage(img2, 2)
            )

            every { imageRepository.getSequencedImages() } returns sequenced

            val result = useCase.finalizeSequence()

            result shouldHaveSize 2
            result[0].sequenceIndex shouldBe 1
            result[1].sequenceIndex shouldBe 2
            result[0].image shouldBe img1
            result[1].image shouldBe img2
            verify { imageRepository.getSequencedImages() }
        }

        it("returns empty list when no images in repository") {
            every { imageRepository.getSequencedImages() } returns emptyList()

            val result = useCase.finalizeSequence()

            result shouldHaveSize 0
            verify { imageRepository.getSequencedImages() }
        }
    }
})
