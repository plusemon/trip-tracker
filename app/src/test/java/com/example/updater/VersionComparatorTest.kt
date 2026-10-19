package com.example.updater

import com.example.updater.model.GitHubRelease
import com.example.updater.model.GitHubReleaseAsset
import com.example.updater.util.VersionComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun testCleanVersionString() {
        assertEquals("1.0.2", VersionComparator.cleanVersionString("v1.0.2"))
        assertEquals("1.0.2", VersionComparator.cleanVersionString("V1.0.2-beta"))
        assertEquals("2.1.0", VersionComparator.cleanVersionString("2.1.0+build123"))
        assertEquals("1.5", VersionComparator.cleanVersionString("  v1.5_release "))
    }

    @Test
    fun testCompareVersions() {
        // v1 > v2
        assertTrue(VersionComparator.compareVersions("1.0.1", "1.0.0") > 0)
        assertTrue(VersionComparator.compareVersions("1.1.0", "1.0.9") > 0)
        assertTrue(VersionComparator.compareVersions("2.0.0", "1.99.99") > 0)
        assertTrue(VersionComparator.compareVersions("1.0.1", "1.0") > 0)

        // v1 == v2
        assertEquals(0, VersionComparator.compareVersions("1.0.0", "1.0"))
        assertEquals(0, VersionComparator.compareVersions("v1.2", "1.2.0"))

        // v1 < v2
        assertTrue(VersionComparator.compareVersions("1.0.0", "1.0.1") < 0)
        assertTrue(VersionComparator.compareVersions("0.9.9", "1.0.0") < 0)
    }

    @Test
    fun testIsRemoteNewer() {
        assertTrue(VersionComparator.isRemoteNewer("v1.0.1", "1.0", 1))
        assertTrue(VersionComparator.isRemoteNewer("v1.1", "1.0", 1))
        assertTrue(VersionComparator.isRemoteNewer("v2.0.0", "1.0", 1))

        assertFalse(VersionComparator.isRemoteNewer("v1.0.0", "1.0", 1))
        assertFalse(VersionComparator.isRemoteNewer("v0.9.9", "1.0", 1))

        // Build code comparison when semantic version matches
        assertTrue(VersionComparator.isRemoteNewer("v1.0-build5", "1.0", 1))
        assertFalse(VersionComparator.isRemoteNewer("v1.0-build1", "1.0", 2))
    }

    @Test
    fun testFindApkAsset() {
        val release = GitHubRelease(
            tagName = "v1.0.1",
            assets = listOf(
                GitHubReleaseAsset(
                    id = 1,
                    name = "source-code.zip",
                    browserDownloadUrl = "https://github.com/test/source.zip"
                ),
                GitHubReleaseAsset(
                    id = 2,
                    name = "TripSplitBD-v1.0.1.apk",
                    size = 12500000,
                    browserDownloadUrl = "https://github.com/test/TripSplitBD-v1.0.1.apk",
                    contentType = "application/vnd.android.package-archive"
                )
            )
        )

        val apkAsset = release.findApkAsset()
        assertNotNull(apkAsset)
        assertEquals("TripSplitBD-v1.0.1.apk", apkAsset?.name)
        assertEquals("https://github.com/test/TripSplitBD-v1.0.1.apk", apkAsset?.browserDownloadUrl)
    }
}
