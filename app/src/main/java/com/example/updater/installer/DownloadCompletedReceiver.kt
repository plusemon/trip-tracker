package com.example.updater.installer

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.io.File

class DownloadCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: intentType?) {
        // Safe check for null intent
        val receivedIntent = intent ?: return
        if (receivedIntent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val downloadId = receivedIntent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) return

        Log.d("DownloadReceiver", "Download complete event received for ID: $downloadId")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor != null && cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)

            val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1
            val localUriString = if (uriIndex >= 0) cursor.getString(uriIndex) else null
            cursor.close()

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                val targetFile = if (!localUriString.isNullOrEmpty()) {
                    val uri = Uri.parse(localUriString)
                    if (uri.scheme == "file") {
                        File(uri.path ?: "")
                    } else {
                        ApkInstaller.getLastDownloadedFile(context)
                    }
                } else {
                    ApkInstaller.getLastDownloadedFile(context)
                }

                if (targetFile != null && targetFile.exists() && targetFile.length() > 0) {
                    Log.i("DownloadReceiver", "Triggering installation for: ${targetFile.absolutePath}")
                    ApkInstaller.installApkFile(context, targetFile)
                }
            }
        } else {
            cursor?.close()
        }
    }
}

private typealias intentType = Intent
