package com.compressify.app.data.model

import android.net.Uri
import java.io.File

data class CompressionResult(
    val originalMetadata: ImageMetadata,
    val compressedFile: File,
    val compressedUri: Uri,
    val compressedByteSize: Long,
    val compressedWidth: Int,
    val compressedHeight: Int,
    val timeTakenMs: Long
) {
    val compressedResolutionString: String
        get() = "$compressedWidth × $compressedHeight"

    val bytesSaved: Long
        get() = (originalMetadata.byteSize - compressedByteSize).coerceAtLeast(0L)

    val percentageSaved: Int
        get() = if (originalMetadata.byteSize > 0) {
            val ratio = (originalMetadata.byteSize - compressedByteSize).toDouble() / originalMetadata.byteSize
            (ratio * 100).toInt().coerceIn(0, 99)
        } else {
            0
        }
}
