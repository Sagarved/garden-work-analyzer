package com.gardenworkanalyzer.domain.model

import android.net.Uri

data class GardenImage(
    val uri: Uri,
    val mimeType: String,
    val addedTimestamp: Long,
    val thumbnailUri: Uri? = null
)
