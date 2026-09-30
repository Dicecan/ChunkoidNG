package com.noches.chunkoidng.ui.screens.pruner

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
import com.noches.chunkoidng.core.conversion.PruningProfile
import com.noches.chunkoidng.core.world.ArchiveManager
import com.noches.chunkoidng.core.world.HistoryManager
import com.noches.chunkoidng.core.world.WorldInfo
import com.noches.chunkoidng.service.ConversionForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class PrunerStage {
    SELECT_SOURCE,
    STAGING,
    CONFIGURE,
    PRUNING,
    COMPLETED,
    ERROR
}

data class PrunerUiState(
    val stage: PrunerStage = PrunerStage.SELECT_SOURCE,
    val stagingProgress: Int = 0,
    val stagingMessage: String = "",
    val worldInfo: WorldInfo? = null,
    val originalSizeBytes: Long = 0L,
    val prunedSizeBytes: Long = 0L,
    // Pruning profile & options
    val pruningProfile: PruningProfile = PruningProfile.OVERWORLD_ONLY,
    val includeOverworld: Boolean = true,
    val includeNether: Boolean = false,
    val includeTheEnd: Boolean = false,
    val keepOriginalNbt: Boolean = true,
    val overrideWorldName: String = "",
    // Execution
    val pruningProgress: Int = 0,
    val pruningStageText: String = "",
    val pruningLogs: List<String> = emptyList(),
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val latestHistoryId: String? = null
)

class DimensionPrunerViewModel(application: Application) : AndroidViewModel(application) {

    private val archiveManager = ArchiveManager(application)
    private val historyManager = HistoryManager(application)

    private val _uiState = MutableStateFlow(PrunerUiState())
    val uiState: StateFlow<PrunerUiState> = _uiState.asStateFlow()

    private var boundService: ConversionForegroundService? = null
    private var isServiceBound = false
    private var serviceJob: Job? = null
    private var pendingConfig: ConversionConfig? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ConversionForegroundService.LocalBinder
            boundService = binder.getService()
            isServiceBound = true
            observeServiceEvents()

