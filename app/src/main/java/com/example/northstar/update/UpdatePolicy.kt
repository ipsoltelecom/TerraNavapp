package com.example.northstar.update

import java.net.URI

data class AppRelease(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val size: Long,
    val minSdk: Int,
    val notes: String,
    val pageUrl: String,
)

object UpdatePolicy {
    const val REPOSITORY = "ipsoltelecom/TerraNavapp"
    const val API_URL = "https://api.github.com/repos/$REPOSITORY/releases/latest"
    const val MANIFEST_NAME = "terranav-update.json"
    const val MAX_APK_SIZE = 200L * 1024 * 1024
    fun trustedAsset(url: String): Boolean = runCatching {
        val u = URI(url)
        u.scheme == "https" && u.host == "github.com" && u.port == -1 && u.userInfo == null &&
            u.fragment == null && u.query == null &&
            u.rawPath.startsWith("/$REPOSITORY/releases/download/") &&
            !u.rawPath.contains("..")
    }.getOrDefault(false)

    fun trustedDownloadHost(url: String): Boolean = runCatching {
        val u = URI(url)
        u.scheme == "https" && u.port == -1 && u.userInfo == null && u.host in setOf(
            "api.github.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com"
        )
    }.getOrDefault(false)

    fun validRelease(r: AppRelease): Boolean = r.versionCode > 0 && r.versionName.isNotBlank() &&
        r.versionName.length <= 80 && trustedAsset(r.apkUrl) &&
        Regex("[a-fA-F0-9]{64}").matches(r.sha256) && r.size in 1..MAX_APK_SIZE && r.minSdk in 24..100

    fun isNewer(r: AppRelease, installed: Long, sdk: Int) = validRelease(r) && r.versionCode > installed && r.minSdk <= sdk

    fun validIdentity(expected: AppRelease, packageName: String?, ownPackage: String,
                      archiveVersion: Long, installedVersion: Long, archiveSigners: Set<String>, ownSigners: Set<String>): Boolean =
        packageName == ownPackage && archiveVersion == expected.versionCode && archiveVersion > installedVersion &&
            ownSigners.isNotEmpty() && archiveSigners == ownSigners
}
