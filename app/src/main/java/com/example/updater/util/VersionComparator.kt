package com.example.updater.util

object VersionComparator {

    /**
     * Cleans a raw tag or version string (e.g., "v1.0.2-beta" -> "1.0.2").
     */
    fun cleanVersionString(raw: String): String {
        return raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split("-", "+", "_")[0] // strip pre-release or build metadata
            .trim()
    }

    /**
     * Parses numeric parts of a semantic version string.
     * Example: "1.0.2" -> listOf(1, 0, 2)
     */
    fun parseVersionNumbers(versionStr: String): List<Int> {
        val cleaned = cleanVersionString(versionStr)
        if (cleaned.isEmpty()) return emptyList()

        return cleaned.split(".")
            .mapNotNull { it.trim().takeWhile { char -> char.isDigit() }.toIntOrNull() }
    }

    /**
     * Compares two semantic version strings.
     * Returns:
     *   > 0 if v1 is newer than v2
     *   < 0 if v1 is older than v2
     *   = 0 if versions are equal
     */
    fun compareVersions(v1: String, v2: String): Int {
        val parts1 = parseVersionNumbers(v1)
        val parts2 = parseVersionNumbers(v2)

        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }

            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }
        return 0
    }

    /**
     * Checks if a remote GitHub tag represents a newer update than current app version.
     * Also checks if tag embeds a build/run number when version numbers match.
     */
    fun isRemoteNewer(
        remoteTag: String,
        localVersionName: String,
        localVersionCode: Int = 0
    ): Boolean {
        val result = compareVersions(remoteTag, localVersionName)
        if (result > 0) return true
        if (result < 0) return false

        // If semantic versions are identical, check if the remote tag contains an explicit build suffix
        // e.g. "v1.0.0-build5" vs local versionCode 1
        val buildRegex = Regex("""(?:build|run|b|#)[-.]?(\d+)""", RegexOption.IGNORE_CASE)
        val match = buildRegex.find(remoteTag)
        if (match != null) {
            val remoteBuild = match.groupValues[1].toIntOrNull() ?: 0
            if (remoteBuild > localVersionCode) {
                return true
            }
        }

        return false
    }
}
