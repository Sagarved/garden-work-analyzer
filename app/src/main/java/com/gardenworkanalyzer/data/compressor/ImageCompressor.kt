package com.gardenworkanalyzer.data.compressor

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.gardenworkanalyzer.domain.model.MAX_IMAGE_SIZE_BYTES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ImageCompressor(private val contentResolver: ContentResolver) {

    companion object {
        private const val TAG = "ImageCompressor"
        private const val INITIAL_QUALITY = 90
        private const val QUALITY_STEP = 10
        private const val MIN_QUALITY = 10
    }

    suspend fun compress(uri: Uri, maxSizeBytes: Long = MAX_IMAGE_SIZE_BYTES): ByteArray =
        withContext(Dispatchers.IO) {
            val bitmap = decodeBitmap(uri)
            compressBitmap(bitmap, maxSizeBytes)
        }

    private fun decodeBitmap(uri: Uri): Bitmap {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: throw ImageCompressionException("Failed to open input stream for URI: $uri")
        } catch (e: ImageCompressionException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap from URI: $uri", e)
            throw ImageCompressionException("Failed to decode image from URI: $uri", e)
        }
    }

    private fun compressBitmap(bitmap: Bitmap, maxSizeBytes: Long): ByteArray {
        var quality = INITIAL_QUALITY

        while (quality >= MIN_QUALITY) {
            try {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                val bytes = outputStream.toByteArray()

                if (bytes.size <= maxSizeBytes) {
                    return bytes
                }

                quality -= QUALITY_STEP
            } catch (e: Exception) {
                Log.e(TAG, "Compression failed at quality $quality", e)
                throw ImageCompressionException("Failed to compress image at quality $quality", e)
            }
        }

        // Final attempt at minimum quality
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, MIN_QUALITY, outputStream)
        return outputStream.toByteArray()
    }
}

class ImageCompressionException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
