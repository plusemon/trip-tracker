package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageStorageManager {

    suspend fun saveImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val receiptsDir = File(context.filesDir, "receipts").apply {
                    if (!exists()) mkdirs()
                }
                val filename = "receipt_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
                val destFile = File(receiptsDir, filename)

                val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val outStream = FileOutputStream(destFile)
                    // Compress to 85% JPEG to preserve disk space and keep loading snappy
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream)
                    outStream.flush()
                    outStream.close()
                    bitmap.recycle()
                    destFile.absolutePath
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    suspend fun deleteInternalImage(filePath: String) {
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }
}
