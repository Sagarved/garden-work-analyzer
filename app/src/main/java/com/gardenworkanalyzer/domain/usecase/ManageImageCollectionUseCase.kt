package com.gardenworkanalyzer.domain.usecase

import android.content.ContentResolver
import android.net.Uri
import com.gardenworkanalyzer.data.repository.ImageRepository
import com.gardenworkanalyzer.domain.model.GardenImage
import com.gardenworkanalyzer.domain.model.MAX_IMAGE_COUNT
import com.gardenworkanalyzer.domain.model.isValidMimeType

class ManageImageCollectionUseCase(
    private val imageRepository: ImageRepository,
    private val contentResolver: ContentResolver
) {

    fun addImages(uris: List<Uri>): Result<List<GardenImage>> {
        if (uris.isEmpty()) {
            return Result.failure(IllegalArgumentException("No images provided"))
        }

        val currentImages = imageRepository.getSequencedImages()
        val currentCount = currentImages.size

        if (isCollectionFull(currentCount, uris.size)) {
            return Result.failure(
                IllegalStateException(
                    "Cannot add ${uris.size} image(s). Maximum of $MAX_IMAGE_COUNT images allowed (currently $currentCount)."
                )
            )
        }

        val invalidUris = uris.filter { !validateFormat(it) }
        if (invalidUris.isNotEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    "Unsupported image format. Supported formats: JPEG, PNG, WebP."
                )
            )
        }

        return imageRepository.addImages(uris)
    }

    fun removeImage(index: Int): Result<List<GardenImage>> {
        val currentImages = imageRepository.getSequencedImages()
        if (index < 0 || index >= currentImages.size) {
            return Result.failure(
                IndexOutOfBoundsException("Invalid image index: $index. Collection size: ${currentImages.size}")
            )
        }
        return imageRepository.removeImage(index)
    }

    fun validateFormat(uri: Uri): Boolean {
        val mimeType = contentResolver.getType(uri) ?: return false
        return isValidMimeType(mimeType)
    }

    fun isCollectionFull(currentCount: Int, addCount: Int): Boolean {
        return currentCount + addCount > MAX_IMAGE_COUNT
    }
}
