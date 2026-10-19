package com.example.updater.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubRelease(
    @Json(name = "tag_name")
    val tagName: String,
    @Json(name = "name")
    val name: String? = null,
    @Json(name = "body")
    val body: String? = null,
    @Json(name = "draft")
    val draft: Boolean = false,
    @Json(name = "prerelease")
    val prerelease: Boolean = false,
    @Json(name = "published_at")
    val publishedAt: String? = null,
    @Json(name = "html_url")
    val htmlUrl: String? = null,
    @Json(name = "assets")
    val assets: List<GitHubReleaseAsset> = emptyList()
) {
    /**
     * Finds the primary APK asset attached to this release.
     * Prefers .apk file extension or android package mime type.
     */
    fun findApkAsset(): GitHubReleaseAsset? {
        return assets.firstOrNull { asset ->
            asset.name.endsWith(".apk", ignoreCase = true) ||
                asset.contentType?.equals("application/vnd.android.package-archive", ignoreCase = true) == true
        } ?: assets.firstOrNull { it.browserDownloadUrl.endsWith(".apk", ignoreCase = true) }
    }
}

@JsonClass(generateAdapter = true)
data class GitHubReleaseAsset(
    @Json(name = "id")
    val id: Long = 0,
    @Json(name = "name")
    val name: String,
    @Json(name = "size")
    val size: Long = 0,
    @Json(name = "browser_download_url")
    val browserDownloadUrl: String,
    @Json(name = "content_type")
    val contentType: String? = null,
    @Json(name = "download_count")
    val downloadCount: Int = 0
)
