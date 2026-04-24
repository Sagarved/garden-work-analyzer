package com.gardenworkanalyzer.data.repository

import android.net.Uri
import com.gardenworkanalyzer.domain.model.GardenImage
import com.gardenworkanalyzer.domain.model.SequencedImage
import kotlinx.coroutines.flow.Flow

interface ImageRepository {
    fun getImages(): Flow<List<GardenImage>>
    fun addImages(uris: List<Uri>): Result<List<GardenImage>>
    fun removeImage(index: Int): Result<List<GardenImage>>
    fun reorder(fromIndex: Int, toIndex: Int): List<GardenImage>
    fun getSequencedImages(): List<SequencedImage>
    fun clear()
}
