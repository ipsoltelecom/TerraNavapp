package com.example.northstar.update

import com.example.northstar.settings.L10n
import com.example.northstar.R
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

enum class UpdateStage { IDLE, CHECKING, AVAILABLE, DOWNLOADING, READY, ERROR }
data class UpdateState(
    val stage: UpdateStage = UpdateStage.IDLE,
    val release: AppRelease? = null,
    val progress: Int = 0,
    val message: String = L10n.text(R.string.ui_f4a2f7a8a6a7),
    val rideActive: Boolean = false,
)

class AppUpdater private constructor(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences("terranav_updates", Context.MODE_PRIVATE)
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _state = MutableStateFlow(UpdateState())
    val state = _state.asStateFlow()
    private val directory = File(context.cacheDir,"updates").apply { mkdirs() }
    private val readyFile = File(directory,"terranav-verified.apk")
    private val partialFile = File(directory,"terranav.part")
    private var task: Job? = null
    private val pm = context.packageManager
    @Suppress("DEPRECATION")
    private fun installedInfo() = pm.getPackageInfo(context.packageName, signerFlags())
    val installedName: String get() = installedInfo().versionName ?: "?"
    private val installedCode: Long get() = PackageInfoCompat.getLongVersionCode(installedInfo())

    @Synchronized fun setRideActive(active: Boolean) {
        _state.update { it.copy(rideActive = active) }
        if (active && task?.isActive == true) {
            task?.cancel()
            _state.update { it.copy(stage = if(it.release != null) UpdateStage.AVAILABLE else UpdateStage.IDLE,
                message = L10n.text(R.string.ui_be558203c1b2)) }
        }
    }

    @Synchronized fun checkForUpdates(force: Boolean = false) {
        if (_state.value.rideActive || task?.isActive == true) return
        val now = System.currentTimeMillis()
        if (!force && now < prefs.getLong("next_check",0)) return
        _state.update { it.copy(stage = UpdateStage.CHECKING, message = L10n.text(R.string.ui_95ef08902d3e)) }
        task = scope.launch {
            try {
                val releaseResponse = getSmallJson(UpdatePolicy.API_URL, 256_000, allow404 = true)
                if (releaseResponse == null) {
                    _state.update { it.copy(stage = UpdateStage.IDLE,release = null,message = L10n.text(R.string.ui_55c083a3f52e)) }
                } else {
                    check(!releaseResponse.optBoolean("draft") && !releaseResponse.optBoolean("prerelease"))
                    val assets = releaseResponse.getJSONArray("assets")
                    val entries = (0 until assets.length()).map { assets.getJSONObject(it) }
                    val manifestAsset = entries.singleOrNull { it.optString("name") == UpdatePolicy.MANIFEST_NAME }
                        ?: error(L10n.text(R.string.ui_0ec64585af5a))
                    val manifestUrl = manifestAsset.getString("browser_download_url")
                    check(UpdatePolicy.trustedAsset(manifestUrl)) { L10n.text(R.string.ui_b43ce39e78a8) }
                    val manifest = getSmallJson(manifestUrl,32_000) ?: error(L10n.text(R.string.ui_b325b9342efa))
                    check(manifest.getString("applicationId") == context.packageName) { L10n.text(R.string.ui_a911aba54471) }
                    val apk = entries.singleOrNull { it.optString("name") == manifest.getString("apkName") }
                        ?: error(L10n.text(R.string.ui_c2b9f9028e98))
                    val r = AppRelease(manifest.getLong("versionCode"),manifest.getString("versionName"),
                        apk.getString("browser_download_url"),manifest.getString("sha256"),manifest.getLong("size"),
                        manifest.getInt("minSdk"),releaseResponse.optString("body").take(6000),
                        "https://github.com/${UpdatePolicy.REPOSITORY}/releases")
                    check(UpdatePolicy.validRelease(r) && apk.getLong("size") == r.size) { L10n.text(R.string.ui_f3438abdfb9d) }
                    currentCoroutineContext().ensureActive()
                    _state.update { it.copy(release = if(UpdatePolicy.isNewer(r,installedCode,Build.VERSION.SDK_INT)) r else null,
                        stage = if(UpdatePolicy.isNewer(r,installedCode,Build.VERSION.SDK_INT)) UpdateStage.AVAILABLE else UpdateStage.IDLE,
                        message = when {
                            r.versionCode <= installedCode -> L10n.text(R.string.ui_45c07211308f)
                            r.minSdk > Build.VERSION.SDK_INT -> L10n.text(R.string.ui_6b624a4ae98b)
                            else -> L10n.text(R.string.ui_842cccf76dd3, r.versionName)
                        }) }
                }
                prefs.edit().putLong("next_check",now + 86_400_000).apply()
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _state.update { it.copy(stage = UpdateStage.ERROR,message = L10n.text(R.string.ui_51731068f538, friendly(e))) }
                prefs.edit().putLong("next_check",now + 3_600_000).apply()
            }
        }
    }

    @Synchronized fun download() {
        val r = _state.value.release ?: return
        if (_state.value.rideActive || task?.isActive == true) return
        _state.update { it.copy(stage = UpdateStage.DOWNLOADING,progress = 0,message = L10n.text(R.string.ui_a69068d58f19)) }
        task = scope.launch {
            var conn: HttpURLConnection? = null
            try {
                readyFile.delete(); partialFile.delete()
                conn = open(r.apkUrl)
                check(conn.responseCode == 200) { L10n.text(R.string.ui_52c64fd8e633, conn.responseCode) }
                var total = 0L
                conn.inputStream.use { input -> partialFile.outputStream().use { output ->
                    val bytes = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(bytes); if(count < 0) break
                        total += count
                        check(total <= r.size && total <= UpdatePolicy.MAX_APK_SIZE) { L10n.text(R.string.ui_963aec0e8b3a) }
                        output.write(bytes,0,count)
                        _state.update { it.copy(progress = (total * 100 / r.size).toInt()) }
                    }
                } }
                currentCoroutineContext().ensureActive()
                verifyApk(partialFile,r)
                check(partialFile.renameTo(readyFile)) { L10n.text(R.string.ui_aad8e9c542cf) }
                currentCoroutineContext().ensureActive()
                _state.update { it.copy(stage = UpdateStage.READY,progress = 100,message = L10n.text(R.string.ui_cc7f0b92f7b9)) }
            } catch (e: CancellationException) { partialFile.delete(); throw e
            } catch (e: Exception) {
                partialFile.delete(); readyFile.delete()
                _state.update { it.copy(stage = UpdateStage.ERROR,message = L10n.text(R.string.ui_153dc3d41e8a, friendly(e))) }
            } finally { conn?.disconnect() }
        }
    }

    @Synchronized fun install() {
        val r = _state.value.release ?: return
        if (_state.value.rideActive || task?.isActive == true || _state.value.stage != UpdateStage.READY) return
        task = scope.launch {
            try {
                verifyApk(readyFile,r)
                currentCoroutineContext().ensureActive()
                withContext(Dispatchers.Main) {
                    if (_state.value.rideActive) return@withContext
                    if (Build.VERSION.SDK_INT >= 26 && !pm.canRequestPackageInstalls()) {
                        _state.update { it.copy(message = L10n.text(R.string.ui_dff903a42502)) }
                        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } else {
                        val uri = FileProvider.getUriForFile(context,"${context.packageName}.updates",readyFile)
                        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _state.update { it.copy(stage = UpdateStage.ERROR,message = L10n.text(R.string.ui_6cdea9012f31, friendly(e))) }
            }
        }
    }

    private fun verifyApk(file: File, r: AppRelease) {
        check(file.isFile && file.length() == r.size) { L10n.text(R.string.ui_07c83518a013) }
        val hash = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val b = ByteArray(65536); while(true) { val n=input.read(b);if(n<0)break;hash.update(b,0,n) } }
        check(hash.digest().hex().equals(r.sha256,true)) { L10n.text(R.string.ui_f70ad9baed01) }
        @Suppress("DEPRECATION")
        val info = pm.getPackageArchiveInfo(file.absolutePath,signerFlags()) ?: error(L10n.text(R.string.ui_efeb87144232))
        check(UpdatePolicy.validIdentity(r,info.packageName,context.packageName,PackageInfoCompat.getLongVersionCode(info),
            installedCode,signers(info),signers(installedInfo()))) { L10n.text(R.string.ui_cf90f2b67915) }
        if (Build.VERSION.SDK_INT >= 24) check((info.applicationInfo?.minSdkVersion ?: Int.MAX_VALUE) <= Build.VERSION.SDK_INT) { L10n.text(R.string.ui_619603afa25f) }
    }
    @Suppress("DEPRECATION")
    private fun signerFlags() = if(Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): Set<String> =
        (if(Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures)
            ?.map { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).hex() }?.toSet() ?: emptySet()
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it.toInt() and 255) }

    private suspend fun getSmallJson(url: String, maximum: Int, allow404: Boolean = false): JSONObject? {
        val conn = open(url)
        try {
            if(allow404 && conn.responseCode == 404) return null
            check(conn.responseCode == 200) { L10n.text(R.string.ui_52c64fd8e633, conn.responseCode) }
            val bytes = conn.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n=input.read(buffer);if(n<0)break
                    check(out.size()+n <= maximum) { L10n.text(R.string.ui_cf179fe68676) }
                    out.write(buffer,0,n)
                };out.toByteArray()
            }
            return JSONObject(bytes.toString(Charsets.UTF_8))
        } finally { conn.disconnect() }
    }

    private suspend fun open(initial: String): HttpURLConnection {
        var address = initial
        repeat(6) {
            currentCoroutineContext().ensureActive()
            check(UpdatePolicy.trustedDownloadHost(address)) { L10n.text(R.string.ui_01d98ba9c6fe) }
            val url = URL(address)
            @Suppress("DEPRECATION")
            val net = cm.allNetworks.firstOrNull { n -> cm.getNetworkCapabilities(n)?.let {
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } == true }
            val conn = (net?.openConnection(url) ?: url.openConnection()) as HttpURLConnection
            conn.instanceFollowRedirects = false; conn.connectTimeout = 15_000; conn.readTimeout = 30_000
            conn.setRequestProperty("User-Agent","TerraNav/$installedName Android updater")
            conn.setRequestProperty("Accept",if(url.host == "api.github.com") "application/vnd.github+json" else "application/octet-stream")
            val code = try { conn.responseCode } catch(e: Exception) { conn.disconnect();throw e }
            if(code in listOf(301,302,303,307,308)) {
                val next = conn.getHeaderField("Location"); conn.disconnect()
                check(next != null) { L10n.text(R.string.ui_6091d9799ad6) }
                address = URL(url,next).toString()
            } else return conn
        }
        error(L10n.text(R.string.ui_a4a560043d01))
    }

    private fun friendly(e: Exception): String = if(e is IllegalStateException) e.message.orEmpty().take(180)
        else L10n.text(R.string.ui_2da2f3c1b7ae)

    companion object {
        @Volatile private var instance: AppUpdater? = null
        fun get(context: Context): AppUpdater = instance ?: synchronized(this) {
            instance ?: AppUpdater(context.applicationContext).also { instance = it }
        }
    }
}
