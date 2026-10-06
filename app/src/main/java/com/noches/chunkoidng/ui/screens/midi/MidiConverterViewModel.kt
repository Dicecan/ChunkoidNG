package com.noches.chunkoidng.ui.screens.midi

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
                    statusMessage = "正在读取并解析 MIDI 文件...",
                    errorMessage = null
                )
            }
            addLog("选择 MIDI 文件: $displayName")
            addLog("正在解析二进制 SMF 结构与音轨...")

            try {
                val parsed = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val input = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("Cannot open MIDI file")
                    val bytes = input.use { stream ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0L
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > MAX_MIDI_INPUT_BYTES) {
                                throw IllegalArgumentException("MIDI file is too large")
                            }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    MidiParser.parse(bytes, displayName.substringBeforeLast('.'))
                }

                addLog("成功解析 MIDI: 格式 ${parsed.format}, 分辨率 ${parsed.division} PPQ")
                addLog("音轨数: ${parsed.tracks.size}, 音符总计: ${parsed.notes.size}, 时长: ${parsed.durationMs / 1000} 秒, 初始 BPM: ${parsed.initialBpm.toInt()}")

                _uiState.update {
                    it.copy(
                        parsedSong = parsed,
                        isParsing = false,
                        statusMessage = "解析完成"
                    )
                }
            } catch (e: Exception) {
                addLog("解析失败: ${e.message}")
                _uiState.update {
                    it.copy(
                        isParsing = false,
                        errorMessage = e.message ?: "Failed to parse MIDI",
                        statusMessage = "解析失败"
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
        addLog("已选择目标世界存档: ${dir.name}")
    }

    fun onTargetWorldTreeSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isStagingWorld = true,
                    statusMessage = "正在通过 SAF 准备目标世界存档文件夹...",
                    errorMessage = null
                )
            }
            addLog("选择目标世界文件夹 (SAF): $uri")
            val stagingDir = File(getApplication<Application>().cacheDir, "midi_target_world")
            val result = archiveManager.stageTreeUriToDir(uri, stagingDir) { _, status ->
                _uiState.update { it.copy(statusMessage = status) }
            }
            result.onSuccess { worldInfo ->
                addLog("成功加载目标世界: ${worldInfo.name} (${worldInfo.platform.name})")
                _uiState.update {
                    it.copy(
                        targetWorldDir = stagingDir,
                        targetWorldName = worldInfo.name,
                        isStagingWorld = false,
                        statusMessage = "目标世界准备就绪"
                    )
                }
            }.onFailure { e ->
                addLog("加载目标世界失败: ${e.message}")
                _uiState.update {
                    it.copy(
                        isStagingWorld = false,
                        errorMessage = e.message ?: "Failed to stage target world",
                        statusMessage = "目标世界加载失败"
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
                    statusMessage = "正在解压目标世界压缩包...",
                    errorMessage = null
                )
            }
            addLog("选择目标世界压缩包: $uri")
            val stagingDir = File(getApplication<Application>().cacheDir, "midi_target_world")
            val result = archiveManager.extractArchiveToDir(uri, stagingDir) { _, status ->
                _uiState.update { it.copy(statusMessage = status) }
            }
            result.onSuccess { worldInfo ->
                addLog("成功加载目标世界: ${worldInfo.name} (${worldInfo.platform.name})")
                _uiState.update {
                    it.copy(
                        targetWorldDir = stagingDir,
                        targetWorldName = worldInfo.name,
                        isStagingWorld = false,
                        statusMessage = "目标世界准备就绪"
                    )
                }
            }.onFailure { e ->
                addLog("加载目标世界压缩包失败: ${e.message}")
                _uiState.update {
                    it.copy(
                        isStagingWorld = false,
                        errorMessage = e.message ?: "Failed to stage target world archive",
                        statusMessage = "目标世界加载失败"
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
                    statusMessage = "开始合成红石音乐..."
                )
            }
            addLog("开始生成红石音乐 - 模式: ${state.selectedExportMode.name}")
            addLog("配置: 速度 ${state.ticksPerSecond} ticks/s, 最优移调: ${state.autoTranspose}")

            try {
                addLog("正在量化时序至 0.1s 红石刻并进行乐器基座方块映射...")
                val quantConfig = QuantizationConfig(
                    ticksPerSecond = state.ticksPerSecond,
                    autoTransposition = state.autoTranspose
                )
                val song = withContext(Dispatchers.IO) {
                    RedstoneQuantizer.quantize(parsed, quantConfig)
                }
                addLog("时序量化完成: ${song.notes.size} 个音符事件, 总时长 ${song.lengthTicks} 红石刻")

                val cacheDir = getApplication<Application>().cacheDir
                val title = song.title.ifBlank { "music" }
                    .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    .take(80)
                    .ifBlank { "music" }

                when (state.selectedExportMode) {
                    MidiExportMode.FLAT_WORLD -> {
                        addLog("正在组装基岩版超平坦即听世界 (.mcworld)...")
                        val outputWorld = File(cacheDir, "$title.mcworld")
                        withContext(Dispatchers.IO) {
                            FlatWorldMusicGenerator.generateFlatWorld(song, outputWorld) { step ->
                                addLog(step)
                            }
                        }
                        val sizeMb = outputWorld.length().toDouble() / (1024.0 * 1024.0)
                        addLog("超平坦世界生成成功！文件大小: %.2f MB".format(sizeMb))
                        _uiState.update {
                            it.copy(isGenerating = false, generatedFile = outputWorld, statusMessage = "超平坦世界生成成功")
                        }
                    }

                    MidiExportMode.INJECT_WORLD -> {
                        val targetDir = state.targetWorldDir
                            ?: throw IllegalArgumentException("Target world folder not selected")
                        addLog("正在注入目标世界存档: ${targetDir.name} (目标坐标: X=${state.coordX}, Y=${state.coordY}, Z=${state.coordZ})...")
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
                            addLog("已创建安全备份: ${result.backupFile.name}")
                        }
                        addLog(result.message)
                        addLog("游戏内触发命令: ${result.inGameCommand}")
                        var exportedWorldFile: File? = null
                        if (result.success) {
                            val safeWorldName = state.targetWorldName.ifBlank { "injected_world" }
                                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            val outputWorld = File(cacheDir, "${safeWorldName}_injected.mcworld")
                            addLog("正在打包已注入的世界存档为 .mcworld...")
                            withContext(Dispatchers.IO) {
                                archiveManager.zipDirectoryToFile(targetDir, outputWorld)
                            }
                            exportedWorldFile = outputWorld
                            addLog("已生成注入世界导出文件: ${outputWorld.name} (${outputWorld.length() / 1024} KB)")
                        }
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                injectionResult = result,
                                generatedFile = exportedWorldFile,
                                statusMessage = if (result.success) "注入完成，可导出世界" else "注入失败"
                            )
                        }
                    }

                    MidiExportMode.STRUCTURE -> {
                        addLog("正在生成基岩版物理结构文件 (.mcstructure)...")
                        val outputStruct = File(cacheDir, "$title.mcstructure")
                        withContext(Dispatchers.IO) {
                            StructureExporter.exportBedrockMcStructure(song, outputStruct)
                        }
                        addLog("结构文件生成成功: ${outputStruct.name} (${outputStruct.length()} 字节)")
                        _uiState.update {
                            it.copy(isGenerating = false, generatedFile = outputStruct, statusMessage = "结构导出完成")
                        }
                    }

                    MidiExportMode.FUNCTION -> {
                        addLog("正在生成全版本指令数据包...")
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
                        addLog("指令数据包生成成功: ${zipFile.name}")
                        _uiState.update {
                            it.copy(isGenerating = false, generatedFile = zipFile, statusMessage = "数据包导出完成")
                        }
                    }

                    MidiExportMode.NBS -> {
                        addLog("正在导出 Note Block Studio (.nbs) 文件...")
                        val outputNbs = File(cacheDir, "$title.nbs")
                        withContext(Dispatchers.IO) {
                            NbsExporter.export(song, outputNbs)
                        }
                        addLog("NBS 文件导出成功: ${outputNbs.name} (${outputNbs.length()} 字节)")
                        _uiState.update {
                            it.copy(isGenerating = false, generatedFile = outputNbs, statusMessage = "NBS 导出完成")
                        }
                    }
                }
            } catch (e: Exception) {
                addLog("合成失败: ${e.message}")
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: "Failed to generate redstone music",
                        statusMessage = "合成失败"
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
                    val out = getApplication<Application>().contentResolver.openOutputStream(destinationUri)
                        ?: throw java.io.IOException("Unable to open destination")
                    out.use { output ->
                        file.inputStream().use { input -> input.copyTo(output) }
                    }
                }
                addLog("成功保存产物至: ${destinationUri.path ?: "外部存储"}")
                onFinished(true)
            } catch (e: Exception) {
                addLog("保存失败: ${e.message}")
                onFinished(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _uiState.value.generatedFile?.delete()
    }
}
