package com.example.northstar.update

import org.junit.Assert.*
import org.junit.Test

class UpdatePolicyTest {
    private val release = AppRelease(4,"0.4-test","https://github.com/ipsoltelecom/TerraNavapp/releases/download/v0.4-test/TerraNav.apk",
        "ab".repeat(32),70_000_000,24,"Novidades","https://github.com/ipsoltelecom/TerraNavapp/releases")
    @Test fun onlyOffersNewCompatibleVersions() {
        assertTrue(UpdatePolicy.isNewer(release,3,36))
        assertFalse(UpdatePolicy.isNewer(release,4,36))
        assertFalse(UpdatePolicy.isNewer(release,5,36))
        assertFalse(UpdatePolicy.isNewer(release.copy(minSdk=37),3,36))
    }
    @Test fun rejectsWrongRepositoriesAndInsecureUrls() {
        assertTrue(UpdatePolicy.trustedAsset(release.apkUrl))
        listOf(release.apkUrl.replace("https:","http:"),release.apkUrl.replace("TerraNavapp","Other"),
            release.apkUrl.replace("github.com","github.com.evil.test"),
            release.apkUrl.replace("github.com/","github.com:8080/"),
            release.apkUrl.replace("github.com/","someone@github.com/"),
            release.apkUrl+"?redirect=evil").forEach { assertFalse(it,UpdatePolicy.trustedAsset(it)) }
    }
    @Test fun onlyAcceptsGithubDownloadRedirects() {
        assertTrue(UpdatePolicy.trustedDownloadHost("https://release-assets.githubusercontent.com/path?signature=abc"))
        assertFalse(UpdatePolicy.trustedDownloadHost("https://example.com/file.apk"))
        assertFalse(UpdatePolicy.trustedDownloadHost("http://release-assets.githubusercontent.com/path"))
    }
    @Test fun rejectsMissingHashAndUnboundedDownload() {
        assertFalse(UpdatePolicy.validRelease(release.copy(sha256="")))
        assertFalse(UpdatePolicy.validRelease(release.copy(size=0)))
        assertFalse(UpdatePolicy.validRelease(release.copy(size=UpdatePolicy.MAX_APK_SIZE+1)))
        assertFalse(UpdatePolicy.validRelease(release.copy(versionCode=0)))
    }
    @Test fun requiresSamePackageSignerAndExactManifestVersion() {
        fun valid(pkg: String="app",code:Long=4,signers:Set<String> = setOf("trusted"),own:Set<String> = setOf("trusted")) =
            UpdatePolicy.validIdentity(release,pkg,"app",code,3,signers,own)
        assertTrue(valid())
        assertFalse(valid(pkg="other"))
        assertFalse(valid(code=3))
        assertFalse(valid(code=5))
        assertFalse(valid(signers=setOf("attacker")))
        assertFalse(valid(signers=emptySet(),own=emptySet()))
    }
}
