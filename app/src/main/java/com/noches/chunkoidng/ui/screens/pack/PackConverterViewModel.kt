package com.noches.chunkoidng.ui.screens.pack

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noches.chunkoidng.core.pack.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipFile

data class PackConverterUiState(
    val selectedPackUri: Uri? = null,
    val selectedPackName: String = "",
    val selectedPackSizeBytes: Long = 0L,
    val detectedPlatform: PackPlatform? = null,
    val sourcePlatform: PackPlatform = PackPlatform.JAVA,
    val targetPlatform: PackPlatform = PackPlatform.BEDROCK,
    val targetBedrockVersion: BedrockEngineVersion = BedrockEngineVersion.V_1_21_0,
    val targetJavaFormat: Int = 34,
    val generateAtlases: Boolean = true,
    val convertAnimations: Boolean = true,
    val convertLang: Boolean = true,
    val convertSounds: Boolean = true,
    val isConverting: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 100,
    val currentStepText: String = "",
    val convertedFile: File? = null,
    val convertedFileSizeBytes: Long = 0L,
    val filesConvertedCount: Int = 0,
    val errorMessage: String? = null
)

class PackConverterViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val MAX_STAGED_INPUT_BYTES = 512L * 1024L * 1024L
        const val MAX_ARCHIVE_ENTRIES_FOR_SCAN = 100_000
        const val MAX_ENTRY_NAME_LENGTH_FOR_SCAN = 512
    }

    private val _uiState = MutableStateFlow(PackConverterUiState())
    val uiState: StateFlow<PackConverterUiState> = _uiState.asStateFlow()

    private var stagedInputFile: File? = null

    fun onPackSelected(uri: Uri, displayName: String, sizeBytes: Long) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedPackUri = uri,
                    selectedPackName = displayName,
                    selectedPackSizeBytes = sizeBytes,
                    isConverting = true,
                    currentStepText = "Staging pack file...",
                    errorMessage = null,
                    convertedFile = null,
                    convertedFileSizeBytes = 0L,
                    filesConvertedCount = 0
                )
            }

            try {
                val file = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val temp = File(context.cacheDir, "pack_input_${System.currentTimeMillis()}.tmp")
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IOException("Unable to open selected pack")
                    try {
                        inputStream.use { input ->
                            FileOutputStream(temp).use { output ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                var total = 0L
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    total += read
                                    if (total > MAX_STAGED_INPUT_BYTES) {
                                        throw IOException("Selected pack is too large")
                                    }
                                    output.write(buffer, 0, read)
                                }
                            }
                        }
                    } catch (cause: Exception) {
                        temp.delete()
                        throw cause
                    }
                    temp
                }
                stagedInputFile?.takeIf { it != file }?.delete()
                stagedInputFile = file

                val detected = withContext(Dispatchers.IO) {
                    val names = mutableListOf<String>()
                    try {
                        ZipFile(file).use { zip ->
                            val entries = zip.entries()
                            var count = 0
                            while (entries.hasMoreElements()) {
                                count++
                                if (count > MAX_ARCHIVE_ENTRIES_FOR_SCAN) {
                                    throw IOException("Archive contains too many entries")
                                }
                                val name = entries.nextElement().name
                                if (name.length > MAX_ENTRY_NAME_LENGTH_FOR_SCAN) {
                                    throw IOException("Archive entry name is too long")
                                }
                                names.add(name)
                            }
                        }
                    } catch (cause: Exception) {
                        throw IOException("Selected file is not a valid ZIP resource pack", cause)
                    }
                    if (names.isEmpty()) throw IOException("Selected resource pack is empty")
                    PackManifestHandler.detectPlatform(names)
                }

                val src = detected ?: PackPlatform.JAVA
                val tgt = if (src == PackPlatform.JAVA) PackPlatform.BEDROCK else PackPlatform.JAVA

                _uiState.update {
                    it.copy(
                        detectedPlatform = detected,
                        sourcePlatform = src,
                        targetPlatform = tgt,
                        isConverting = false,
                        currentStepText = ""
                    )
                }
            } catch (e: Exception) {
                stagedInputFile?.delete()
                stagedInputFile = null
                _uiState.update {
                    it.copy(
                        selectedPackUri = null,
                        selectedPackName = "",
                        selectedPackSizeBytes = 0L,
                        isConverting = false,
                        errorMessage = e.message ?: "Failed to read pack file"
                    )
                }
            }
        }
    }

    fun setDirection(source: PackPlatform, target: PackPlatform) {
        _uiState.update {
            it.copy(sourcePlatform = source, targetPlatform = target)
        }
    }

    fun setTargetBedrockVersion(version: BedrockEngineVersion) {
        _uiState.update { it.copy(targetBedrockVersion = version) }
    }

    fun setTargetJavaFormat(format: Int) {
        _uiState.update { it.copy(targetJavaFormat = format) }
    }

    fun toggleGenerateAtlases(enabled: Boolean) {
        _uiState.update { it.copy(generateAtlases = enabled) }
    }

    fun toggleConvertAnimations(enabled: Boolean) {
        _uiState.update { it.copy(convertAnimations = enabled) }
    }

    fun toggleConvertLang(enabled: Boolean) {
        _uiState.update { it.copy(convertLang = enabled) }
    }

    fun toggleConvertSounds(enabled: Boolean) {
        _uiState.update { it.copy(convertSounds = enabled) }
    }

    fun startConversion() {
        val input = stagedInputFile ?: run {
            _uiState.update { it.copy(errorMessage = "Select a valid resource pack first") }
            return
        }
        val state = _uiState.value
        state.convertedFile?.delete()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConverting = true,
                    progressCurrent = 0,
                    progressTotal = 100,
                    currentStepText = "Extracting pack...",
                    errorMessage = null,
                    convertedFile = null,
                    convertedFileSizeBytes = 0L,
                    filesConvertedCount = 0
                )
            }

            val cacheDir = getApplication<Application>().cacheDir
            val unpackedInput = File(cacheDir, "pack_unpacked_in_${System.currentTimeMillis()}")
            val unpackedOutput = File(cacheDir, "pack_unpacked_out_${System.currentTimeMillis()}")
            var outputFile: File? = null

            try {
                val outputExt = if (state.targetPlatform == PackPlatform.BEDROCK) ".mcpack" else ".zip"
                val baseName = sanitizePackFileName(state.selectedPackName)
                outputFile = File(cacheDir, "${baseName}_converted$outputExt")
                outputFile!!.delete()

                withContext(Dispatchers.IO) {
                    ResourcePackConverterEngine.extractArchive(input, unpackedInput)

                    val config = PackConversionConfig(
                        targetPlatform = state.targetPlatform,
                        targetBedrockVersion = state.targetBedrockVersion,
                        targetJavaFormat = state.targetJavaFormat,
                        generateAtlases = state.generateAtlases,
                        convertAnimations = state.convertAnimations,
                        convertLanguages = state.convertLang,
                        convertSounds = state.convertSounds
                    )

                    ResourcePackConverterEngine.convertPack(
                        sourceDir = unpackedInput,
                        targetDir = unpackedOutput,
                        config = config,
                        onProgress = { progress ->
                            _uiState.update {
                                it.copy(
                                    progressCurrent = progress.current,
                                    progressTotal = progress.total,
                                    currentStepText = progress.log ?: progress.stage
                                )
                            }
                        }
                    )

                    ResourcePackConverterEngine.createArchive(unpackedOutput, outputFile!!)
                }

                val convertedCount = withContext(Dispatchers.IO) {
                    val count = unpackedOutput.walkTopDown().filter { it.isFile }.count()
                    count
                }

                _uiState.update {
                    it.copy(
                        isConverting = false,
                        convertedFile = outputFile,
                        convertedFileSizeBytes = outputFile!!.length(),
                        filesConvertedCount = convertedCount,
                        currentStepText = ""
                    )
                }
            } catch (e: Exception) {
                outputFile?.delete()
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        errorMessage = e.message ?: "Pack conversion failed"
                    )
                }
            } finally {
                withContext(Dispatchers.IO) {
                    unpackedInput.deleteRecursively()
                    unpackedOutput.deleteRecursively()
                }
            }
        }
    }

    private fun sanitizePackFileName(displayName: String): String {
        val baseName = displayName.substringBeforeLast('.', displayName)
        return baseName
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .trim()
            .trim('.')
            .take(80)
            .ifBlank { "pack" }
    }

    fun saveConvertedPack(destinationUri: Uri, onFinished: (Boolean) -> Unit) {
        val file = _uiState.value.convertedFile ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val out = getApplication<Application>().contentResolver.openOutputStream(destinationUri)
                        ?: throw IOException("Unable to open destination")
                    out.use { output ->
                        file.inputStream().use { input -> input.copyTo(output) }
                    }
                }
                onFinished(true)
            } catch (_: Exception) {
                onFinished(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stagedInputFile?.delete()
        _uiState.value.convertedFile?.delete()
    }
}
