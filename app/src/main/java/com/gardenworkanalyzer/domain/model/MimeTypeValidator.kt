package com.gardenworkanalyzer.domain.model

/**
 * Returns true if the given MIME type is in the set of supported MIME types.
 */
fun isValidMimeType(mimeType: String): Boolean =
    mimeType in SUPPORTED_MIME_TYPES