            pendingConfig?.let {
                boundService?.startConversion(it)
                pendingConfig = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
            isServiceBound = false
        }
    }

    init {
        val intent = Intent(application, ConversionForegroundService::class.java)
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onCleared() {
        serviceJob?.cancel()
        if (isServiceBound) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (_: Exception) {}
            isServiceBound = false
        }
        super.onCleared()
    }

    private fun observeServiceEvents() {
        val service = boundService ?: return
        serviceJob?.cancel()
        serviceJob = viewModelScope.launch {
            service.events.collect { event ->
                when (event) {
                    is ConversionEvent.Progress -> {
                        _uiState.update {
                            it.copy(
                                pruningProgress = event.percent,
                                pruningStageText = event.stage
                            )
                        }
                    }
                    is ConversionEvent.LogOutput -> {
                        _uiState.update {
                            it.copy(
                                pruningLogs = (it.pruningLogs + event.line).takeLast(400)
                            )
                        }
                    }
                    is ConversionEvent.Success -> {
                        val outputDir = event.outputDir
                        val prunedSize = calculateDirectorySize(outputDir)
                        val s = _uiState.value
                        val world = s.worldInfo

                        var historyId: String? = null
                        if (world != null) {
                            historyId = historyManager.addRecord(
                                worldName = s.overrideWorldName.ifBlank { world.name },
                                sourcePlatform = world.platform.name,
                                targetPlatform = "${world.platform.name} (瘦身版)",
                                durationMs = event.durationMs,
                                icon = world.iconBitmap
                            )
                        }

                        _uiState.update {
                            it.copy(
                                stage = PrunerStage.COMPLETED,
                                pruningProgress = 100,
                                prunedSizeBytes = prunedSize,
                                latestHistoryId = historyId,
                                pruningLogs = it.pruningLogs + "[SUCCESS] 地图瘦身裁剪成功！耗时: ${event.durationMs / 1000}s"
                            )
                        }
                    }
                    is ConversionEvent.Failure -> {
                        _uiState.update {
                            it.copy(
                                stage = PrunerStage.ERROR,
                                errorMessage = "瘦身裁剪中断: ${event.error}"
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
                    stage = PrunerStage.STAGING,
                    stagingProgress = 0,
                    stagingMessage = "正在解压并分析地图..."
                )
            }

            val result = archiveManager.extractArchive(uri) { progress, status ->
                _uiState.update { it.copy(stagingProgress = progress, stagingMessage = status) }
            }

            result.fold(
                onSuccess = { world ->
                    _uiState.update {
                        it.copy(
                            stage = PrunerStage.CONFIGURE,
                            worldInfo = world,
                            originalSizeBytes = world.sizeBytes,
                            overrideWorldName = "${world.name}_slim"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            stage = PrunerStage.ERROR,
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
                    stage = PrunerStage.STAGING,
                    stagingProgress = 0,
                    stagingMessage = "正在读取世界数据..."
                )
            }

            val result = archiveManager.stageTreeUri(uri) { progress, status ->
                _uiState.update { it.copy(stagingProgress = progress, stagingMessage = status) }
            }

            result.fold(
                onSuccess = { world ->
                    _uiState.update {
                        it.copy(
                            stage = PrunerStage.CONFIGURE,
                            worldInfo = world,
                            originalSizeBytes = world.sizeBytes,
                            overrideWorldName = "${world.name}_slim"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            stage = PrunerStage.ERROR,
                            errorMessage = "目录导入失败: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun selectPruningProfile(profile: PruningProfile) {
        _uiState.update { current ->
            when (profile) {
                PruningProfile.FULL -> current.copy(
                    pruningProfile = profile,
                    includeOverworld = true,
                    includeNether = true,
                    includeTheEnd = true
                )
                PruningProfile.OVERWORLD_ONLY -> current.copy(
                    pruningProfile = profile,
                    includeOverworld = true,
                    includeNether = false,
                    includeTheEnd = false
                )
                PruningProfile.SPEED -> current.copy(
                    pruningProfile = profile,
                    includeOverworld = true,
                    includeNether = false,
                    includeTheEnd = false,
                    keepOriginalNbt = true
                )
                PruningProfile.CUSTOM -> current.copy(
                    pruningProfile = profile
                )
            }
        }
    }

    fun toggleDimension(dimension: String, enabled: Boolean) {
        _uiState.update { current ->
            val updated = when (dimension) {
                "OVERWORLD" -> current.copy(includeOverworld = enabled)
                "NETHER" -> current.copy(includeNether = enabled)
                "THE_END" -> current.copy(includeTheEnd = enabled)
                else -> current
            }
            updated.copy(pruningProfile = PruningProfile.CUSTOM)
        }
    }

    fun setKeepOriginalNbt(keep: Boolean) {
        _uiState.update { it.copy(keepOriginalNbt = keep) }
    }

    fun setOverrideWorldName(name: String) {
        _uiState.update { it.copy(overrideWorldName = name) }
    }

    fun startPruning() {
        val currentState = _uiState.value
        val world = currentState.worldInfo ?: return

        _uiState.update {
            it.copy(
                stage = PrunerStage.PRUNING,
                pruningProgress = 0,
                pruningStageText = "启动瘦身引擎...",
                pruningLogs = listOf("[SYSTEM] 开始执行存档瘦身与维度裁剪...")
            )
        }

        // Use INPUT format so Chunker preserves original edition/version and only executes dimension pruning
        val config = ConversionConfig(
            inputDir = archiveManager.inputDir,
            outputDir = archiveManager.outputDir,
            targetFormat = ChunkerFormat.FORMAT_INPUT,
            keepOriginalNbt = currentState.keepOriginalNbt,
            pruningProfile = currentState.pruningProfile,
            includeOverworld = currentState.includeOverworld,
            includeNether = currentState.includeNether,
            includeTheEnd = currentState.includeTheEnd,
            overrideWorldName = currentState.overrideWorldName.ifBlank { null }
        )

        val serviceIntent = Intent(getApplication(), ConversionForegroundService::class.java)
        getApplication<Application>().startService(serviceIntent)
        val service = boundService
        if (service != null) {
            service.startConversion(config)
        } else {
            pendingConfig = config
        }
    }

    fun cancelPruning() {
        boundService?.cancelConversion()
        _uiState.update {
            it.copy(
                stage = PrunerStage.CONFIGURE,
                pruningLogs = it.pruningLogs + "[CANCEL] 瘦身已被用户主动取消。"
            )
        }
    }

    fun exportPrunedWorld(targetUri: Uri, packAsArchive: Boolean, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val worldName = _uiState.value.overrideWorldName.ifBlank {
                _uiState.value.worldInfo?.name ?: "Pruned_World"
            }
            val isBedrock = _uiState.value.worldInfo?.platform?.isBedrock ?: true

            val result = archiveManager.exportToUri(targetUri, worldName, packAsArchive, isBedrock)
            _uiState.update { it.copy(isExporting = false) }
            result.onSuccess {
                _uiState.value.latestHistoryId?.let { id ->
                    withContext(Dispatchers.IO) {
                        historyManager.updateExportLocation(id, targetUri.toString())
                    }
                }
                onDone(true)
            }.onFailure {
                onDone(false)
            }
        }
    }

    fun reset() {
        viewModelScope.launch(Dispatchers.IO) {
            archiveManager.cleanWorkspace()
            withContext(Dispatchers.Main) {
                _uiState.value = PrunerUiState()
            }
        }
    }

    private fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
