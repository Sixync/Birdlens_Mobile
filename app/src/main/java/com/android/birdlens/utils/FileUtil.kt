// EXE201/app/src/main/java/com/android/birdlens/utils/FileUtil.kt
package com.android.birdlens.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
}