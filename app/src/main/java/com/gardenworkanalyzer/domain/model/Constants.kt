package com.gardenworkanalyzer.domain.model

val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
const val MAX_IMAGE_COUNT = 10
const val MIN_IMAGE_COUNT = 1
const val MAX_IMAGE_SIZE_BYTES = 2L * 1024 * 1024
const val MAX_RETRY_ATTEMPTS = 3
const val CONFIDENCE_THRESHOLD = 0.5
