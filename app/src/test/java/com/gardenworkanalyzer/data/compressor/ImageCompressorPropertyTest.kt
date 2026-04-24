package com.gardenworkanalyzer.data.compressor

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.gardenworkanalyzer.domain.model.MAX_IMAGE_SIZE_BYTES
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

// Feature: garden-work-analyzer, Property 9: Compression output is within size limit

@OptIn(ExperimentalKotest::class)
class ImageCompressorPropertyTest : FreeSpec({

    "Property 9 - compression output is within size limit" {
        // **Validates: Requirements 5.3**
        // For any image (regardless of original size), after compression
        // the output byte array should be at most 2,097,152 bytes (2 MB).
        //
        // We mock BitmapFactory.decodeStream and Bitmap.compress to simulate
        // images of varying sizes. The mock compress() produces output
        // proportional to quality, modeling realistic JPEG behavior.
        // This validates the compressor's iterative quality reduction
        // always brings output within the 2 MB limit.

        mockkStatic(BitmapFactory::class)

        val contentResolver = mockk<ContentResolver>()
        val uri = mockk<Uri>()
        val bitmap = mockk<Bitmap>(relaxed = true)

        every { contentResolver.openInputStream(uri) } returns
            ByteArrayInputStream(ByteArray(1))
        every { BitmapFactory.decodeStream(any<InputStream>()) } returns bitmap

        try {
            checkAll(
                PropTestConfig(iterations = 100),
                // Simulate raw image sizes from 1 KB to 4 MB (in bytes)
                // This covers images both well under and well over the 2 MB limit
                Arb.long(1_024L..4_194_304L)
            ) { simulatedRawSize ->

                every { bitmap.compress(any(), any(), any()) } answers {
                    val quality = secondArg<Int>()
                    val outputStream = thirdArg<ByteArrayOutputStream>()
                    // Simulate JPEG compression: output ≈ rawSize * (quality / 100)
                    val outputSize = (simulatedRawSize * quality / 100)
                        .coerceAtLeast(1)
                        .toInt()
                    // Write a single byte array of the computed size
                    outputStream.write(ByteArray(outputSize))
                    true
                }

                val compressor = ImageCompressor(contentResolver)

                runTest {
                    val result = compressor.compress(uri)
                    result.size.toLong() shouldBeLessThanOrEqual MAX_IMAGE_SIZE_BYTES
                }
            }
        } finally {
            unmockkStatic(BitmapFactory::class)
        }
    }
})
