package com.gardenworkanalyzer.domain.model

data class SequencedImage(
    val image: GardenImage,
    val sequenceIndex: Int // 1-based index
)
