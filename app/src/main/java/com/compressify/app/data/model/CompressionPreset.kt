package com.compressify.app.data.model

/**
 * Three simple presets + Custom quality option as defined in requirements:
 * - High Quality: Best visual quality • Larger file
 * - Balanced: Recommended • Great quality + smaller size
 * - Small Size: Smallest file • Lower quality
 * - Custom: User-defined quality (10 - 100)
 */
enum class CompressionPreset(
    val title: String,
    val description: String,
    val defaultQuality: Int,
    val maxDimension: Int?
) {
    HIGH_QUALITY(
        title = "High Quality",
        description = "Best visual quality • Larger file",
        defaultQuality = 90,
        maxDimension = 3840 // 4K max bounds if massive, otherwise preserved
    ),
    BALANCED(
        title = "Balanced",
        description = "Recommended • Great quality + smaller size",
        defaultQuality = 78,
        maxDimension = 2560 // 2.5K sweet spot
    ),
    SMALL_SIZE(
        title = "Small Size",
        description = "Smallest file • Lower quality",
        defaultQuality = 55,
        maxDimension = 1920 // FHD scale for optimal size reduction
    ),
    TARGET_SIZE(
        title = "Target File Size",
        description = "Set specific max target size (e.g. 200 KB, 500 KB, 1 MB)",
        defaultQuality = 80,
        maxDimension = null
    ),
    CUSTOM(
        title = "Custom Quality",
        description = "Fine-tune quality slider to your needs",
        defaultQuality = 80,
        maxDimension = null
    )
}
