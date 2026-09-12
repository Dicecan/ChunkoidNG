package com.noches.chunkoidng.ui.screens.converter

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noches.chunkoidng.core.conversion.ChunkerFormat
import com.noches.chunkoidng.core.conversion.ConversionConfig
import com.noches.chunkoidng.core.conversion.ConversionEvent
import com.noches.chunkoidng.core.settings.AppPreferences
import com.noches.chunkoidng.core.world.ArchiveManager
import com.noches.chunkoidng.core.world.HistoryManager
import com.noches.chunkoidng.core.world.WorldInfo
import com.noches.chunkoidng.service.ConversionForegroundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ConverterStage {
    SELECT_SOURCE,
    STAGING,
    CONFIGURE,
    CONVERTING,
    COMPLETED,
    ERROR
}

data class ConverterUiState(
    val stage: ConverterStage = ConverterStage.SELECT_SOURCE,
    val stagingProgress: Int = 0,
    val stagingMessage: String = "",
    val worldInfo: WorldInfo? = null,
    val targetFormat: ChunkerFormat = ChunkerFormat.BEDROCK_FORMATS.first { it.id == "BEDROCK_1_21_50" },
    // Advanced settings
    val includeOverworld: Boolean = true,
    val includeNether: Boolean = true,
    val includeTheEnd: Boolean = true,
    val overrideWorldName: String = "",
    val overrideGameMode: String = "DEFAULT",
    val overrideDifficulty: String = "DEFAULT",
    // Converting state
    val conversionProgress: Int = 0,
    val conversionStageText: String = "",
    val conversionLogs: List<String> = emptyList(),
    val startTimestamp: Long = 0, // For duration calculation
    // Result
    val exportedUri: Uri? = null,
    val isExporting: Boolean = false,
    val errorMessage: String? = null
)

class WorldConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val archiveManager = ArchiveManager(application)
    private val historyManager = HistoryManager(application)
    private val prefs = AppPreferences(application)
    
    private val _uiState = MutableStateFlow(ConverterUiState())
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    private var boundService: ConversionForegroundService? = null
    private var serviceJob: Job? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? ConversionForegroundService.LocalBinder
            boundService = binder?.getService()
            observeService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }
    }

    init {
        val intent = Intent(application, ConversionForegroundService::class.java)
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun observeService() {
        val s = boundService ?: return
        serviceJob?.cancel()
        serviceJob = viewModelScope.launch {
            s.events.collect { event ->
                when (event) {
                    is ConversionEvent.Progress -> {
                        _uiState.update {
                            it.copy(
                                conversionProgress = event.percent,
                                conversionStageText = event.stage
                            )
                        }
                    }
                    is ConversionEvent.LogOutput -> {
                        _uiState.update {
                            val updatedLogs = (it.conversionLogs + event.line).takeLast(1200)
                            it.copy(conversionLogs = updatedLogs)
                        }
                    }
                    is ConversionEvent.Success -> {
                        val state = _uiState.value
                        val duration = System.currentTimeMillis() - state.startTimestamp
                        
                        historyManager.addRecord(
                            worldName = state.overrideWorldName.ifBlank { state.worldInfo?.name ?: "Unknown" },
                            sourcePlatform = state.worldInfo?.platform?.displayName ?: "Unknown",
                            targetPlatform = state.targetFormat.platform.displayName,
                            durationMs = duration,
                            icon = state.worldInfo?.iconBitmap
                        )
                        
                        _uiState.update {
                            it.copy(
                                stage = ConverterStage.COMPLETED,
                                conversionProgress = 100,
                                conversionStageText = "转换完成！"
                            )
                        }
                    }
                    is ConversionEvent.Failure -> {
                        _uiState.update {
                            it.copy(
                                stage = ConverterStage.ERROR,
                                errorMessage = event.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun handleArchiveSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = ConverterStage.STAGING,
                    stagingProgress = 0,
                    stagingMessage = "正在准备归档文件..."
                )
            }

            val result = archiveManager.extractArchive(uri) { progress, status ->
                _uiState.update { it.copy(stagingProgress = progress, stagingMessage = status) }
            }

            result.fold(
                onSuccess = { world ->
                    val defaultTarget = ChunkerFormat.getDefaultFormatForOpposite(world.platform)
                    _uiState.update {
                        it.copy(
                            stage = ConverterStage.CONFIGURE,
                            worldInfo = world,
                            targetFormat = defaultTarget,
                            overrideWorldName = world.name
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            stage = ConverterStage.ERROR,
                            errorMessage = "归档加载失败: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun handleFolderSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = ConverterStage.STAGING,
                    stagingProgress = 0,
                    stagingMessage = "正在读取世界目录..."
                )
            }

            val result = archiveManager.stageTreeUri(uri) { progress, status ->
                _uiState.update { it.copy(stagingProgress = progress, stagingMessage = status) }
            }

            result.fold(
                onSuccess = { world ->
                    val defaultTarget = ChunkerFormat.getDefaultFormatForOpposite(world.platform)
                    _uiState.update {
                        it.copy(
                            stage = ConverterStage.CONFIGURE,
                            worldInfo = world,
                            targetFormat = defaultTarget,
                            overrideWorldName = world.name
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            stage = ConverterStage.ERROR,
                            errorMessage = "目录导入失败: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun selectTargetFormat(format: ChunkerFormat) {
        _uiState.update { it.copy(targetFormat = format) }
    }

    fun toggleDimension(dim: String, include: Boolean) {
        _uiState.update {
            when (dim.uppercase()) {
                "OVERWORLD" -> it.copy(includeOverworld = include)
                "NETHER" -> it.copy(includeNether = include)
                "THE_END", "END" -> it.copy(includeTheEnd = include)
                else -> it
            }
        }
    }

    fun setOverrideWorldName(name: String) {
        _uiState.update { it.copy(overrideWorldName = name) }
    }

    fun setOverrideGameMode(mode: String) {
        _uiState.update { it.copy(overrideGameMode = mode) }
    }

    fun setOverrideDifficulty(difficulty: String) {
        _uiState.update { it.copy(overrideDifficulty = difficulty) }
    }

    fun startConversion() {
        val s = _uiState.value
        val world = s.worldInfo ?: return

        val config = ConversionConfig(
            inputDir = archiveManager.inputDir,
            outputDir = archiveManager.outputDir,
            targetFormat = s.targetFormat,
            keepOriginalNbt = prefs.keepOriginalNbt,
            lowMemoryMode = prefs.lowRamModeEnabled,
            maxMemoryMB = prefs.maxMemoryMb.toInt(),
            includeOverworld = s.includeOverworld,
            includeNether = s.includeNether,
            includeTheEnd = s.includeTheEnd,
            overrideWorldName = s.overrideWorldName.ifBlank { null },
            overrideGameMode = s.overrideGameMode,
            overrideDifficulty = s.overrideDifficulty
        )

        _uiState.update {
            it.copy(
                stage = ConverterStage.CONVERTING,
                conversionProgress = 0,
                conversionStageText = "正在启动转换...",
                conversionLogs = emptyList(),
                startTimestamp = System.currentTimeMillis()
            )
        }

        // Start Foreground Service
        val serviceIntent = Intent(getApplication(), ConversionForegroundService::class.java)
        getApplication<Application>().startService(serviceIntent)
        boundService?.startConversion(config)
    }

    fun cancelConversion() {
        boundService?.cancelConversion()
        _uiState.update {
            it.copy(
                stage = ConverterStage.CONFIGURE,
                conversionProgress = 0,
                conversionStageText = "已取消"
            )
        }
    }

    fun exportConvertedWorld(targetUri: Uri, packAsArchive: Boolean, onDone: (Uri) -> Unit) {
        val s = _uiState.value
        val name = s.overrideWorldName.ifBlank { s.worldInfo?.name ?: "Converted_World" }
        val isBedrock = s.targetFormat.platform.isBedrock

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val result = archiveManager.exportToUri(targetUri, name, packAsArchive, isBedrock)
            _uiState.update { it.copy(isExporting = false) }
            result.onSuccess {
                _uiState.update { it.copy(exportedUri = targetUri) }
                onDone(targetUri)
            }
        }
    }

    fun reset() {
        archiveManager.cleanWorkspace()
        _uiState.update {
            ConverterUiState(stage = ConverterStage.SELECT_SOURCE)
        }
    }

    override fun onCleared() {
        try {
            getApplication<Application>().unbindService(serviceConnection)
        } catch (_: Exception) {}
        super.onCleared()
    }
}

