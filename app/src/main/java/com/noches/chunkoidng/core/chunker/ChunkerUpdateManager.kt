package com.noches.chunkoidng.core.chunker

import android.content.Context
import com.noches.chunkoidng.core.conversion.ChunkerFormat
import com.noches.chunkoidng.core.world.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class ChunkerReleaseInfo(
    val version: String,
    val tagName: String,
    val downloadUrl: String,
    val fileSize: Long,
    val releaseNotes: String,
    val publishedAt: String,
    val newlySupportedVersions: List<ChunkerFormat> = emptyList()
)

sealed interface ChunkerUpdateStatus {
    object Idle : ChunkerUpdateStatus
    object Checking : ChunkerUpdateStatus
    data class UpdateAvailable(val info: ChunkerReleaseInfo) : ChunkerUpdateStatus
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : ChunkerUpdateStatus
    object Installing : ChunkerUpdateStatus
    data class UpToDate(val currentVersion: String) : ChunkerUpdateStatus
    data class Error(val message: String) : ChunkerUpdateStatus
}

object ChunkerUpdateManager {
    private const val PREFS_NAME = "chunker_engine_prefs"
    private const val KEY_INSTALLED_VERSION = "installed_chunker_version"
    private const val KEY_DYNAMIC_FORMATS = "dynamic_formats_json"
    private const val DEFAULT_VERSION = "1.19.1"
    private const val GITHUB_API_URL = "https://api.github.com/repos/HiveGamesOSS/Chunker/releases"
    private const val GITHUB_MIRROR_URL = "https://ghproxy.net/https://api.github.com/repos/HiveGamesOSS/Chunker/releases"

    private val _statusFlow = MutableStateFlow<ChunkerUpdateStatus>(ChunkerUpdateStatus.Idle)
    val statusFlow: StateFlow<ChunkerUpdateStatus> = _statusFlow.asStateFlow()

