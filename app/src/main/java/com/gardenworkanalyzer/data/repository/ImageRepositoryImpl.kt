package com.gardenworkanalyzer.data.repository

import android.net.Uri
import com.gardenworkanalyzer.domain.model.GardenImage
import com.gardenworkanalyzer.domain.model.SequencedImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ImageRepositoryImpl : ImageRepository {

    private val _images = MutableStateFlow<List<GardenImage>>(emptyList())

    override fun getImages(): Flow<List<GardenImage>> = _images.asStateFlow()

    override fun addImages(uris: List<Uri>): Result<List<GardenImage>> {
        val newImages = uris.map { uri ->
            GardenImage(
                uri = uri,
                mimeType = "image/jpeg",
                addedTimestamp = System.currentTimeMillis()
            )
        }
        val updated = _images.value + newImages
        _images.value = updated
        return Result.success(updated)
    }

    override fun removeImage(index: Int): Result<List<GardenImage>> {
        val current = _images.value
        if (index < 0 || index >= current.size) {
            return Result.failure(
                IndexOutOfBoundsException("Invalid index: $index. Collection size: ${current.size}")
            )
        }
        val updated = current.toMutableList().apply { removeAt(index) }
        _images.value = updated
        return Result.success(updated)
    }

    override fun reorder(fromIndex: Int, toIndex: Int): List<GardenImage> {
        val current = _images.value.toMutableList()
        val element = current.removeAt(fromIndex)
        current.add(toIndex, element)
        _images.value = current
        return current
    }

    override fun getSequencedImages(): List<SequencedImage> {
        return _images.value.mapIndexed { index, image ->
            SequencedImage(image = image, sequenceIndex = index + 1)
        }
    }

    override fun clear() {
        _images.value = emptyList()
    }
}
