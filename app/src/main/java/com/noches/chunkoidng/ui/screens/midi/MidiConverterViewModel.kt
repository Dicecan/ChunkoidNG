package com.noches.chunkoidng.ui.screens.midi

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noches.chunkoidng.core.midi.*
import com.noches.chunkoidng.core.midi.exporters.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class MidiExportMode {
    FLAT_WORLD,
    INJECT_WORLD,
    STRUCTURE,
    FUNCTION,
    NBS
}

data class MidiConverterUiState(
    val selectedMidiUri: Uri? = null,
    val selectedMidiName: String = "",
    val parsedSong: ParsedMidiSong? = null,
    val ticksPerSecond: Double = 10.0,
    val autoTranspose: Boolean = true,
    val selectedExportMode: MidiExportMode = MidiExportMode.FLAT_WORLD,
    val targetWorldDir: File? = null,
    val targetWorldName: String = "",
    val coordX: Int = 0,
    val coordY: Int = 64,
    val coordZ: Int = 0,
    val autoBackup: Boolean = true,
    val isGenerating: Boolean = false,
    val generatedFile: File? = null,
    val injectionResult: WorldInjectionResult? = null,
    val errorMessage: String? = null
)

class MidiConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MidiConverterUiState())
    val uiState: StateFlow<MidiConverterUiState> = _uiState.asStateFlow()

    fun onMidiSelected(uri: Uri, displayName: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedMidiUri = uri,
                    selectedMidiName = displayName,
                    isGenerating = true,
                    errorMessage = null
                )
            }

            try {
                val parsed = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalArgumentException("Cannot open MIDI file")
                    MidiParser.parse(bytes, displayName.substringBeforeLast('.'))
                }

                _uiState.update {
                    it.copy(
                        parsedSong = parsed,
                        isGenerating = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: "Failed to parse MIDI"
                    )
                }
            }
        }
    }

    fun setTicksPerSecond(tps: Double) {
        _uiState.update { it.copy(ticksPerSecond = tps) }
    }

    fun setAutoTranspose(enabled: Boolean) {
        _uiState.update { it.copy(autoTranspose = enabled) }
    }

    fun setExportMode(mode: MidiExportMode) {
        _uiState.update { it.copy(selectedExportMode = mode) }
    }

    fun setTargetWorld(dir: File) {
        _uiState.update {
            it.copy(targetWorldDir = dir, targetWorldName = dir.name)
        }
    }

    fun setCoordinates(x: Int, y: Int, z: Int) {
        _uiState.update { it.copy(coordX = x, coordY = y, coordZ = z) }
    }

    fun setAutoBackup(backup: Boolean) {
        _uiState.update { it.copy(autoBackup = backup) }
    }

    fun generateAndExport() {
        val parsed = _uiState.value.parsedSong ?: return
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update {
                it.copy(isGenerating = true, errorMessage = null, injectionResult = null, generatedFile = null)
            }

            try {
                val quantConfig = QuantizationConfig(
                    ticksPerSecond = state.ticksPerSecond,
                    autoTransposition = state.autoTranspose
                )
                val song = RedstoneQuantizer.quantize(parsed, quantConfig)
                val cacheDir = getApplication<Application>().cacheDir
                val title = song.title.ifBlank { "music" }
                    .replace(Regex("[^a-zA-Z0-9_-]"), "_")

                when (state.selectedExportMode) {
                    MidiExportMode.FLAT_WORLD -> {
                        val outputWorld = File(cacheDir, "$title.mcworld")
                        withContext(Dispatchers.IO) {
                            FlatWorldMusicGenerator.generateFlatWorld(song, outputWorld)
                        }
                        _uiState.update {
                            it.copy(isGenerating = false, generatedFile = outputWorld)
                        }
                    }

                    MidiExportMode.INJECT_WORLD -> {
                        val targetDir = state.targetWorldDir
                            ?: throw IllegalArgumentException("Target world folder not selected")
                        val injectConfig = WorldInjectionConfig(
                            targetX = state.coordX,
                            targetY = state.coordY,
                            targetZ = state.coordZ,
                            autoLoadOnJoin = true,
                            createBackup = state.autoBackup
                        )
                        val result = withContext(Dispatchers.IO) {
                            WorldBlockInjector.injectSongIntoWorld(targetDir, song, injectConfig)
                        }
                        _uiState.update {
                            it.copy(isGenerating = false, injectionResult = result)
                        }
                    }

                    MidiExportMode.STRUCTURE -> {
                        val outputStruct = File(cacheDir, "$title.mcstructure")
                        withContext(Dispatchers.IO) {
                            StructureExporter.exportBedrockMcStructure(song, outputStruct)
                        }
                        _uiState.update {
                            it.copy(isGenerating = false, generatedFile = outputStruct)
                        }
                    }

                    MidiExportMode.FUNCTION -> {
                        val dpDir = File(cacheDir, "music_${title}_datapack")
                        withContext(Dispatchers.IO) {
                            dpDir.deleteRecursively()
                            dpDir.mkdirs()
                            McFunctionMusicExporter.exportScheduledDatapack(song, "music_$title", dpDir)
                        }
                        val zipFile = File(cacheDir, "music_${title}_datapack.zip")
                        withContext(Dispatchers.IO) {
                            val zos = java.util.zip.ZipOutputStream(FileOutputStream(zipFile).buffered())
                            val prefix = dpDir.absolutePath.length + 1
                            dpDir.walkTopDown().forEach { f ->
                                if (f.isFile) {
                                    val name = f.absolutePath.substring(prefix).replace('\\', '/')
                                    zos.putNextEntry(java.util.zip.ZipEntry(name))
                                    f.inputStream().use { it.copyTo(zos) }
                                    zos.closeEntry()
                                }
                            }
                            zos.close()
                        }
                        _uiState.update {
                            it.copy(isGenerating = false, generatedFile = zipFile)
                        }
                    }

                    MidiExportMode.NBS -> {
                        val outputNbs = File(cacheDir, "$title.nbs")
                        withContext(Dispatchers.IO) {
                            NbsExporter.export(song, outputNbs)
                        }
                        _uiState.update {
                            it.copy(isGenerating = false, generatedFile = outputNbs)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: "Failed to generate redstone music"
                    )
                }
            }
        }
    }

    fun saveOutputFile(destinationUri: Uri, onFinished: (Boolean) -> Unit) {
        val file = _uiState.value.generatedFile ?: return
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
        _uiState.value.generatedFile?.delete()
    }
}
