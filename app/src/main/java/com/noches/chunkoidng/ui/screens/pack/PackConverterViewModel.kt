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
                    errorMessage = null
                )
            }

            try {
                val file = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val temp = File(context.cacheDir, "pack_input_${System.currentTimeMillis()}.tmp")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(temp).use { output ->
                            input.copyTo(output)
                        }
                    }
                    temp
                }
                stagedInputFile = file

                val detected = withContext(Dispatchers.IO) {
                    val names = mutableListOf<String>()
                    try {
                        ZipFile(file).use { zip ->
                            val entries = zip.entries()
                            while (entries.hasMoreElements()) {
                                names.add(entries.nextElement().name)
                            }
                        }
                    } catch (_: Exception) {}
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
                _uiState.update {
                    it.copy(
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
        val input = stagedInputFile ?: return
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConverting = true,
                    progressCurrent = 0,
                    progressTotal = 100,
                    currentStepText = "Extracting pack...",
                    errorMessage = null
                )
            }

            try {
                val cacheDir = getApplication<Application>().cacheDir
                val unpackedInput = File(cacheDir, "pack_unpacked_in_${System.currentTimeMillis()}")
                val unpackedOutput = File(cacheDir, "pack_unpacked_out_${System.currentTimeMillis()}")

                val outputExt = if (state.targetPlatform == PackPlatform.BEDROCK) ".mcpack" else ".zip"
                val baseName = state.selectedPackName.substringBeforeLast('.')
                val outputFile = File(cacheDir, "${baseName}_converted$outputExt")

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

                    ResourcePackConverterEngine.createArchive(unpackedOutput, outputFile)
                }

                val convertedCount = withContext(Dispatchers.IO) {
                    val count = unpackedOutput.walkTopDown().filter { it.isFile }.count()
                    unpackedInput.deleteRecursively()
                    unpackedOutput.deleteRecursively()
                    count
                }

                _uiState.update {
                    it.copy(
                        isConverting = false,
                        convertedFile = outputFile,
                        convertedFileSizeBytes = outputFile.length(),
                        filesConvertedCount = convertedCount,
                        currentStepText = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        errorMessage = e.message ?: "Pack conversion failed"
                    )
                }
            }
        }
    }

    fun saveConvertedPack(destinationUri: Uri, onFinished: (Boolean) -> Unit) {
        val file = _uiState.value.convertedFile ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(destinationUri)?.use { out ->
                        file.inputStream().use { input ->
                            input.copyTo(out)
                        }
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
