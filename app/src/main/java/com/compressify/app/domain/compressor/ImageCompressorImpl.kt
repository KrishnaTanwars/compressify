package com.compressify.app.domain.compressor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.compressify.app.data.model.CompressionConfig
import com.compressify.app.data.model.CompressionPreset
import com.compressify.app.data.model.CompressionResult
import com.compressify.app.data.model.ImageMetadata
import com.compressify.app.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class ImageCompressorImpl(
    private val context: Context
) : ImageCompressor {

    companion object {
        private const val TAG = "ImageCompressorImpl"
    }

    override suspend fun extractMetadata(uri: Uri): Result<ImageMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val localFile = FileUtils.copyUriToTempFile(context, uri)

            val (fileName, querySize) = FileUtils.queryFileDetails(context, uri)
            val finalByteSize = if (localFile != null && localFile.length() > 0) localFile.length() else querySize
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            if (localFile != null) {
                BitmapFactory.decodeFile(localFile.absolutePath, options)
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedInputStream(stream).use { buffered ->
                        BitmapFactory.decodeStream(buffered, null, options)
                    }
                } ?: throw IllegalArgumentException("Cannot open image input stream")
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                throw IllegalArgumentException("Unsupported or corrupted image file: outWidth=${options.outWidth}, outHeight=${options.outHeight}")
            }

            // Read EXIF orientation
            var orientationDegrees = 0
            try {
                val exif = if (localFile != null) {
                    ExifInterface(localFile)
                } else {
                    context.contentResolver.openInputStream(uri)?.use { exifStream ->
                        ExifInterface(exifStream)
                    }
                }
                exif?.let {
                    orientationDegrees = when (it.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read EXIF info", e)
            }

            val finalWidth = if (orientationDegrees == 90 || orientationDegrees == 270) {
                options.outHeight
            } else {
                options.outWidth
            }

            val finalHeight = if (orientationDegrees == 90 || orientationDegrees == 270) {
                options.outWidth
            } else {
                options.outHeight
            }

            ImageMetadata(
                uri = uri,
                fileName = fileName,
                mimeType = mimeType,
                byteSize = finalByteSize,
                width = finalWidth,
                height = finalHeight,
                orientationDegrees = orientationDegrees
            )
        }.onFailure { error ->
            Log.e(TAG, "Error extracting metadata for URI: $uri", error)
        }
    }

    override suspend fun compress(
        metadata: ImageMetadata,
        config: CompressionConfig
    ): Result<CompressionResult> = withContext(Dispatchers.IO) {
        runCatching {
            val startTime = System.currentTimeMillis()
            val localFile = FileUtils.copyUriToTempFile(context, metadata.uri)

            // 1. Calculate target dimension constraints
            val targetMaxDim = when {
                config.preset.maxDimension != null -> config.preset.maxDimension
                else -> 4096 // Absolute safe maximum limit to prevent OOM
            }

            // 2. Decode with sample size
            val rawOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            if (localFile != null) {
                BitmapFactory.decodeFile(localFile.absolutePath, rawOptions)
            } else {
                context.contentResolver.openInputStream(metadata.uri)?.use { stream ->
                    BufferedInputStream(stream).use { buffered ->
                        BitmapFactory.decodeStream(buffered, null, rawOptions)
                    }
                }
            }

            val sampleSize = calculateInSampleSize(rawOptions.outWidth, rawOptions.outHeight, targetMaxDim)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var decodedBitmap: Bitmap = try {
                if (localFile != null) {
                    BitmapFactory.decodeFile(localFile.absolutePath, decodeOptions)
                        ?: throw IllegalStateException("Failed to decode image from local file")
                } else {
                    context.contentResolver.openInputStream(metadata.uri)?.use { stream ->
                        BufferedInputStream(stream).use { buffered ->
                            BitmapFactory.decodeStream(buffered, null, decodeOptions)
                        }
                    } ?: throw IllegalStateException("Failed to decode image from stream")
                }
            } catch (e: OutOfMemoryError) {
                val fallbackOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize * 2
                }
                if (localFile != null) {
                    BitmapFactory.decodeFile(localFile.absolutePath, fallbackOptions)
                        ?: throw IllegalStateException("Image is too large to process on this device.")
                } else {
                    context.contentResolver.openInputStream(metadata.uri)?.use { stream ->
                        BufferedInputStream(stream).use { buffered ->
                            BitmapFactory.decodeStream(buffered, null, fallbackOptions)
                        }
                    } ?: throw IllegalStateException("Image is too large to process on this device.")
                }
            }

            // 3. Handle rotation & precise scaling
            var processedBitmap = decodedBitmap
            val matrix = Matrix()

            if (metadata.orientationDegrees != 0) {
                matrix.postRotate(metadata.orientationDegrees.toFloat())
            }

            val currentMax = max(processedBitmap.width, processedBitmap.height)
            if (currentMax > targetMaxDim) {
                val scale = targetMaxDim.toFloat() / currentMax.toFloat()
                matrix.postScale(scale, scale)
            }

            if (!matrix.isIdentity) {
                val transformed = Bitmap.createBitmap(
                    processedBitmap,
                    0,
                    0,
                    processedBitmap.width,
                    processedBitmap.height,
                    matrix,
                    true
                )
                if (transformed != processedBitmap) {
                    processedBitmap.recycle()
                    processedBitmap = transformed
                }
            }

            // 4. Determine format and compression parameters
            val isPng = metadata.mimeType.contains("png", ignoreCase = true)
            val hasTransparency = processedBitmap.hasAlpha()

            val (format, extension) = when {
                isPng && hasTransparency && config.preserveTransparency -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Pair(Bitmap.CompressFormat.WEBP_LOSSY, "webp")
                    } else {
                        Pair(Bitmap.CompressFormat.PNG, "png")
                    }
                }
                else -> {
                    Pair(Bitmap.CompressFormat.JPEG, "jpg")
                }
            }

            // 5. Output file generation
            if (config.preset == CompressionPreset.TARGET_SIZE && config.targetSizeBytes != null && config.targetSizeBytes > 0) {
                val targetResult = compressToTargetSize(
                    processedBitmap = processedBitmap,
                    targetSizeBytes = config.targetSizeBytes,
                    format = format,
                    extension = extension,
                    metadata = metadata,
                    startTime = startTime
                )
                processedBitmap.recycle()
                return@runCatching targetResult
            }

            val outFileName = FileUtils.generateCompressedFileName(metadata.fileName, extension)
            val tempFile = FileUtils.createTempCompressedFile(context, outFileName)

            FileOutputStream(tempFile).use { outStream ->
                val quality = config.quality.coerceIn(10, 100)
                processedBitmap.compress(format, quality, outStream)
                outStream.flush()
            }

            val compressedWidth = processedBitmap.width
            val compressedHeight = processedBitmap.height

            processedBitmap.recycle()

            val fileProviderAuthority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, fileProviderAuthority, tempFile)

            val duration = System.currentTimeMillis() - startTime

            CompressionResult(
                originalMetadata = metadata,
                compressedFile = tempFile,
                compressedUri = contentUri,
                compressedByteSize = tempFile.length(),
                compressedWidth = compressedWidth,
                compressedHeight = compressedHeight,
                timeTakenMs = duration
            )
        }.onFailure { error ->
            Log.e(TAG, "Error compressing image for URI: ${metadata.uri}", error)
        }
    }

    private fun compressToTargetSize(
        processedBitmap: Bitmap,
        targetSizeBytes: Long,
        format: Bitmap.CompressFormat,
        extension: String,
        metadata: ImageMetadata,
        startTime: Long
    ): CompressionResult {
        var lowQ = 10
        var highQ = 95
        var bestQuality = 50
        var bestTempFile: File? = null
        var bestSize = Long.MAX_VALUE
        var finalWidth = processedBitmap.width
        var finalHeight = processedBitmap.height

        var workingBitmap = processedBitmap

        // Search for highest quality under targetSizeBytes
        for (i in 0..5) {
            val midQ = (lowQ + highQ) / 2
            val outFileName = FileUtils.generateCompressedFileName(metadata.fileName, extension)
            val tempFile = FileUtils.createTempCompressedFile(context, outFileName)

            FileOutputStream(tempFile).use { outStream ->
                workingBitmap.compress(format, midQ, outStream)
                outStream.flush()
            }

            val size = tempFile.length()
            if (size <= targetSizeBytes) {
                bestQuality = midQ
                bestTempFile = tempFile
                bestSize = size
                lowQ = midQ + 1
            } else {
                if (size < bestSize) {
                    bestSize = size
                    bestTempFile = tempFile
                }
                highQ = midQ - 1
            }
        }

        // If file size is still larger than target, iteratively scale down dimensions
        if (bestSize > targetSizeBytes) {
            var scale = 0.85f
            while (scale >= 0.2f && bestSize > targetSizeBytes) {
                val scaledW = (processedBitmap.width * scale).toInt().coerceAtLeast(100)
                val scaledH = (processedBitmap.height * scale).toInt().coerceAtLeast(100)
                val scaledBitmap = Bitmap.createScaledBitmap(processedBitmap, scaledW, scaledH, true)

                for (q in listOf(80, 60, 40, 20)) {
                    val outFileName = FileUtils.generateCompressedFileName(metadata.fileName, extension)
                    val tempFile = FileUtils.createTempCompressedFile(context, outFileName)
                    FileOutputStream(tempFile).use { outStream ->
                        scaledBitmap.compress(format, q, outStream)
                        outStream.flush()
                    }
                    val size = tempFile.length()
                    if (size <= targetSizeBytes || size < bestSize) {
                        bestSize = size
                        bestTempFile = tempFile
                        finalWidth = scaledW
                        finalHeight = scaledH
                        if (size <= targetSizeBytes) break
                    }
                }
                if (scaledBitmap != processedBitmap) {
                    scaledBitmap.recycle()
                }
                scale -= 0.15f
            }
        }

        val finalFile = bestTempFile ?: FileUtils.createTempCompressedFile(
            context,
            FileUtils.generateCompressedFileName(metadata.fileName, extension)
        ).also { file ->
            FileOutputStream(file).use { outStream ->
                processedBitmap.compress(format, 15, outStream)
            }
        }

        val fileProviderAuthority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, fileProviderAuthority, finalFile)
        val duration = System.currentTimeMillis() - startTime

        return CompressionResult(
            originalMetadata = metadata,
            compressedFile = finalFile,
            compressedUri = contentUri,
            compressedByteSize = finalFile.length(),
            compressedWidth = finalWidth,
            compressedHeight = finalHeight,
            timeTakenMs = duration
        )
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        val maxDim = max(width, height)

        if (maxDim > maxDimension) {
            val halfMax = maxDim / 2
            while ((halfMax / inSampleSize) >= maxDimension) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
