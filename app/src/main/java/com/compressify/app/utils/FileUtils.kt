package com.compressify.app.utils

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    /**
     * Resolves display name and file size from a content URI.
     */
    fun queryFileDetails(context: Context, uri: Uri): Pair<String, Long> {
        var name = "image_${System.currentTimeMillis()}"
        var size = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { if (it.isNotBlank()) name = it }
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to defaults
        }

        // If size is still 0, try reading from AssetFileDescriptor
        if (size <= 0L) {
            try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { pfd ->
                    val len = pfd.length
                    if (len != AssetFileDescriptor.UNKNOWN_LENGTH) {
                        size = len
                    }
                }
            } catch (_: Exception) {}
        }

        return Pair(name, size)
    }

    /**
     * Generates a safe, clean compressed file name:
     * e.g. "photo_compressed.jpg" or "IMG_20260903_182500_compressed.jpg"
     * Never overwrites original images.
     */
    fun generateCompressedFileName(originalName: String?, outputExtension: String): String {
        val ext = if (outputExtension.startsWith(".")) outputExtension else ".$outputExtension"
        if (originalName.isNullOrBlank()) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            return "IMG_${timestamp}_compressed$ext"
        }

        val dotIndex = originalName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
        // Clean special characters
        val cleanBaseName = baseName.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
        return "${cleanBaseName}_compressed$ext"
    }

    /**
     * Creates a temporary file in cacheDir/compressed/
     */
    fun createTempCompressedFile(context: Context, fileName: String): File {
        val cacheDir = File(context.cacheDir, "compressed")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, fileName)
    }

    /**
     * Copies a Content URI stream to a temporary local file in cacheDir
     * for safe, repeatable decoding and EXIF parsing.
     */
    fun copyUriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val tempFile = File.createTempFile("input_img_", ".tmp", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            if (tempFile.length() > 0) tempFile else null
        } catch (e: Exception) {
            Log.e("FileUtils", "Error copying URI to temp file", e)
            null
        }
    }

    /**
     * Clears old temporary files older than 24 hours.
     */
    fun cleanOldCache(context: Context) {
        try {
            // Clean compressed directory
            val cacheDir = File(context.cacheDir, "compressed")
            if (cacheDir.exists()) {
                val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
                cacheDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < oneDayAgo) {
                        file.delete()
                    }
                }
            }
            // Clean root cacheDir temporary input files
            val oneHourAgo = System.currentTimeMillis() - 60 * 60 * 1000
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("input_img_") && file.lastModified() < oneHourAgo) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }
}
