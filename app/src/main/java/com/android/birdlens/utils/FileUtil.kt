// EXE201/app/src/main/java/com/android/birdlens/utils/FileUtil.kt
package com.android.birdlens.utils

import android.content.Context
import android.graphics.Bitmap
// Logic: Import Build and ImageDecoder for modern image handling.
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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

    // Logic: This new function reads an image from any given Uri, decodes it into a Bitmap,
    // and then compresses it into a standard format (like JPEG). This allows the app
    // to accept any image type from the user's gallery (e.g., HEIC, WEBP) and convert
    // it into a format the backend understands, ensuring compatibility and reducing upload size.
    fun getConvertedImageFileFromUri(uri: Uri, format: Bitmap.CompressFormat, quality: Int): File? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            // Create a temporary file with a new name and the correct extension.
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

    fun createFileFromBitmap(bitmap: Bitmap, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()
        return file
    }
}