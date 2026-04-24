package com.gardenworkanalyzer.domain.usecase

import android.content.ContentResolver
import android.net.Uri
import com.gardenworkanalyzer.data.repository.ImageRepository
import com.gardenworkanalyzer.domain.model.GardenImage
import com.gardenworkanalyzer.domain.model.MAX_IMAGE_COUNT
import com.gardenworkanalyzer.domain.model.SequencedImage
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ManageImageCollectionUseCaseTest : DescribeSpec({

    val imageRepository = mockk<ImageRepository>()
    val contentResolver = mockk<ContentResolver>()
    val useCase = ManageImageCollectionUseCase(imageRepository, contentResolver)

    describe("isCollectionFull") {
        it("returns false when adding images within limit") {
            useCase.isCollectionFull(5, 3).shouldBeFalse()
        }

        it("returns false when adding images exactly at limit") {
            useCase.isCollectionFull(7, 3).shouldBeFalse()
        }

        it("returns true when adding images exceeds limit") {
            useCase.isCollectionFull(8, 3).shouldBeTrue()
        }

        it("returns true when collection is already full") {
            useCase.isCollectionFull(MAX_IMAGE_COUNT, 1).shouldBeTrue()
        }

        it("returns false when adding zero images to full collection") {
            useCase.isCollectionFull(MAX_IMAGE_COUNT, 0).shouldBeFalse()
        }
    }

    describe("validateFormat") {
        it("returns true for supported JPEG MIME type") {
            val uri = mockk<Uri>()
            every { contentResolver.getType(uri) } returns "image/jpeg"
            useCase.validateFormat(uri).shouldBeTrue()
        }

        it("returns true for supported PNG MIME type") {
            val uri = mockk<Uri>()
            every { contentResolver.getType(uri) } returns "image/png"
            useCase.validateFormat(uri).shouldBeTrue()
        }

        it("returns true for supported WebP MIME type") {
            val uri = mockk<Uri>()
            every { contentResolver.getType(uri) } returns "image/webp"
            useCase.validateFormat(uri).shouldBeTrue()
        }

        it("returns false for unsupported MIME type") {
            val uri = mockk<Uri>()
            every { contentResolver.getType(uri) } returns "image/gif"
            useCase.validateFormat(uri).shouldBeFalse()
        }

        it("returns false when MIME type is null") {
            val uri = mockk<Uri>()
            every { contentResolver.getType(uri) } returns null
            useCase.validateFormat(uri).shouldBeFalse()
        }
    }

    describe("addImages") {
        it("returns failure when uri list is empty") {
            val result = useCase.addImages(emptyList())
            result.isFailure.shouldBeTrue()
        }

        it("returns failure when collection would exceed max") {
            val uri = mockk<Uri>()
            every { imageRepository.getSequencedImages() } returns List(MAX_IMAGE_COUNT) {
                SequencedImage(
                    GardenImage(uri, "image/jpeg", System.currentTimeMillis()),
                    it + 1
                )
            }

            val result = useCase.addImages(listOf(uri))
            result.isFailure.shouldBeTrue()
        }

        it("returns failure when image has unsupported format") {
            val uri = mockk<Uri>()
            every { imageRepository.getSequencedImages() } returns emptyList()
            every { contentResolver.getType(uri) } returns "image/gif"

            val result = useCase.addImages(listOf(uri))
            result.isFailure.shouldBeTrue()
        }

        it("delegates to repository when all validations pass") {
            val uri = mockk<Uri>()
            val expectedImages = listOf(GardenImage(uri, "image/jpeg", System.currentTimeMillis()))
            every { imageRepository.getSequencedImages() } returns emptyList()
            every { contentResolver.getType(uri) } returns "image/jpeg"
            every { imageRepository.addImages(listOf(uri)) } returns Result.success(expectedImages)

            val result = useCase.addImages(listOf(uri))
            result.isSuccess.shouldBeTrue()
            result.getOrNull()?.size shouldBe 1
            verify { imageRepository.addImages(listOf(uri)) }
        }
    }

    describe("removeImage") {
        it("returns failure for negative index") {
            every { imageRepository.getSequencedImages() } returns listOf(
                SequencedImage(
                    GardenImage(mockk(), "image/jpeg", System.currentTimeMillis()),
                    1
                )
            )

            val result = useCase.removeImage(-1)
            result.isFailure.shouldBeTrue()
        }

        it("returns failure for index out of bounds") {
            every { imageRepository.getSequencedImages() } returns listOf(
                SequencedImage(
                    GardenImage(mockk(), "image/jpeg", System.currentTimeMillis()),
                    1
                )
            )

            val result = useCase.removeImage(1)
            result.isFailure.shouldBeTrue()
        }

        it("delegates to repository for valid index") {
            val uri = mockk<Uri>()
            val image = GardenImage(uri, "image/jpeg", System.currentTimeMillis())
            every { imageRepository.getSequencedImages() } returns listOf(
                SequencedImage(image, 1)
            )
            every { imageRepository.removeImage(0) } returns Result.success(emptyList())

            val result = useCase.removeImage(0)
            result.isSuccess.shouldBeTrue()
            verify { imageRepository.removeImage(0) }
        }
    }
})
