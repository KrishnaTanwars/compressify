package com.compressify.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompressionPresetTest {

    @Test
    fun presets_haveSensibleQualityValues() {
        assertTrue(CompressionPreset.HIGH_QUALITY.defaultQuality > CompressionPreset.BALANCED.defaultQuality)
        assertTrue(CompressionPreset.BALANCED.defaultQuality > CompressionPreset.SMALL_SIZE.defaultQuality)
    }

    @Test
    fun presets_dimensionsDescending() {
        assertEquals(3840, CompressionPreset.HIGH_QUALITY.maxDimension)
        assertEquals(2560, CompressionPreset.BALANCED.maxDimension)
        assertEquals(1920, CompressionPreset.SMALL_SIZE.maxDimension)
    }

    @Test
    fun compressionConfig_defaultIsBalanced() {
        val config = CompressionConfig()
        assertEquals(CompressionPreset.BALANCED, config.preset)
        assertEquals(78, config.quality)
    }
}
