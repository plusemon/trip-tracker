package com.example.updater.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.updater.api.GitHubApiClient
import com.example.updater.installer.ApkInstaller
import com.example.updater.util.VersionComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UpdateInfo(
    val tagName: String,
    val title: String,
    val changelog: String,
    val apkUrl: String,
    val apkSize: Long,
    val publishedAt: String? = null
) {
    val formattedSize: String
        get() {
            if (apkSize <= 0) return "Unknown size"
            val mb = apkSize.toDouble() / (1024 * 1024)
            return String.format("%.1f MB", mb)
        }
}

data class UpdateUiState(
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val updateInfo: UpdateInfo? = null,
    val showDialog: Boolean = false,
    val userMessage: String? = null,
    val lastCheckStatus: String? = null,
    val isUpToDate: Boolean = false,
    val needsInstallPermission: Boolean = false
)

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    // Configurable repo owner & name, initialized with BuildConfig constants
    var repoOwner: String = BuildConfig.GITHUB_REPO_OWNER
    var repoName: String = BuildConfig.GITHUB_REPO_NAME

    /**
     * Checks for updates from GitHub releases.
     * @param manual If true, triggers UI feedback even when no update is found.
     */
    fun checkForUpdates(manual: Boolean = false) {
        if (_uiState.value.isChecking) return

        _uiState.update { it.copy(isChecking = true, userMessage = null) }

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    GitHubApiClient.releaseService.getLatestRelease(
                        owner = repoOwner,
                        repo = repoName
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    val release = response.body()!!
                    val isNewer = VersionComparator.isRemoteNewer(
                        remoteTag = release.tagName,
                        localVersionName = BuildConfig.VERSION_NAME,
                        localVersionCode = BuildConfig.VERSION_CODE
                    )

                    val apkAsset = release.findApkAsset()

                    val prefs = getApplication<Application>().getSharedPreferences("updater_prefs", Context.MODE_PRIVATE)
                    val skippedTag = prefs.getString("skipped_tag", null)
                    val shouldShow = isNewer && apkAsset != null && (manual || release.tagName != skippedTag)

                    if (shouldShow) {
                        val info = UpdateInfo(
                            tagName = release.tagName,
                            title = release.name ?: release.tagName,
                            changelog = release.body ?: "No release notes provided for this version.",
                            apkUrl = apkAsset.browserDownloadUrl,
                            apkSize = apkAsset.size,
                            publishedAt = release.publishedAt
                        )

                        _uiState.update {
                            it.copy(
                                isChecking = false,
                                updateInfo = info,
                                showDialog = true,
                                isUpToDate = false,
                                lastCheckStatus = "New update available: ${release.tagName}"
                            )
                        }
                    } else if (isNewer && apkAsset == null) {
                        _uiState.update {
                            it.copy(
                                isChecking = false,
                                lastCheckStatus = "Release ${release.tagName} found, but no APK asset is attached yet.",
                                userMessage = if (manual) "Found ${release.tagName}, but the APK build is still generating or not attached." else null
                            )
                        }
                    } else {
                        // Current version is up to date
                        _uiState.update {
                            it.copy(
                                isChecking = false,
                                isUpToDate = true,
                                lastCheckStatus = "App is up to date (v${BuildConfig.VERSION_NAME})",
                                userMessage = if (manual) "You are using the latest version of Trip Tracker (v${BuildConfig.VERSION_NAME})! 🎉" else null
                            )
                        }
                    }
                } else {
                    val errorMsg = "Could not fetch release info (HTTP ${response.code()})"
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            lastCheckStatus = errorMsg,
                            userMessage = if (manual) errorMsg else null
                        )
                    }
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Network error checking for updates"
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        lastCheckStatus = "Failed: $errorMsg",
                        userMessage = if (manual) "Unable to connect to GitHub. Check internet connection." else null
                    )
                }
            }
        }
    }

    /**
     * Initiates the download and installation of the detected update.
     */
    fun startUpdateDownload(context: Context) {
        val info = _uiState.value.updateInfo ?: return

        // Verify install permission on Android 8+
        if (!ApkInstaller.canRequestPackageInstalls(context)) {
            _uiState.update { it.copy(needsInstallPermission = true) }
        }

        _uiState.update {
            it.copy(
                isDownloading = true,
                downloadProgress = 0f,
                downloadedBytes = 0L,
                totalBytes = info.apkSize
            )
        }

        ApkInstaller.startDownload(
            context = context,
            apkUrl = info.apkUrl,
            versionTag = info.tagName,
            onProgressUpdate = { progress, downloaded, total ->
                _uiState.update {
                    it.copy(
                        downloadProgress = progress,
                        downloadedBytes = downloaded,
                        totalBytes = total
                    )
                }
            },
            onComplete = {
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        showDialog = false,
                        userMessage = "Update downloaded! Opening package installer..."
                    )
                }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        userMessage = "Update download error: $error"
                    )
                }
            }
        )
    }

    fun openPermissionSettings(context: Context) {
        ApkInstaller.openInstallPermissionSettings(context)
        _uiState.update { it.copy(needsInstallPermission = false) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showDialog = false) }
    }

    fun skipVersion(tagName: String) {
        val prefs = getApplication<Application>().getSharedPreferences("updater_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("skipped_tag", tagName).apply()
        _uiState.update { it.copy(showDialog = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
