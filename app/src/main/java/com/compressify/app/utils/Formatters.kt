package com.compressify.app.utils

import java.text.DecimalFormat
import java.util.Locale

object Formatters {
    private val decimalFormatOne = DecimalFormat("#.#")
    private val decimalFormatZero = DecimalFormat("#")

    /**
     * Formats bytes to human-readable size:
     * e.g. 4.8 MB, 780 KB, 850 B
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> "${decimalFormatOne.format(gb)} GB"
            mb >= 1.0 -> "${decimalFormatOne.format(mb)} MB"
            kb >= 1.0 -> "${decimalFormatZero.format(kb)} KB"
            else -> "$bytes B"
        }
    }

    /**
     * Formats percentage saved into user-friendly text:
     * e.g. "84% smaller"
     */
    fun formatPercentageSaved(percentage: Int): String {
        val clamped = percentage.coerceIn(0, 99)
        return "$clamped% smaller"
    }
}
