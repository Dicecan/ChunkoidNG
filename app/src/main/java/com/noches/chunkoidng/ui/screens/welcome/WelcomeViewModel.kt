package com.noches.chunkoidng.ui.screens.welcome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noches.chunkoidng.core.runtime.JavaRuntimeEnvironment
import com.noches.chunkoidng.core.settings.AppPreferences
import com.noches.chunkoidng.core.settings.DeviceHardwareInfo
import com.noches.chunkoidng.core.settings.DevicePerformanceEvaluator
import com.noches.chunkoidng.core.settings.PerformanceMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WelcomeUiState(
    val selectedLanguage: String,
    val hardwareInfo: DeviceHardwareInfo,
    val selectedMode: PerformanceMode,
    val customMaxMemoryMb: Float,
    val customParallelism: Int,
    val customUseSerialGc: Boolean,
    val archiveMaxEntryMb: Int,
    val archiveMaxTotalMb: Int,
    val archiveMaxEntries: Int,
    val nbtArrayPageSize: Int,
    val packMaxFrames: Int,
    val midiMaxFileMb: Int,
    val midiMaxNotes: Int,
    val isCustomExpanded: Boolean = false
)

class WelcomeViewModel(application: Application) : AndroidViewModel(application) {
    val prefs = AppPreferences(application)
    private val hardware = DevicePerformanceEvaluator.evaluate(application)
    val runtimeEnv = JavaRuntimeEnvironment(application)

    private val _isRootfsReady = MutableStateFlow(runtimeEnv.isRootfsReady())
    val isRootfsReady: StateFlow<Boolean> = _isRootfsReady.asStateFlow()

    private val _isDeployingEnvironment = MutableStateFlow(false)
    val isDeployingEnvironment: StateFlow<Boolean> = _isDeployingEnvironment.asStateFlow()

    private val _deploymentProgress = MutableStateFlow(if (runtimeEnv.isRootfsReady()) 100 else 0)
    val deploymentProgress: StateFlow<Int> = _deploymentProgress.asStateFlow()

    private val _deploymentStatusText = MutableStateFlow("")
    val deploymentStatusText: StateFlow<String> = _deploymentStatusText.asStateFlow()

    fun checkEnvironmentReady() {
        _isRootfsReady.value = runtimeEnv.isRootfsReady()
    }

    fun deployEnvironment() {
        if (_isDeployingEnvironment.value) return
        viewModelScope.launch {
            _isDeployingEnvironment.value = true
            _deploymentProgress.value = 0
            _deploymentStatusText.value = ""
            try {
                runtimeEnv.extractRootFS().collect { progress ->
                    _deploymentProgress.value = progress
                }
                runtimeEnv.ensureCliJar()
                _isRootfsReady.value = runtimeEnv.isRootfsReady()
            } catch (e: Exception) {
                _deploymentStatusText.value = e.localizedMessage ?: "Deployment failed"
            } finally {
                _isDeployingEnvironment.value = false
            }
        }
    }

    private val _uiState = MutableStateFlow(
        WelcomeUiState(
            selectedLanguage = prefs.appLanguage,
            hardwareInfo = hardware,
            selectedMode = PerformanceMode.fromId(prefs.performanceMode),
            customMaxMemoryMb = prefs.maxMemoryMb,
            customParallelism = prefs.customParallelism.coerceIn(1, hardware.cpuCores),
            customUseSerialGc = prefs.customUseSerialGc,
            archiveMaxEntryMb = prefs.archiveMaxEntryMb,
            archiveMaxTotalMb = prefs.archiveMaxTotalMb,
            archiveMaxEntries = prefs.archiveMaxEntries,
            nbtArrayPageSize = prefs.nbtArrayPageSize,
            packMaxFrames = prefs.packMaxFrames,
            midiMaxFileMb = prefs.midiMaxFileMb,
            midiMaxNotes = prefs.midiMaxNotes
        )
    )
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun updateLanguage(lang: String) {
        prefs.appLanguage = lang
        _uiState.update { it.copy(selectedLanguage = lang) }
    }

    fun selectMode(mode: PerformanceMode) {
        prefs.performanceMode = mode.id
        prefs.lowRamModeEnabled = (mode == PerformanceMode.STRICT)
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun toggleCustomExpanded() {
        _uiState.update { it.copy(isCustomExpanded = !it.isCustomExpanded) }
    }

    fun updateCustomMaxMemory(mb: Float) {
        prefs.maxMemoryMb = mb
        _uiState.update { it.copy(customMaxMemoryMb = mb) }
    }

    fun updateCustomParallelism(parallelism: Int) {
        prefs.customParallelism = parallelism
        _uiState.update { it.copy(customParallelism = parallelism) }
    }

    fun updateCustomUseSerialGc(enabled: Boolean) {
        prefs.customUseSerialGc = enabled
        _uiState.update { it.copy(customUseSerialGc = enabled) }
    }

    fun updateArchiveMaxEntryMb(mb: Int) {
        prefs.archiveMaxEntryMb = mb
        _uiState.update { it.copy(archiveMaxEntryMb = mb) }
    }

    fun updateArchiveMaxTotalMb(mb: Int) {
        prefs.archiveMaxTotalMb = mb
        _uiState.update { it.copy(archiveMaxTotalMb = mb) }
    }

    fun updateArchiveMaxEntries(entries: Int) {
        prefs.archiveMaxEntries = entries
        _uiState.update { it.copy(archiveMaxEntries = entries) }
    }

    fun updateNbtArrayPageSize(size: Int) {
        prefs.nbtArrayPageSize = size
        _uiState.update { it.copy(nbtArrayPageSize = size) }
    }

    fun updatePackMaxFrames(frames: Int) {
        prefs.packMaxFrames = frames
        _uiState.update { it.copy(packMaxFrames = frames) }
    }

    fun updateMidiMaxFileMb(mb: Int) {
        prefs.midiMaxFileMb = mb
        _uiState.update { it.copy(midiMaxFileMb = mb) }
    }

    fun updateMidiMaxNotes(notes: Int) {
        prefs.midiMaxNotes = notes
        _uiState.update { it.copy(midiMaxNotes = notes) }
    }

    fun completeOnboarding() {
        prefs.markWelcomeCompleted()
    }
}
