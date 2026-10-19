package com.example.updater.model

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubRelease(
    @SerializedName("tag_name")
    @Json(name = "tag_name")
    val tagName: String,

    @SerializedName("name")
    @Json(name = "name")
    val name: String? = null,

    @SerializedName("body")
    @Json(name = "body")
    val body: String? = null,

    @SerializedName("draft")
    @Json(name = "draft")
    val draft: Boolean = false,

    @SerializedName("prerelease")
    @Json(name = "prerelease")
    val prerelease: Boolean = false,

    @SerializedName("published_at")
    @Json(name = "published_at")
    val publishedAt: String? = null,

    @SerializedName("html_url")
    @Json(name = "html_url")
    val htmlUrl: String? = null,

    @SerializedName("assets")
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
    @SerializedName("id")
    @Json(name = "id")
    val id: Long = 0,

    @SerializedName("name")
    @Json(name = "name")
    val name: String,

    @SerializedName("size")
    @Json(name = "size")
    val size: Long = 0,

    @SerializedName("browser_download_url")
    @Json(name = "browser_download_url")
    val browserDownloadUrl: String,

    @SerializedName("content_type")
    @Json(name = "content_type")
    val contentType: String? = null,

    @SerializedName("download_count")
    @Json(name = "download_count")
    val downloadCount: Int = 0
)
