package com.compressify.app.data.storage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.compressify.app.data.model.CompressionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FileStorageManager(
    private val context: Context
) {

    /**
     * Saves the compressed image to the public Pictures/Compressify directory
     * using MediaStore for Scoped Storage compliance.
     */
    suspend fun saveToGallery(result: CompressionResult): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFile = result.compressedFile
            val fileName = sourceFile.name
            val mimeType = when {
                fileName.endsWith(".png", true) -> "image/png"
                fileName.endsWith(".webp", true) -> "image/webp"
                else -> "image/jpeg"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Compressify")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = context.contentResolver.insert(collection, contentValues)
                    ?: throw IllegalStateException("Failed to create MediaStore entry")

                context.contentResolver.openOutputStream(itemUri)?.use { outStream ->
                    FileInputStream(sourceFile).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                } ?: throw IllegalStateException("Failed to open output stream for image")

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(itemUri, contentValues, null, null)

                itemUri
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val targetDir = File(picturesDir, "Compressify")
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                val targetFile = File(targetDir, fileName)
                FileInputStream(sourceFile).use { inStream ->
                    FileOutputStream(targetFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, targetFile.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: Uri.fromFile(targetFile)
            }
        }
    }

    /**
     * Creates an Intent to share the compressed image file using FileProvider.
     */
    fun createShareIntent(result: CompressionResult): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            val fileName = result.compressedFile.name
            type = when {
                fileName.endsWith(".png", true) -> "image/png"
                fileName.endsWith(".webp", true) -> "image/webp"
                else -> "image/jpeg"
            }
            putExtra(Intent.EXTRA_STREAM, result.compressedUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Share Compressed Image")
    }
}
