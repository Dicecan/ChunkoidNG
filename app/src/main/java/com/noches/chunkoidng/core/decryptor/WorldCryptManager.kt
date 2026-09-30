package com.noches.chunkoidng.core.decryptor

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

import androidx.annotation.StringRes
import com.noches.chunkoidng.R

enum class CryptMode(val displayName: String, @StringRes val nameRes: Int) {
    DECRYPT("Decrypt", R.string.decryptor_mode_decrypt),
    PASSIVE_ENCRYPT("Passive Encrypt", R.string.decryptor_mode_encrypt)
}

sealed class CryptEvent {
    data class Progress(val percent: Int, val currentFile: String, val message: String) : CryptEvent()
    data class LogOutput(val line: String) : CryptEvent()
    data class Success(
        val outputDir: File,
        val worldName: String,
        val mode: CryptMode,
        val keyHex: String,
        val filesProcessed: Int,
        val ldbVerified: Boolean,
        val durationMs: Long
    ) : CryptEvent()
    data class Failure(val error: String) : CryptEvent()
}

class WorldCryptManager(private val context: Context) {

    val workspaceCryptDir: File
        get() = File(context.filesDir, "workspace/crypt_output").apply { mkdirs() }

    fun processWorldDirectory(
        sourceDir: File,
        mode: CryptMode,
        customKey: String? = null
    ): Flow<CryptEvent> = flow {
        val startTime = System.currentTimeMillis()

        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            emit(CryptEvent.Failure(context.getString(R.string.crypt_error_dir_not_exist, sourceDir.absolutePath)))
            return@flow
        }

        emit(CryptEvent.Progress(5, "", context.getString(R.string.crypt_status_verifying_integrity)))
        emit(CryptEvent.LogOutput(context.getString(R.string.crypt_log_scanning_dir, sourceDir.name)))

        val dbDir = File(sourceDir, "db")
        if (!dbDir.exists() || !dbDir.isDirectory) {
            emit(CryptEvent.Failure(context.getString(R.string.crypt_error_missing_db)))
            return@flow
        }

        val dbFiles = dbDir.listFiles()?.filter { it.isFile } ?: emptyList()
        val currentFile = dbFiles.find { it.name.equals("CURRENT", ignoreCase = true) }
        val manifestFiles = dbFiles.filter { it.name.matches(Regex("MANIFEST-\\d+", RegexOption.IGNORE_CASE)) }

        if (currentFile == null || manifestFiles.isEmpty()) {
            emit(CryptEvent.Failure(context.getString(R.string.crypt_error_missing_pointers)))
            return@flow
        }

        val key = try {
            if (!customKey.isNullOrBlank()) {
                customKey.trim().toByteArray(StandardCharsets.US_ASCII).also {
                    require(it.isNotEmpty() && it.size <= 1024) { context.getString(R.string.crypt_error_custom_key_invalid) }
                }
            } else if (mode == CryptMode.DECRYPT) {
                val currentBytes = currentFile.readBytes()
                val derived = manifestFiles.asSequence().mapNotNull { manifest ->
                    runCatching { NetEaseCryptor.deriveKey(currentBytes, manifest.name) }.getOrNull()
                }.firstOrNull() ?: throw IllegalArgumentException(context.getString(R.string.crypt_error_manifest_match))
                emit(CryptEvent.LogOutput(context.getString(R.string.crypt_log_derived_key, derived.size)))
                derived
            } else {
                emit(CryptEvent.LogOutput(context.getString(R.string.crypt_log_default_key)))
                NetEaseCryptor.DEFAULT_KEY
            }
        } catch (e: Exception) {
            emit(CryptEvent.Failure(context.getString(R.string.crypt_error_key_validation, e.message ?: "")))
            return@flow
        }

        val taskDir = File(context.filesDir, "workspace/crypt_output.${System.currentTimeMillis()}").apply { mkdirs() }
        val targetDbDir = File(taskDir, "db").apply { mkdirs() }

        emit(CryptEvent.Progress(15, "", context.getString(R.string.crypt_status_staging_metadata)))
        emit(CryptEvent.LogOutput(context.getString(R.string.crypt_log_syncing_files)))

