package com.gardenworkanalyzer.data.repository

import android.net.Uri
import app.cash.turbine.test
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk

class ImageRepositoryImplTest : DescribeSpec({

    lateinit var repo: ImageRepositoryImpl

    beforeEach {
        repo = ImageRepositoryImpl()
    }

    describe("getImages") {
        it("returns empty list initially") {
            repo.getImages().test {
                awaitItem().shouldBeEmpty()
                cancelAndConsumeRemainingEvents()
            }
        }

        it("emits updates reactively") {
            val uri1 = mockk<Uri>()
            val uri2 = mockk<Uri>()

            repo.getImages().test {
                awaitItem().shouldBeEmpty()

                repo.addImages(listOf(uri1))
                awaitItem() shouldHaveSize 1

                repo.addImages(listOf(uri2))
                awaitItem() shouldHaveSize 2

                repo.removeImage(0)
                awaitItem() shouldHaveSize 1

                repo.clear()
                awaitItem().shouldBeEmpty()

                cancelAndConsumeRemainingEvents()
            }
        }
    }

    describe("addImages") {
        it("adds images and returns updated list") {
            val uri1 = mockk<Uri>()
            val uri2 = mockk<Uri>()

            val result = repo.addImages(listOf(uri1, uri2))

            result.isSuccess shouldBe true
            val images = result.getOrThrow()
            images shouldHaveSize 2
            images[0].uri shouldBe uri1
            images[1].uri shouldBe uri2
            images[0].mimeType shouldBe "image/jpeg"
        }

        it("accumulates across multiple calls") {
            val uri1 = mockk<Uri>()
            val uri2 = mockk<Uri>()

            repo.addImages(listOf(uri1))
            repo.addImages(listOf(uri2))

            repo.getImages().test {
                awaitItem() shouldHaveSize 2
                cancelAndConsumeRemainingEvents()
            }
        }
    }

    describe("removeImage") {
        it("removes at valid index") {
            val uri1 = mockk<Uri>()
            val uri2 = mockk<Uri>()
            repo.addImages(listOf(uri1, uri2))

            val result = repo.removeImage(0)

            result.isSuccess shouldBe true
            val images = result.getOrThrow()
            images shouldHaveSize 1
            images[0].uri shouldBe uri2
        }

        it("fails for out-of-bounds index on empty collection") {
            val result = repo.removeImage(0)

            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<IndexOutOfBoundsException>()
        }

        it("fails for negative index") {
            repo.addImages(listOf(mockk<Uri>()))
            val result = repo.removeImage(-1)

            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<IndexOutOfBoundsException>()
        }
    }

    describe("reorder") {
        it("moves element from fromIndex to toIndex") {
            val uri1 = mockk<Uri>()
            val uri2 = mockk<Uri>()
            val uri3 = mockk<Uri>()
            repo.addImages(listOf(uri1, uri2, uri3))

            val result = repo.reorder(0, 2)

            result shouldHaveSize 3
            result[0].uri shouldBe uri2
            result[1].uri shouldBe uri3
            result[2].uri shouldBe uri1
        }
    }

    describe("getSequencedImages") {
        it("returns 1-based indices") {
            val uri1 = mockk<Uri>()
            val uri2 = mockk<Uri>()
            repo.addImages(listOf(uri1, uri2))

            val sequenced = repo.getSequencedImages()

            sequenced shouldHaveSize 2
            sequenced[0].sequenceIndex shouldBe 1
            sequenced[0].image.uri shouldBe uri1
            sequenced[1].sequenceIndex shouldBe 2
            sequenced[1].image.uri shouldBe uri2
        }

        it("returns empty list when no images") {
            repo.getSequencedImages().shouldBeEmpty()
        }
    }

    describe("clear") {
        it("empties the collection") {
            repo.addImages(listOf(mockk<Uri>()))
            repo.clear()

            repo.getImages().test {
                awaitItem().shouldBeEmpty()
                cancelAndConsumeRemainingEvents()
            }
        }
    }
})
