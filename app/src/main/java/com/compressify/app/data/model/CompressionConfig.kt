package com.compressify.app.data.model

data class CompressionConfig(
    val preset: CompressionPreset = CompressionPreset.BALANCED,
    val quality: Int = 78,
    val targetSizeBytes: Long? = null,
    val preserveTransparency: Boolean = true
)
