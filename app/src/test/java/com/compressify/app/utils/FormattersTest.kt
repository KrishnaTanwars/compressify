package com.compressify.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun formatFileSize_zeroOrNegative_returnsZeroBytes() {
        assertEquals("0 B", Formatters.formatFileSize(0))
        assertEquals("0 B", Formatters.formatFileSize(-100))
    }

    @Test
    fun formatFileSize_bytesLessThan1KB_formatsCorrectly() {
        assertEquals("512 B", Formatters.formatFileSize(512))
        assertEquals("1023 B", Formatters.formatFileSize(1023))
    }

    @Test
    fun formatFileSize_kilobytes_formatsCorrectly() {
        assertEquals("1 KB", Formatters.formatFileSize(1024))
        assertEquals("780 KB", Formatters.formatFileSize(780 * 1024))
    }

    @Test
    fun formatFileSize_megabytes_formatsCorrectly() {
        assertEquals("4.8 MB", Formatters.formatFileSize((4.8 * 1024 * 1024).toLong()))
        assertEquals("10.5 MB", Formatters.formatFileSize((10.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun formatPercentageSaved_standardValues_formatsCorrectly() {
        assertEquals("84% smaller", Formatters.formatPercentageSaved(84))
        assertEquals("50% smaller", Formatters.formatPercentageSaved(50))
        assertEquals("0% smaller", Formatters.formatPercentageSaved(0))
    }

    @Test
    fun formatPercentageSaved_outOfRange_clampsSafely() {
        assertEquals("0% smaller", Formatters.formatPercentageSaved(-10))
        assertEquals("99% smaller", Formatters.formatPercentageSaved(105))
    }
}
