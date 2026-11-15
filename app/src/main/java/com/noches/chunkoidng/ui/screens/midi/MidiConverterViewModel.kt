package com.noches.chunkoidng.ui.screens.midi

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noches.chunkoidng.R
import com.noches.chunkoidng.core.io.SafeFileIO
import com.noches.chunkoidng.core.settings.LocaleHelper
import com.noches.chunkoidng.core.world.ArchiveManager
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val isStagingWorld: Boolean = false,
    val coordX: Int = 0,
    val coordY: Int = 64,
    val coordZ: Int = 0,
    val autoBackup: Boolean = true,
    val isParsing: Boolean = false,
    val isGenerating: Boolean = false,
    val statusMessage: String = "",
    val logs: List<String> = emptyList(),
    val generatedFile: File? = null,
    val injectionResult: WorldInjectionResult? = null,
    val errorMessage: String? = null
)

class MidiConverterViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val MAX_MIDI_INPUT_BYTES = 128L * 1024L * 1024L
    }

    private val _uiState = MutableStateFlow(MidiConverterUiState())
    val uiState: StateFlow<MidiConverterUiState> = _uiState.asStateFlow()
    private val archiveManager = ArchiveManager(application)
    private val l10n get() = LocaleHelper.localizedContext(getApplication())

    private fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "[$time] $message"
        _uiState.update {
            it.copy(
                statusMessage = message,
                logs = it.logs + line
            )
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    fun onMidiSelected(uri: Uri, displayName: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedMidiUri = uri,
                    selectedMidiName = displayName,
                    parsedSong = null,
                    isParsing = true,
                    statusMessage = l10n.getString(R.string.midi_status_reading),
                    errorMessage = null
                )
            }
            addLog(l10n.getString(R.string.midi_log_selected, displayName))
            addLog(l10n.getString(R.string.midi_log_parsing_smf))

            try {
                val parsed = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val input = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException(l10n.getString(R.string.midi_err_open_file))
                    val bytes = input.use { stream ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0L
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > MAX_MIDI_INPUT_BYTES) {
                                throw IllegalArgumentException(l10n.getString(R.string.midi_err_too_large))
                            }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    MidiParser.parse(bytes, displayName.substringBeforeLast('.'))
                }

                addLog(l10n.getString(R.string.midi_log_parsed, parsed.format, parsed.division))
                addLog(l10n.getString(R.string.midi_log_parsed_stats, parsed.tracks.size, parsed.notes.size, (parsed.durationMs / 1000).toInt(), parsed.initialBpm.toInt()))

                _uiState.update {
                    it.copy(
                        parsedSong = parsed,
                        isParsing = false,
                        statusMessage = l10n.getString(R.string.midi_status_parsed)
                    )
                }
            } catch (e: Exception) {
                addLog(l10n.getString(R.string.midi_log_parse_failed, e.message ?: ""))
                _uiState.update {
                    it.copy(
                        isParsing = false,
                        errorMessage = e.message ?: l10n.getString(R.string.midi_err_parse_failed),
                        statusMessage = l10n.getString(R.string.midi_status_parse_failed)
                    )
                }
            }
        }
    }

    fun setTicksPerSecond(tps: Double) {
        if (tps.isFinite()) {
            _uiState.update { it.copy(ticksPerSecond = tps.coerceIn(0.1, 100.0)) }
        }
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
        addLog(l10n.getString(R.string.midi_log_target_selected, dir.name))
    }

    fun onTargetWorldTreeSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isStagingWorld = true,
                    statusMessage = l10n.getString(R.string.midi_status_staging_saf),
                    errorMessage = null
                )
            }
            addLog(l10n.getString(R.string.midi_log_target_saf, uri.toString()))
            val stagingDir = File(getApplication<Application>().cacheDir, "midi_target_world")
            val result = archiveManager.stageTreeUriToDir(uri, stagingDir) { _, status ->
                _uiState.update { it.copy(statusMessage = status) }
            }
            result.onSuccess { worldInfo ->
                addLog(l10n.getString(R.string.midi_log_target_loaded, worldInfo.name, worldInfo.platform.name))
                _uiState.update {
                    it.copy(
                        targetWorldDir = stagingDir,
                        targetWorldName = worldInfo.name,
                        isStagingWorld = false,
                        statusMessage = l10n.getString(R.string.midi_status_target_ready)
                    )
                }
            }.onFailure { e ->
                addLog(l10n.getString(R.string.midi_log_target_load_failed, e.message ?: ""))
                _uiState.update {
                    it.copy(
                        isStagingWorld = false,
                        errorMessage = e.message ?: l10n.getString(R.string.midi_err_target_stage),
                        statusMessage = l10n.getString(R.string.midi_status_target_load_failed)
                    )
                }
            }
        }
    }

    fun onTargetWorldArchiveSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isStagingWorld = true,
                    statusMessage = l10n.getString(R.string.midi_status_extracting_world),
                    errorMessage = null
                )
            }
            addLog(l10n.getString(R.string.midi_log_target_archive, uri.toString()))
            val stagingDir = File(getApplication<Application>().cacheDir, "midi_target_world")
            val result = archiveManager.extractArchiveToDir(uri, stagingDir) { _, status ->
                _uiState.update { it.copy(statusMessage = status) }
            }
            result.onSuccess { worldInfo ->
                addLog(l10n.getString(R.string.midi_log_target_loaded, worldInfo.name, worldInfo.platform.name))
                _uiState.update {
                    it.copy(
                        targetWorldDir = stagingDir,
                        targetWorldName = worldInfo.name,
                        isStagingWorld = false,
                        statusMessage = l10n.getString(R.string.midi_status_target_ready)
                    )
                }
            }.onFailure { e ->
                addLog(l10n.getString(R.string.midi_log_target_archive_failed, e.message ?: ""))
                _uiState.update {
                    it.copy(
                        isStagingWorld = false,
                        errorMessage = e.message ?: l10n.getString(R.string.midi_err_target_archive_stage),
                        statusMessage = l10n.getString(R.string.midi_status_target_load_failed)
                    )
                }
            }
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
                it.copy(
                    isGenerating = true,
                    errorMessage = null,
                    injectionResult = null,
                    generatedFile = null,
                    statusMessage = l10n.getString(R.string.midi_status_generating)
                )
            }
            addLog(l10n.getString(R.string.midi_log_gen_start, state.selectedExportMode.name))
            addLog(l10n.getString(R.string.midi_log_gen_config, state.ticksPerSecond.toString(), state.autoTranspose.toString()))

            try {
                addLog(l10n.getString(R.string.midi_log_quantizing))
                val quantConfig = QuantizationConfig(
                    ticksPerSecond = state.ticksPerSecond,
                    autoTransposition = state.autoTranspose
                )
                val song = withContext(Dispatchers.IO) {
                    RedstoneQuantizer.quantize(parsed, quantConfig)
                }
                addLog(l10n.getString(R.string.midi_log_quantized, song.notes.size, song.lengthTicks))

                val cacheDir = getApplication<Application>().cacheDir
                val title = song.title.ifBlank { "music" }
                    .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    .take(80)
                    .ifBlank { "music" }

                when (state.selectedExportMode) {
                    MidiExportMode.FLAT_WORLD -> {
                        addLog(l10n.getString(R.string.midi_log_flatworld_assembling))
                        val outputWorld = File(cacheDir, "$title.mcworld")
                        withContext(Dispatchers.IO) {
                            FlatWorldMusicGenerator.generateFlatWorld(song, outputWorld) { step ->
                                addLog(step)
                            }
                        }
                        val sizeMb = outputWorld.length().toDouble() / (1024.0 * 1024.0)
                        addLog(l10n.getString(R.string.midi_log_flatworld_done, String.format(Locale.US, "%.2f", sizeMb)))
                        _uiState.update {
                            it.copy(isGenerating = false, generatedFile = outputWorld, statusMessage = l10n.getString(R.string.midi_status_flatworld_done))
                        }
                    }

                    MidiExportMode.INJECT_WORLD -> {
                        val targetDir = state.targetWorldDir
                            ?: throw IllegalArgumentException(l10n.getString(R.string.midi_err_target_not_selected))
                        addLog(l10n.getString(R.string.midi_log_injecting, targetDir.name, state.coordX, state.coordY, state.coordZ))
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
                        if (result.backupFile != null) {
                            addLog(l10n.getString(R.string.midi_log_backup_created, result.backupFile.name))
                        }
                        addLog(result.message)
                        addLog(l10n.getString(R.string.midi_log_ingame_cmd, result.inGameCommand))
                        var exportedWorldFile: File? = null
                        if (result.success) {
                            val safeWorldName = state.targetWorldName.ifBlank { "injected_world" }
                                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            val outputWorld = File(cacheDir, "${safeWorldName}_injected.mcworld")
                            addLog(l10n.getString(R.string.midi_log_packing_world))
                            withContext(Dispatchers.IO) {
                                archiveManager.zipDirectoryToFile(targetDir, outputWorld)
                            }
                            exportedWorldFile = outputWorld
                            addLog(l10n.getString(R.string.midi_log_packed_world, outputWorld.name, outputWorld.length() / 1024))
                        }
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                injectionResult = result,
                                generatedFile = exportedWorldFile,
                                statusMessage = if (result.success) l10n.getString(R.string.midi_status_inject_done) else l10n.getString(R.string.midi_status_inject_failed)
                            )
                        }
                    }

                    MidiExportMode.STRUCTURE -> {
                        addLog(l10n.getString(R.string.midi_log_struct_building))
                        val outputStruct = File(cacheDir, "$title.mcstructure")
                        withContext(Dispatchers.IO) {
                            StructureExporter.exportBedrockMcStructure(song, outputStruct)
                        }
                        addLog(l10n.getString(R.string.midi_log_struct_done, outputStruct.name, outputStruct.length()))
                        _uiState.update {
                            it.copy(isGenerating = false, generatedFile = outputStruct, statusMessage = l10n.getString(R.string.midi_status_struct_done))
                        }
                    }

                    MidiExportMode.FUNCTION -> {
                        addLog(l10n.getString(R.string.midi_log_dp_building))
                        val dpDir = File(cacheDir, "music_${title}_datapack")
                        withContext(Dispatchers.IO) {
                            dpDir.deleteRecursively()
                            dpDir.mkdirs()
                            McFunctionMusicExporter.exportScheduledDatapack(song, "music_$title", dpDir)
                        }
                        val zipFile = File(cacheDir, "music_${title}_datapack.zip")
                        withContext(Dispatchers.IO) {
                            java.util.zip.ZipOutputStream(FileOutputStream(zipFile).buffered()).use { zos ->
                                val prefix = dpDir.absolutePath.length + 1
                                dpDir.walkTopDown().forEach { f ->
                                    if (f.isFile) {
                                        val name = f.absolutePath.substring(prefix).replace('\\', '/')
                                        zos.putNextEntry(java.util.zip.ZipEntry(name))
                                        f.inputStream().use { it.copyTo(zos) }
                                        zos.closeEntry()
                                    }
                                }
                            }
                        }
                        addLog(l10n.getString(R.string.midi_log_dp_done, zipFile.name))
                        _uiState.update {
                            it.copy(isGenerating = false, generatedFile = zipFile, statusMessage = l10n.getString(R.string.midi_status_dp_done))
                        }
                    }

                    MidiExportMode.NBS -> {
                        addLog(l10n.getString(R.string.midi_log_nbs_building))
                        val outputNbs = File(cacheDir, "$title.nbs")
                        withContext(Dispatchers.IO) {
                            NbsExporter.export(song, outputNbs)
                        }
                        addLog(l10n.getString(R.string.midi_log_nbs_done, outputNbs.name, outputNbs.length()))
                        _uiState.update {
                            it.copy(isGenerating = false, generatedFile = outputNbs, statusMessage = l10n.getString(R.string.midi_status_nbs_done))
                        }
                    }
                }
            } catch (e: Exception) {
                addLog(l10n.getString(R.string.midi_log_gen_failed, e.message ?: ""))
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: l10n.getString(R.string.midi_err_gen_failed),
                        statusMessage = l10n.getString(R.string.midi_status_gen_failed)
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
                    val out = SafeFileIO.openTruncatingOutputStream(
                        getApplication<Application>().contentResolver,
                        destinationUri
                    ) ?: throw java.io.IOException(l10n.getString(R.string.midi_err_open_dest))
                    out.use { output ->
                        file.inputStream().use { input -> input.copyTo(output) }
                    }
                }
                addLog(l10n.getString(R.string.midi_log_saved, destinationUri.path ?: l10n.getString(R.string.midi_external_storage)))
                onFinished(true)
            } catch (e: Exception) {
                addLog(l10n.getString(R.string.midi_log_save_failed, e.message ?: ""))
                onFinished(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _uiState.value.generatedFile?.delete()
    }
}
