// EXE201/app/src/main/java/com/android/birdlens/utils/FileUtil.kt
package com.android.birdlens.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Transformer
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class FileUtil(private val context: Context) {
    fun getFileFromUri(uri: Uri): File? {
        var inputStream: InputStream? = null
        var file: File? = null
        try {
            val fileName = getFileName(uri) ?: return null
            inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, fileName)
            tempFile.createNewFile()
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            outputStream.close()
            file = tempFile
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
        return file
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        result = cursor.getString(displayNameIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1 && cut != null) {
                if (result != null) {
                    result = result.substring(cut + 1)
                }
            }
        }
        return result ?: "temp_media_file" // Fallback filename
    }

    fun getConvertedImageFileFromUri(uri: Uri, format: Bitmap.CompressFormat, quality: Int): File? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            val extension = when (format) {
                Bitmap.CompressFormat.JPEG -> ".jpg"
                Bitmap.CompressFormat.PNG -> ".png"
                else -> ".jpg" // Default to JPEG
            }
            val tempFile = File.createTempFile("converted_image_", extension, context.cacheDir)

            FileOutputStream(tempFile).use { out ->
                bitmap.compress(format, quality, out)
            }

            bitmap.recycle() // Free up memory used by the bitmap
            tempFile
        } catch (e: Exception) {
            Log.e("FileUtil", "Error converting image from URI: $uri", e)
            null
        }
    }

    @OptIn(UnstableApi::class)
    fun convertVideoToMp4(
        uri: Uri,
        listener: Transformer.Listener
    ): File {
        val outputFileName = "converted_video_${UUID.randomUUID()}.mp4"
        val outputFile = File(context.cacheDir, outputFileName)
        val mediaItem = MediaItem.fromUri(uri)

        // Updated API: Use addListener() instead of setListener()
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264) // Standard H.264 codec
            .setAudioMimeType(MimeTypes.AUDIO_AAC)   // Standard AAC audio codec
            .addListener(listener)  // Changed from setListener() to addListener()
            .setLooper(context.mainLooper)
            .build()

        // Use the new API: start() method with EditedMediaItem
        val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()
        transformer.start(editedMediaItem, outputFile.absolutePath)

        return outputFile
    }

    fun createFileFromBitmap(bitmap: Bitmap, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()
        return file
    }
}