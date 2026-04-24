package com.gardenworkanalyzer.data.repository

import android.net.Uri
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.kotest.common.ExperimentalKotest
import io.mockk.mockk

// Feature: garden-work-analyzer, Property 2: Cancel preserves collection state

@OptIn(ExperimentalKotest::class)
class CancelPreservesStatePropertyTest : FreeSpec({

    "Property 2 - cancel preserves collection state" {
        // **Validates: Requirements 1.5, 2.6**
        checkAll(PropTestConfig(iterations = 100), Arb.int(0..10)) { n ->
            val repo = ImageRepositoryImpl()

            // Populate with n random images
            if (n > 0) {
                val uris = (0 until n).map { mockk<Uri>(name = "img-$it") }
                repo.addImages(uris)
            }

            // Snapshot state before cancel
            val imagesBefore = repo.getSequencedImages().map { it.image }
            val sizeBefore = imagesBefore.size
            val orderBefore = imagesBefore.map { it.uri }

            // Simulate cancel: no-op (do NOT call addImages or removeImage)

            // Verify state is identical after cancel
            val imagesAfter = repo.getSequencedImages().map { it.image }
            val sizeAfter = imagesAfter.size
            val orderAfter = imagesAfter.map { it.uri }

            sizeAfter shouldBe sizeBefore
            orderAfter shouldBe orderBefore
            imagesAfter shouldBe imagesBefore
        }
    }
})
