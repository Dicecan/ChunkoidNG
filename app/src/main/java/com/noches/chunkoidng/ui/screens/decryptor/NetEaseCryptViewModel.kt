package com.noches.chunkoidng.ui.screens.decryptor

import com.noches.chunkoidng.R
import androidx.compose.ui.res.stringResource

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noches.chunkoidng.core.decryptor.CryptEvent
import com.noches.chunkoidng.core.decryptor.CryptMode
import com.noches.chunkoidng.core.decryptor.WorldCryptManager
import com.noches.chunkoidng.core.world.ArchiveManager
import com.noches.chunkoidng.core.world.HistoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
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

    val processProgress: Int = 0,
    val currentFileText: String = "",
    val statusText: String = "",
    val logs: List<String> = emptyList(),

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
    private val historyManager = HistoryManager(application)

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
                    stagingMessage = getApplication<Application>().getString(R.string.crypt_staging_reading_dir)
                )
            }

            val result = withContext(Dispatchers.IO) {
                try {
                    if (stagingInputDir.exists()) stagingInputDir.deleteRecursively()
                    stagingInputDir.mkdirs()

                    val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(getApplication(), treeUri)
                        ?: return@withContext Result.failure(Exception(getApplication<Application>().getString(R.string.archive_error_access_dir)))

                    val failures = copyDocumentDir(docFile, stagingInputDir)
                    if (failures.isNotEmpty()) {
                        return@withContext Result.failure(Exception(getApplication<Application>().getString(R.string.decrypt_err_export_fail, failures.take(3).joinToString())))
                    }
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
                            errorMessage = getApplication<Application>().getString(R.string.crypt_error_dir_read, err.message ?: "")
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
                    stagingMessage = getApplication<Application>().getString(R.string.crypt_staging_extracting)
                )
            }

            val result = archiveManager.extractArchive(archiveUri) { progress, status ->
                _uiState.update { it.copy(stagingProgress = progress, stagingMessage = status) }
            }

            result.fold(
                onSuccess = { worldInfo ->

                    if (stagingInputDir.exists()) stagingInputDir.deleteRecursively()
                    stagingInputDir.mkdirs()
                    archiveManager.inputDir.copyRecursively(stagingInputDir, overwrite = true)
                    startProcessing()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            stage = CryptUiStage.ERROR,
                            errorMessage = getApplication<Application>().getString(R.string.crypt_error_archive_load, err.message ?: "")
                        )
                    }
                }
            )
        }
    }

    private fun startProcessing() {
        val s = _uiState.value
        val modeTitle = getApplication<Application>().getString(s.mode.nameRes)
        _uiState.update {
            it.copy(
                stage = CryptUiStage.PROCESSING,
                processProgress = 0,
                currentFileText = "",
                statusText = getApplication<Application>().getString(R.string.crypt_status_init_engine),
                logs = listOf(getApplication<Application>().getString(R.string.crypt_log_init_task, modeTitle))
            )
        }

        viewModelScope.launch {
            try {
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
                                keyHex = "",
                                filesProcessed = event.filesProcessed,
                                ldbVerified = event.ldbVerified,
                                statusText = getApplication<Application>().getString(R.string.crypt_status_success, modeTitle)
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
            } catch (e: Exception) {
                _uiState.update { it.copy(stage = CryptUiStage.ERROR, errorMessage = getApplication<Application>().getString(R.string.crypt_error_failed, e.message ?: "")) }
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
                        ?: return@withContext Result.failure(Exception(getApplication<Application>().getString(R.string.archive_error_access_dir)))

                    val outputDir = cryptManager.workspaceCryptDir
                    if (!outputDir.isDirectory || outputDir.listFiles().isNullOrEmpty()) {
                        return@withContext Result.failure(Exception(getApplication<Application>().getString(R.string.crypt_error_output_empty)))
                    }
                    if (packAsArchive) {
                        val extension = if (s.mode == CryptMode.PASSIVE_ENCRYPT) ".zip" else ".mcworld"
                        val mimeType = if (extension == ".mcworld") "application/octet-stream" else "application/zip"
                        val docFile = treeDoc.createFile(mimeType, "$safeName$extension")
                            ?: return@withContext Result.failure(Exception(getApplication<Application>().getString(R.string.crypt_error_create_archive)))

                        try {
                            val streamSuccess = getApplication<Application>().contentResolver.openOutputStream(docFile.uri)?.use { out ->
                                ZipOutputStream(BufferedOutputStream(out, 64 * 1024)).use { zos ->
                                    zipDirectory(outputDir, outputDir, zos)
                                }
                                true
                            } ?: false

                            if (!streamSuccess) {
                                docFile.delete()
                                return@withContext Result.failure(Exception(getApplication<Application>().getString(R.string.decrypt_err_export)))
                            }
                            Result.success(docFile.uri)
                        } catch (e: Exception) {
                            docFile.delete()
                            throw e
                        }
                    } else {
                        val destDir = treeDoc.createDirectory(safeName)
                            ?: return@withContext Result.failure(Exception(getApplication<Application>().getString(R.string.decrypt_err_target_dir)))
                        val failures = copyToDocumentDir(outputDir, destDir)
                        if (failures.isNotEmpty()) {
                            destDir.delete()
                            return@withContext Result.failure(Exception(getApplication<Application>().getString(R.string.decrypt_err_export_fail, failures.take(3).joinToString())))
                        }
                        Result.success(destDir.uri)
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            _uiState.update { it.copy(isExporting = false) }
            if (result.isSuccess) {
                val uri = result.getOrThrow()
                val state = _uiState.value
                val historyId = withContext(Dispatchers.IO) {
                    historyManager.addRecord(
                        worldName = state.worldName.ifBlank { "Minecraft_World" },
                        sourcePlatform = getApplication<Application>().getString(R.string.decrypt_platform_source),
                        targetPlatform = if (state.mode == CryptMode.DECRYPT) getApplication<Application>().getString(R.string.decrypt_platform_decrypted) else getApplication<Application>().getString(R.string.decrypt_platform_encrypted),
                        durationMs = state.durationMs,
                        icon = null
                    )
                }
                withContext(Dispatchers.IO) { historyManager.updateExportLocation(historyId, uri.toString()) }
                onDone(uri)
            } else {
                val err = result.exceptionOrNull() ?: Exception(getApplication<Application>().getString(R.string.decrypt_err_unknown))
                _uiState.update { it.copy(errorMessage = getApplication<Application>().getString(R.string.decrypt_err_export_failed, err.message)) }
            }
        }
    }

    fun resetState() {
        _uiState.update {
            CryptUiState(mode = it.mode)
        }
    }

    private fun copyDocumentDir(source: androidx.documentfile.provider.DocumentFile, target: File): List<String> {
        val failures = mutableListOf<String>()
        target.mkdirs()
        source.listFiles().forEach { child ->
            val destChild = File(target, child.name ?: "unknown")
            if (child.isDirectory) {
                failures += copyDocumentDir(child, destChild)
            } else if (child.isFile) {
                try {
                    val input = getApplication<Application>().contentResolver.openInputStream(child.uri)
                        ?: throw java.io.IOException(getApplication<Application>().getString(R.string.decrypt_err_input_stream))
                    input.use { inStream ->
                        FileOutputStream(destChild).use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                } catch (e: Exception) {
                    destChild.delete()
                    failures += child.uri.toString()
                }
            }
        }
        return failures
    }

    private fun copyToDocumentDir(source: File, targetDoc: androidx.documentfile.provider.DocumentFile): List<String> {
        val failures = mutableListOf<String>()
        val files = source.listFiles() ?: return listOf(source.absolutePath)
        for (file in files) {
            if (file.isDirectory) {
                val subDirDoc = targetDoc.createDirectory(file.name)
                if (subDirDoc == null) failures += file.absolutePath else failures += copyToDocumentDir(file, subDirDoc)
            } else {
                try {
                    val newFile = targetDoc.createFile("application/octet-stream", file.name)
                        ?: throw java.io.IOException(getApplication<Application>().getString(R.string.decrypt_err_create_file))
                    val out = getApplication<Application>().contentResolver.openOutputStream(newFile.uri)
                        ?: throw java.io.IOException(getApplication<Application>().getString(R.string.decrypt_err_output_stream))
                    out.use { stream ->
                        file.inputStream().use { input -> input.copyTo(stream) }
                    }
                } catch (e: Exception) { failures += file.absolutePath }
            }
        }
        return failures
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
