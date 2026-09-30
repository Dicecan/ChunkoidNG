package com.noches.chunkoidng.ui.screens.decryptor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noches.chunkoidng.core.decryptor.CryptEvent
import com.noches.chunkoidng.core.decryptor.CryptMode
import com.noches.chunkoidng.core.decryptor.WorldCryptManager
import com.noches.chunkoidng.core.world.ArchiveManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class CryptUiStage {
    SELECT_SOURCE,
    STAGING,
    PROCESSING,
    COMPLETED,
    ERROR
}

data class CryptUiState(
    val stage: CryptUiStage = CryptUiStage.SELECT_SOURCE,
    val mode: CryptMode = CryptMode.DECRYPT,
    val stagingProgress: Int = 0,
    val stagingMessage: String = "",
    val customKey: String = "",
    // In progress
    val processProgress: Int = 0,
    val currentFileText: String = "",
    val statusText: String = "",
    val logs: List<String> = emptyList(),
    // Result
    val worldName: String = "",
    val durationMs: Long = 0,
    val keyHex: String = "",
    val filesProcessed: Int = 0,
    val ldbVerified: Boolean = true,
    val isExporting: Boolean = false,
    val errorMessage: String? = null
)

class NetEaseCryptViewModel(application: Application) : AndroidViewModel(application) {

    private val cryptManager = WorldCryptManager(application)
    private val archiveManager = ArchiveManager(application)

    private val stagingInputDir: File
        get() = File(getApplication<Application>().filesDir, "workspace/crypt_input").apply { mkdirs() }

    private val _uiState = MutableStateFlow(CryptUiState())
    val uiState: StateFlow<CryptUiState> = _uiState.asStateFlow()

    fun setMode(mode: CryptMode) {
        if (_uiState.value.stage == CryptUiStage.SELECT_SOURCE) {
            _uiState.update { it.copy(mode = mode) }
        }
    }

    fun setCustomKey(key: String) {
        _uiState.update { it.copy(customKey = key) }
    }

