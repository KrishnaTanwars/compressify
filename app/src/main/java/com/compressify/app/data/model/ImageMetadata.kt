package com.compressify.app.data.model

import android.net.Uri

data class ImageMetadata(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val byteSize: Long,
    val width: Int,
    val height: Int,
    val orientationDegrees: Int = 0
) {
    val resolutionString: String
        get() = "$width × $height"
}
