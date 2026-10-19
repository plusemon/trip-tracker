package com.example.updater.installer

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

object ApkInstaller {

    private const val TAG = "ApkInstaller"
    private const val PREFS_NAME = "tripsplit_updater_prefs"
    private const val KEY_LAST_DOWNLOAD_ID = "last_download_id"
    private const val KEY_LAST_APK_PATH = "last_apk_path"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Initiates APK download via Android's native DownloadManager.
     * Returns the download ID.
     */
    fun startDownload(
        context: Context,
        apkUrl: String,
        versionTag: String,
        onProgressUpdate: ((progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit)? = null,
        onComplete: ((file: File) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (downloadManager == null) {
            onError?.invoke("DownloadManager service is unavailable on this device.")
            return -1L
        }

        try {
            val fileName = "tripsplit-$versionTag.apk"
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val targetFile = File(downloadsDir, fileName)

            if (targetFile.exists()) {
                targetFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
                setTitle("Trip Tracker Update")
                setDescription("Downloading version $versionTag")
                setMimeType("application/vnd.android.package-archive")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(targetFile))
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = downloadManager.enqueue(request)

            // Save to shared prefs for receiver recovery
            getPrefs(context).edit()
                .putLong(KEY_LAST_DOWNLOAD_ID, downloadId)
                .putString(KEY_LAST_APK_PATH, targetFile.absolutePath)
                .apply()

            // Monitor progress asynchronously
            if (onProgressUpdate != null || onComplete != null || onError != null) {
                trackDownloadProgress(
                    context = context,
                    downloadManager = downloadManager,
                    downloadId = downloadId,
                    targetFile = targetFile,
                    onProgressUpdate = onProgressUpdate,
                    onComplete = onComplete,
                    onError = onError
                )
            }

            return downloadId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start download", e)
            onError?.invoke("Failed to start download: ${e.localizedMessage}")
            return -1L
        }
    }

    private fun trackDownloadProgress(
        context: Context,
        downloadManager: DownloadManager,
        downloadId: Long,
        targetFile: File,
        onProgressUpdate: ((Float, Long, Long) -> Unit)?,
        onComplete: ((File) -> Unit)?,
        onError: ((String) -> Unit)?
    ): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            var isRunning = true
            while (isRunning && isActive) {
                delay(350)
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1
                    val bytesDownloaded = if (bytesDownloadedIndex >= 0) cursor.getLong(bytesDownloadedIndex) else 0L
                    val totalBytes = if (bytesTotalIndex >= 0) cursor.getLong(bytesTotalIndex) else -1L

                    cursor.close()

                    if (totalBytes > 0 && bytesDownloaded >= 0) {
                        val progress = (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        onProgressUpdate?.invoke(progress, bytesDownloaded, totalBytes)
                    }

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            isRunning = false
                            onComplete?.invoke(targetFile)
                            installApkFile(context, targetFile)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            isRunning = false
                            onError?.invoke("Download failed or was cancelled.")
                        }
                    }
                } else {
                    cursor?.close()
                }
            }
        }
    }

    /**
     * Checks if app has permission to install unknown packages (Android 8.0+).
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens system settings to allow installing packages from this app.
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Installs an APK file using FileProvider and the system package installer.
     */
    fun installApkFile(context: Context, file: File): Boolean {
        if (!file.exists()) {
            Log.e(TAG, "Cannot install: File does not exist at ${file.absolutePath}")
            return false
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            context.startActivity(installIntent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching package installer", e)
            return false
        }
    }

    /**
     * Retrieves the last downloaded APK file if it exists.
     */
    fun getLastDownloadedFile(context: Context): File? {
        val path = getPrefs(context).getString(KEY_LAST_APK_PATH, null) ?: return null
        val file = File(path)
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Cleans up older downloaded APK files to save disk space.
     */
    fun cleanOldDownloads(context: Context) {
        try {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
            val files = downloadsDir.listFiles { _, name -> name.endsWith(".apk") } ?: return
            for (file in files) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed cleaning old APK files", e)
        }
    }
}