    fun handleFolderSelected(treeUri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = CryptUiStage.STAGING,
                    stagingProgress = 0,
                    stagingMessage = "正在读取存档目录结构..."
                )
            }

            val result = withContext(Dispatchers.IO) {
                try {
                    if (stagingInputDir.exists()) stagingInputDir.deleteRecursively()
                    stagingInputDir.mkdirs()

                    val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(getApplication(), treeUri)
                        ?: return@withContext Result.failure(Exception("无法访问所选目录"))

                    copyDocumentDir(docFile, stagingInputDir)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            result.fold(
                onSuccess = {
                    startProcessing()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            stage = CryptUiStage.ERROR,
                            errorMessage = "读取目录失败: ${err.message}"
                        )
                    }
                }
            )
        }
    }

    fun handleArchiveSelected(archiveUri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = CryptUiStage.STAGING,
                    stagingProgress = 0,
                    stagingMessage = "正在解压待处理归档文件..."
                )
            }

            val result = archiveManager.extractArchive(archiveUri) { progress, status ->
                _uiState.update { it.copy(stagingProgress = progress, stagingMessage = status) }
            }

            result.fold(
                onSuccess = { worldInfo ->
                    // archiveManager extracts into archiveManager.inputDir
                    if (stagingInputDir.exists()) stagingInputDir.deleteRecursively()
                    stagingInputDir.mkdirs()
                    archiveManager.inputDir.copyRecursively(stagingInputDir, overwrite = true)
                    startProcessing()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            stage = CryptUiStage.ERROR,
                            errorMessage = "归档加载失败: ${err.message}"
                        )
                    }
                }
            )
        }
    }

    private fun startProcessing() {
        val s = _uiState.value
        _uiState.update {
            it.copy(
                stage = CryptUiStage.PROCESSING,
                processProgress = 0,
                currentFileText = "",
                statusText = "正在初始化加解密引擎...",
                logs = listOf("[INIT] 准备执行${s.mode.displayName}任务...")
            )
        }

        viewModelScope.launch {
            cryptManager.processWorldDirectory(
                sourceDir = stagingInputDir,
                mode = s.mode,
                customKey = s.customKey.ifBlank { null }
            ).collect { event ->
                when (event) {
                    is CryptEvent.Progress -> {
                        _uiState.update {
                            it.copy(
                                processProgress = event.percent,
                                currentFileText = event.currentFile,
                                statusText = event.message
                            )
                        }
                    }
                    is CryptEvent.LogOutput -> {
                        _uiState.update {
                            it.copy(logs = (it.logs + event.line).takeLast(600))
                        }
                    }
                    is CryptEvent.Success -> {
                        _uiState.update {
                            it.copy(
                                stage = CryptUiStage.COMPLETED,
                                processProgress = 100,
                                worldName = event.worldName,
                                durationMs = event.durationMs,
                                keyHex = event.keyHex,
                                filesProcessed = event.filesProcessed,
                                ldbVerified = event.ldbVerified,
                                statusText = "${s.mode.displayName}成功！"
                            )
                        }
                    }
                    is CryptEvent.Failure -> {
                        _uiState.update {
                            it.copy(
                                stage = CryptUiStage.ERROR,
                                errorMessage = event.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun prepareHandoffToConverter(onReady: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val outputDir = cryptManager.workspaceCryptDir
            if (outputDir.exists()) {
                val converterInput = archiveManager.inputDir
                if (converterInput.exists()) converterInput.deleteRecursively()
                converterInput.mkdirs()
                outputDir.copyRecursively(converterInput, overwrite = true)
            }
            withContext(Dispatchers.Main) {
                onReady()
            }
        }
    }

    fun exportResultWorld(
        targetTreeUri: Uri,
        packAsArchive: Boolean,
        onDone: (Uri) -> Unit
    ) {
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            val s = _uiState.value
            val isBedrock = true
            val suffix = if (s.mode == CryptMode.DECRYPT) "_Decrypted" else "_Encrypted"
            val safeName = s.worldName.ifBlank { "Minecraft_World" } + suffix

            val result = withContext(Dispatchers.IO) {
                try {
                    val treeDoc = androidx.documentfile.provider.DocumentFile.fromTreeUri(getApplication(), targetTreeUri)
                        ?: return@withContext Result.failure(Exception("无法访问导出位置"))

                    val outputDir = cryptManager.workspaceCryptDir
                    if (packAsArchive) {
                        val extension = if (s.mode == CryptMode.PASSIVE_ENCRYPT) ".zip" else ".mcworld"
                        val docFile = treeDoc.createFile("application/zip", "$safeName$extension")
                            ?: return@withContext Result.failure(Exception("无法创建目标归档"))

                        getApplication<Application>().contentResolver.openOutputStream(docFile.uri)?.use { out ->
                            ZipOutputStream(BufferedOutputStream(out, 64 * 1024)).use { zos ->
                                zipDirectory(outputDir, outputDir, zos)
                            }
                        }
                        Result.success(docFile.uri)
                    } else {
                        val destDir = treeDoc.createDirectory(safeName)
                            ?: return@withContext Result.failure(Exception("无法创建目标目录"))
                        copyToDocumentDir(outputDir, destDir)
                        Result.success(destDir.uri)
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            _uiState.update { it.copy(isExporting = false) }
            result.onSuccess { uri ->
                onDone(uri)
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = "导出失败: ${err.message}") }
            }
        }
    }

    fun resetState() {
        _uiState.update {
            CryptUiState(mode = it.mode)
        }
    }

    private fun copyDocumentDir(source: androidx.documentfile.provider.DocumentFile, target: File) {
        target.mkdirs()
        source.listFiles().forEach { child ->
            val destChild = File(target, child.name ?: "unknown")
            if (child.isDirectory) {
                copyDocumentDir(child, destChild)
            } else if (child.isFile) {
                getApplication<Application>().contentResolver.openInputStream(child.uri)?.use { inStream ->
                    FileOutputStream(destChild).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
            }
        }
    }

    private fun copyToDocumentDir(source: File, targetDoc: androidx.documentfile.provider.DocumentFile) {
        val files = source.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                val subDirDoc = targetDoc.createDirectory(file.name) ?: continue
                copyToDocumentDir(file, subDirDoc)
            } else {
                val newFile = targetDoc.createFile("application/octet-stream", file.name) ?: continue
                getApplication<Application>().contentResolver.openOutputStream(newFile.uri)?.use { out ->
                    file.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
            }
        }
    }

    private fun zipDirectory(rootDir: File, currentDir: File, zos: ZipOutputStream) {
        val files = currentDir.listFiles() ?: return
        val buffer = ByteArray(64 * 1024)
        for (file in files) {
            if (file.isDirectory) {
                val relPath = file.relativeTo(rootDir).path.replace('\\', '/') + "/"
                zos.putNextEntry(ZipEntry(relPath))
                zos.closeEntry()
                zipDirectory(rootDir, file, zos)
            } else {
                val relPath = file.relativeTo(rootDir).path.replace('\\', '/')
                zos.putNextEntry(ZipEntry(relPath))
                file.inputStream().use { input ->
                    var count: Int
                    while (input.read(buffer).also { count = it } != -1) {
                        zos.write(buffer, 0, count)
                    }
                }
                zos.closeEntry()
            }
        }
    }
}
