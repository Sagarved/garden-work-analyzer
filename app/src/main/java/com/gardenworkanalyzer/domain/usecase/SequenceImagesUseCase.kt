package com.gardenworkanalyzer.domain.usecase

import com.gardenworkanalyzer.data.repository.ImageRepository
import com.gardenworkanalyzer.domain.model.GardenImage
import com.gardenworkanalyzer.domain.model.SequencedImage

class SequenceImagesUseCase(private val imageRepository: ImageRepository) {

    fun reorder(fromIndex: Int, toIndex: Int): List<GardenImage> {
        return imageRepository.reorder(fromIndex, toIndex)
    }

    fun finalizeSequence(): List<SequencedImage> {
        return imageRepository.getSequencedImages()
    }
}