        var worldName = sourceDir.name
        sourceDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                if (file.name.equals("levelname.txt", ignoreCase = true)) {
                    val name = file.readText().trim()
                    if (name.isNotBlank()) worldName = name
                }
                val destFile = File(taskDir, file.name)
                file.copyTo(destFile, overwrite = true)
            } else if (file.isDirectory && !file.name.equals("db", ignoreCase = true)) {
                file.copyRecursively(File(taskDir, file.name), overwrite = true)
            }
        }

        emit(CryptEvent.Progress(25, "", context.getString(R.string.crypt_status_processing_leveldb)))
        val totalDbFiles = dbFiles.size
        var processedCount = 0
        var ldbVerified = true

        for ((index, file) in dbFiles.withIndex()) {
            val fileName = file.name
            val destFile = File(targetDbDir, fileName)
            if (!currentCoroutineContext().isActive) {
                taskDir.deleteRecursively()
                emit(CryptEvent.Failure(context.getString(R.string.crypt_error_cancelled)))
                return@flow
            }
            val isDbTable = fileName.endsWith(".ldb", ignoreCase = true) ||
                    fileName.equals("CURRENT", ignoreCase = true) ||
                    fileName.matches(Regex("MANIFEST-\\d+", RegexOption.IGNORE_CASE)) ||
                    fileName.endsWith(".log", ignoreCase = true)
            val wasEncrypted = if (isDbTable) {
                FileOutputStream(destFile).use { out ->
                    file.inputStream().use { input ->
                        NetEaseCryptor.processFile(input, out, mode == CryptMode.DECRYPT, key)
                    }
                }
            } else {
                file.copyTo(destFile, overwrite = true)
                false
            }
            if (mode == CryptMode.DECRYPT && fileName.endsWith(".ldb", ignoreCase = true)) {
                if (!wasEncrypted || !NetEaseCryptor.verifyLdbFooter(destFile.readBytes())) ldbVerified = false
            }
            processedCount++

            val progressPercent = (25 + ((index + 1).toFloat() / totalDbFiles * 70)).toInt().coerceIn(25, 95)
            emit(CryptEvent.Progress(progressPercent, fileName, context.getString(R.string.crypt_status_processing_file, fileName)))
            if (index % 5 == 0 || index == totalDbFiles - 1) {
                emit(CryptEvent.LogOutput(context.getString(R.string.crypt_log_processed_file, processedCount, totalDbFiles, fileName)))
            }
        }

        if (dbFiles.none { it.name.endsWith(".ldb", ignoreCase = true) } || !ldbVerified) {
            taskDir.deleteRecursively()
            emit(CryptEvent.Failure(context.getString(R.string.crypt_error_verify_failed)))
            return@flow
        }
        val duration = System.currentTimeMillis() - startTime
        try {
            if (workspaceCryptDir.exists()) workspaceCryptDir.deleteRecursively()
            check(taskDir.renameTo(workspaceCryptDir)) { context.getString(R.string.crypt_error_commit_output, "") }
        } catch (e: Exception) {
            taskDir.deleteRecursively()
            emit(CryptEvent.Failure(context.getString(R.string.crypt_error_commit_output, e.message ?: "")))
            return@flow
        }
        val modeTitle = context.getString(mode.nameRes)
        emit(CryptEvent.Progress(100, "", context.getString(R.string.crypt_status_mode_completed, modeTitle)))
        emit(CryptEvent.LogOutput(context.getString(R.string.crypt_log_success_detail, duration / 1000.0, processedCount)))

        if (mode == CryptMode.DECRYPT && ldbVerified) {
            emit(CryptEvent.LogOutput(context.getString(R.string.crypt_log_magic_verified)))
        }

        emit(
            CryptEvent.Success(
                outputDir = workspaceCryptDir,
                worldName = worldName,
                mode = mode,
                keyHex = NetEaseCryptor.keyToHexString(key),
                filesProcessed = processedCount,
                ldbVerified = ldbVerified,
                durationMs = duration
            )
        )
    }.flowOn(Dispatchers.IO)
}