    fun getCurrentVersion(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_INSTALLED_VERSION, DEFAULT_VERSION) ?: DEFAULT_VERSION
    }

    fun init(context: Context) {
        loadPersistedDynamicFormats(context)
    }

    fun checkUpdateFlow(context: Context, force: Boolean = false): Flow<ChunkerUpdateStatus> = flow {
        emit(ChunkerUpdateStatus.Checking)
        _statusFlow.value = ChunkerUpdateStatus.Checking

        val currentVersion = getCurrentVersion(context)
        val result = fetchLatestRelease(context, currentVersion)
        _statusFlow.value = result
        emit(result)
    }.flowOn(Dispatchers.IO)

    suspend fun checkSilently(context: Context) = withContext(Dispatchers.IO) {
        if (_statusFlow.value is ChunkerUpdateStatus.Checking || _statusFlow.value is ChunkerUpdateStatus.Downloading) {
            return@withContext
        }
        val currentVersion = getCurrentVersion(context)
        val result = fetchLatestRelease(context, currentVersion)
        _statusFlow.value = result
    }

    private fun fetchLatestRelease(context: Context, currentVersion: String): ChunkerUpdateStatus {
        val endpoints = listOf(GITHUB_API_URL, GITHUB_MIRROR_URL)
        var lastError: Exception? = null

        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "ChunkoidNG")
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                }

                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val releases = JSONArray(body)
                    for (i in 0 until releases.length()) {
                        val rel = releases.getJSONObject(i)
                        if (rel.optBoolean("draft", false) || rel.optBoolean("prerelease", false)) continue

                        val rawTag = rel.optString("tag_name", "").trim()
                        val versionStr = rawTag.removePrefix("v").removePrefix("V")
                        if (versionStr.isBlank()) continue

                        val assets = rel.optJSONArray("assets") ?: JSONArray()
                        var jarUrl = ""
                        var jarSize = 0L
                        for (j in 0 until assets.length()) {
                            val asset = assets.getJSONObject(j)
                            val name = asset.optString("name", "")
                            if (name.startsWith("chunker-cli") && name.endsWith(".jar")) {
                                jarUrl = asset.optString("browser_download_url", "")
                                jarSize = asset.optLong("size", 0L)
                                break
                            }
                        }

                        if (jarUrl.isBlank()) continue

                        val bodyText = rel.optString("body", "")
                        val publishedAt = rel.optString("published_at", "")
                        val newlyDiscovered = parseSupportedFormatsFromBody(bodyText, versionStr)

                        return if (isNewerVersion(versionStr, currentVersion)) {
                            ChunkerUpdateStatus.UpdateAvailable(
                                ChunkerReleaseInfo(
                                    version = versionStr,
                                    tagName = rawTag,
                                    downloadUrl = jarUrl,
                                    fileSize = jarSize,
                                    releaseNotes = bodyText,
                                    publishedAt = publishedAt,
                                    newlySupportedVersions = newlyDiscovered
                                )
                            )
                        } else {
                            ChunkerUpdateStatus.UpToDate(currentVersion)
                        }
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        return ChunkerUpdateStatus.Error(lastError?.localizedMessage ?: "Failed to query Chunker repository")
    }

    suspend fun downloadAndInstall(
        context: Context,
        info: ChunkerReleaseInfo,
        onProgress: (Float, Long, Long) -> Unit = { _, _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        _statusFlow.value = ChunkerUpdateStatus.Downloading(0f, 0L, info.fileSize)
        val downloadFile = File(context.filesDir, "cli.jar.download")
        val oldFile = File(context.filesDir, "cli.jar.old")
        val targetFile = File(context.filesDir, "cli.jar")

        try {
            cleanupTempFiles(context)
            if (downloadFile.exists()) downloadFile.delete()

            val success = downloadWithFallback(info.downloadUrl, downloadFile, info.fileSize) { prog, curr, total ->
                _statusFlow.value = ChunkerUpdateStatus.Downloading(prog, curr, total)
                onProgress(prog, curr, total)
            }

            if (!success || !verifyJarFile(downloadFile)) {
                if (downloadFile.exists()) downloadFile.delete()
                _statusFlow.value = ChunkerUpdateStatus.Error("Downloaded file verification failed")
                return@withContext false
            }

            _statusFlow.value = ChunkerUpdateStatus.Installing

            if (oldFile.exists()) oldFile.delete()
            if (targetFile.exists()) {
                targetFile.renameTo(oldFile)
            }

            val moved = downloadFile.renameTo(targetFile)
            if (!moved) {
                downloadFile.copyTo(targetFile, overwrite = true)
                downloadFile.delete()
            }

            if (oldFile.exists()) {
                oldFile.delete()
            }

            cleanupTempFiles(context)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_INSTALLED_VERSION, info.version).apply()

            if (info.newlySupportedVersions.isNotEmpty()) {
                ChunkerFormat.registerDynamicFormats(info.newlySupportedVersions)
                persistDynamicFormats(context, ChunkerFormat.getAllDynamicFormats())
            }

            _statusFlow.value = ChunkerUpdateStatus.UpToDate(info.version)
            true
        } catch (e: Exception) {
            if (downloadFile.exists()) downloadFile.delete()
            if (!targetFile.exists() && oldFile.exists()) {
                oldFile.renameTo(targetFile)
            }
            _statusFlow.value = ChunkerUpdateStatus.Error(e.localizedMessage ?: "Installation failed")
            false
        }
    }

    private fun cleanupTempFiles(context: Context) {
        val names = listOf("cli.jar.download", "cli.jar.old", "cli.jar.tmp", "cli.jar.bak")
        for (name in names) {
            val f = File(context.filesDir, name)
            if (f.exists()) f.delete()
        }
        val cacheDir = context.cacheDir
        cacheDir.listFiles()?.forEach { f ->
            if (f.name.startsWith("cli") && f.name.endsWith(".tmp")) {
                f.delete()
            }
        }
    }

    private fun verifyJarFile(file: File): Boolean {
        if (!file.exists() || file.length() < 5 * 1024 * 1024) return false
        return try {
            FileInputStream(file).use { input ->
                val magic = ByteArray(4)
                if (input.read(magic) != 4) return false
                magic[0] == 0x50.toByte() && magic[1] == 0x4B.toByte() &&
                        magic[2] == 0x03.toByte() && magic[3] == 0x04.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun downloadWithFallback(
        primaryUrl: String,
        targetFile: File,
        expectedSize: Long,
        onProgress: (Float, Long, Long) -> Unit
    ): Boolean {
        val mirrorUrl = "https://ghfast.top/$primaryUrl"
        val candidates = listOf(primaryUrl, mirrorUrl)

        for (candidate in candidates) {
            try {
                val url = URL(candidate)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "ChunkoidNG")
                }

                var currentCode = conn.responseCode
                var activeConn = conn
                var redirects = 0
                while (currentCode in listOf(301, 302, 303, 307, 308) && redirects < 5) {
                    val newUrl = activeConn.getHeaderField("Location")
                    activeConn.disconnect()
                    activeConn = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 30000
                        setRequestProperty("User-Agent", "ChunkoidNG")
                    }
                    currentCode = activeConn.responseCode
                    redirects++
                }

                if (currentCode in 200..299) {
                    val total = if (activeConn.contentLengthLong > 0) activeConn.contentLengthLong else expectedSize
                    var downloaded = 0L

                    activeConn.inputStream.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            val buffer = ByteArray(65536)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloaded += read
                                val prog = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
                                onProgress(prog, downloaded, total)
                            }
                            output.flush()
                        }
                    }
                    if (targetFile.exists() && targetFile.length() > 5 * 1024 * 1024) {
                        return true
                    }
                }
            } catch (e: Exception) {
                if (targetFile.exists()) targetFile.delete()
            }
        }
        return false
    }

    fun isNewerVersion(remote: String, local: String): Boolean {
        val rParts = remote.split("-", "_")[0].split(".").mapNotNull { it.toIntOrNull() }
        val lParts = local.split("-", "_")[0].split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    fun parseSupportedFormatsFromBody(body: String, version: String): List<ChunkerFormat> {
        val results = mutableListOf<ChunkerFormat>()
        val bedrockPattern = Regex("""(?i)bedrock\s+([0-9]+\.[0-9]+(?:\.[0-9]+)?)""")
        bedrockPattern.findAll(body).forEach { match ->
            val v = match.groupValues[1]
            val id = "BEDROCK_" + v.replace('.', '_')
            results.add(ChunkerFormat(id, Platform.BEDROCK, "Adapted $v", "Bedrock $v", true))
        }

        val javaPattern = Regex("""(?i)java\s+([0-9]+\.[0-9]+(?:\.[0-9]+)?)""")
        javaPattern.findAll(body).forEach { match ->
            val v = match.groupValues[1]
            val id = "JAVA_" + v.replace('.', '_')
            results.add(ChunkerFormat(id, Platform.JAVA, "Adapted $v", "Java $v", true))
        }

        if (results.isEmpty() && version.isNotBlank()) {
            val parts = version.split(".")
            if (parts.size >= 2) {
                val majorMinor = "${parts[0]}.${parts[1]}"
                val idB = "BEDROCK_" + version.replace('.', '_')
                val idJ = "JAVA_" + version.replace('.', '_')
                results.add(ChunkerFormat(idB, Platform.BEDROCK, "$majorMinor.x", "Bedrock $version", true))
                results.add(ChunkerFormat(idJ, Platform.JAVA, "$majorMinor.x", "Java $version", true))
            }
        }
        return results
    }

    private fun persistDynamicFormats(context: Context, formats: List<ChunkerFormat>) {
        val array = JSONArray()
        for (f in formats) {
            val obj = JSONObject().apply {
                put("id", f.id)
                put("platform", f.platform.name)
                put("group", f.group)
                put("displayName", f.displayName)
                put("isPopular", f.isPopular)
            }
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DYNAMIC_FORMATS, array.toString())
            .apply()
    }

    private fun loadPersistedDynamicFormats(context: Context) {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DYNAMIC_FORMATS, null) ?: return
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<ChunkerFormat>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id")
                val platform = Platform.valueOf(obj.optString("platform", Platform.BEDROCK.name))
                val group = obj.optString("group")
                val name = obj.optString("displayName")
                val pop = obj.optBoolean("isPopular", false)
                if (id.isNotBlank() && name.isNotBlank()) {
                    list.add(ChunkerFormat(id, platform, group, name, pop))
                }
            }
            if (list.isNotEmpty()) {
                ChunkerFormat.registerDynamicFormats(list)
            }
        } catch (e: Exception) {
        }
    }
}
