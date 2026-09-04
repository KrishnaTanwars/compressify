package com.compressify.app.domain.compressor

import android.net.Uri
import com.compressify.app.data.model.CompressionConfig
import com.compressify.app.data.model.CompressionResult
import com.compressify.app.data.model.ImageMetadata

interface ImageCompressor {
    suspend fun extractMetadata(uri: Uri): Result<ImageMetadata>
    suspend fun compress(metadata: ImageMetadata, config: CompressionConfig): Result<CompressionResult>
}
