package com.noches.chunkoidng.core.decryptor

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

enum class CryptMode(val displayName: String) {
    DECRYPT("网易存档被动解密"),
    PASSIVE_ENCRYPT("网易存档被动加密")
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

/**
 * Manages full Minecraft world decryption and passive encryption pipelines.
 */
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
            emit(CryptEvent.Failure("输入目录不存在或不是有效文件夹: ${sourceDir.absolutePath}"))
            return@flow
        }

        emit(CryptEvent.Progress(5, "", "正在校验存档文件完整性..."))
        emit(CryptEvent.LogOutput("[INFO] 正在扫描存档目录: ${sourceDir.name}"))

        val dbDir = File(sourceDir, "db")
        if (!dbDir.exists() || !dbDir.isDirectory) {
            emit(CryptEvent.Failure("未在存档根目录下检测到 db/ 数据库文件夹"))
            return@flow
        }

        val dbFiles = dbDir.listFiles()?.filter { it.isFile } ?: emptyList()
        val currentFile = dbFiles.find { it.name.equals("CURRENT", ignoreCase = true) }
        val manifestFile = dbFiles.find { it.name.startsWith("MANIFEST", ignoreCase = true) }

        if (currentFile == null || manifestFile == null) {
            emit(CryptEvent.Failure("db 目录下缺失关键指针文件 (CURRENT 或 MANIFEST-*)"))
            return@flow
        }

        // Determine key
        val key: ByteArray = if (!customKey.isNullOrBlank()) {
            customKey.toByteArray(StandardCharsets.US_ASCII)
        } else if (mode == CryptMode.DECRYPT) {
            val currentBytes = currentFile.readBytes()
            val derived = NetEaseCryptor.deriveKey(currentBytes, manifestFile.name)
            emit(CryptEvent.LogOutput("[INFO] 成功推导解密密钥: 0x${NetEaseCryptor.keyToHexString(derived)}"))
            derived
        } else {
            emit(CryptEvent.LogOutput("[INFO] 采用网易被动标准加密密钥: 88329851"))
            NetEaseCryptor.DEFAULT_KEY
        }

        // Prepare clean output directory
        if (workspaceCryptDir.exists()) {
            workspaceCryptDir.deleteRecursively()
        }
        workspaceCryptDir.mkdirs()
        val targetDbDir = File(workspaceCryptDir, "db").apply { mkdirs() }

        emit(CryptEvent.Progress(15, "", "正在转存基本元数据文件..."))
        emit(CryptEvent.LogOutput("[INFO] 正在同步非数据库文件..."))

        // Copy root files (level.dat, world_icon, etc.)
        var worldName = sourceDir.name
        sourceDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                if (file.name.equals("levelname.txt", ignoreCase = true)) {
                    val name = file.readText().trim()
                    if (name.isNotBlank()) worldName = name
                }
                val destFile = File(workspaceCryptDir, file.name)
                file.copyTo(destFile, overwrite = true)
            } else if (file.isDirectory && !file.name.equals("db", ignoreCase = true)) {
                file.copyRecursively(File(workspaceCryptDir, file.name), overwrite = true)
            }
        }

        emit(CryptEvent.Progress(25, "", "开始处理核心 LevelDB 数据库..."))
        val totalDbFiles = dbFiles.size
        var processedCount = 0
        var ldbVerified = true

        for ((index, file) in dbFiles.withIndex()) {
            val fileName = file.name
            val destFile = File(targetDbDir, fileName)
            val fileBytes = file.readBytes()

            val processedBytes = when (mode) {
                CryptMode.DECRYPT -> {
                    val isDbTable = fileName.endsWith(".ldb", ignoreCase = true) ||
                            fileName.equals("CURRENT", ignoreCase = true) ||
                            fileName.startsWith("MANIFEST", ignoreCase = true) ||
                            fileName.endsWith(".log", ignoreCase = true)

                    if (isDbTable && NetEaseCryptor.hasMagicHeader(fileBytes)) {
                        val dec = NetEaseCryptor.decryptData(fileBytes, key)
                        if (fileName.endsWith(".ldb", ignoreCase = true)) {
                            if (!NetEaseCryptor.verifyLdbFooter(dec)) {
                                ldbVerified = false
                            }
                        }
                        dec
                    } else {
                        fileBytes
                    }
                }
                CryptMode.PASSIVE_ENCRYPT -> {
                    val shouldEncrypt = fileName.endsWith(".ldb", ignoreCase = true) ||
                            fileName.equals("CURRENT", ignoreCase = true) ||
                            fileName.startsWith("MANIFEST", ignoreCase = true) ||
                            fileName.endsWith(".log", ignoreCase = true)

                    if (shouldEncrypt && !NetEaseCryptor.hasMagicHeader(fileBytes)) {
                        NetEaseCryptor.encryptData(fileBytes, key)
                    } else {
                        fileBytes
                    }
                }
            }

            destFile.writeBytes(processedBytes)
            processedCount++

            val progressPercent = (25 + ((index + 1).toFloat() / totalDbFiles * 70)).toInt().coerceIn(25, 95)
            emit(CryptEvent.Progress(progressPercent, fileName, "处理: $fileName"))
            if (index % 5 == 0 || index == totalDbFiles - 1) {
                emit(CryptEvent.LogOutput("[PROCESS] 已处理 ($processedCount/$totalDbFiles): $fileName"))
            }
        }

        val duration = System.currentTimeMillis() - startTime
        emit(CryptEvent.Progress(100, "", "${mode.displayName} 完成！"))
        emit(CryptEvent.LogOutput("[SUCCESS] 任务完成，耗时 ${duration / 1000.0} 秒，共处理 $processedCount 个数据库文件"))

        if (mode == CryptMode.DECRYPT && ldbVerified) {
            emit(CryptEvent.LogOutput("[VERIFY] LevelDB SSTable 魔数校验通过 (0x57FB808B247547DB)，数据结构完整可用！"))
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
