package com.compressify.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileUtilsTest {

    @Test
    fun generateCompressedFileName_standardName_appendsCompressed() {
        val original = "vacation_photo.jpg"
        val result = FileUtils.generateCompressedFileName(original, "jpg")
        assertEquals("vacation_photo_compressed.jpg", result)
    }

    @Test
    fun generateCompressedFileName_differentExtension_preservesNewExtension() {
        val original = "document.png"
        val result = FileUtils.generateCompressedFileName(original, "webp")
        assertEquals("document_compressed.webp", result)
    }

    @Test
    fun generateCompressedFileName_nullOrBlank_generatesTimestampName() {
        val resultNull = FileUtils.generateCompressedFileName(null, "jpg")
        assertTrue(resultNull.startsWith("IMG_"))
        assertTrue(resultNull.endsWith("_compressed.jpg"))

        val resultBlank = FileUtils.generateCompressedFileName("   ", "jpg")
        assertTrue(resultBlank.startsWith("IMG_"))
        assertTrue(resultBlank.endsWith("_compressed.jpg"))
    }

    @Test
    fun generateCompressedFileName_specialCharacters_sanitizesCorrectly() {
        val original = "my/photo:test?#1.jpeg"
        val result = FileUtils.generateCompressedFileName(original, "jpg")
        assertTrue(result.endsWith("_compressed.jpg"))
        assertTrue(!result.contains(":"))
        assertTrue(!result.contains("?"))
    }
}
