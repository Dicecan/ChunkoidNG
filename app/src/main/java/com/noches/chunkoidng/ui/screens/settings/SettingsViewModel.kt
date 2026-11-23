package com.noches.chunkoidng.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noches.chunkoidng.core.settings.AppPreferences
import com.noches.chunkoidng.core.settings.LocaleHelper
import com.noches.chunkoidng.core.runtime.JavaRuntimeEnvironment
import com.noches.chunkoidng.core.settings.PerformanceMode
import com.noches.chunkoidng.core.world.ArchiveManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            dynamicColorEnabled = prefs.dynamicColorEnabled,
            followSystemDarkMode = prefs.followSystemDarkMode,
            manualDarkMode = prefs.manualDarkMode,
            lowRamModeEnabled = prefs.lowRamModeEnabled,
            wakeLockEnabled = prefs.wakeLockEnabled,
            keepOriginalNbt = prefs.keepOriginalNbt,
            vibrationEnabled = prefs.vibrationEnabled,
            maxMemoryMb = prefs.maxMemoryMb,
            appLanguage = prefs.appLanguage,
            performanceMode = prefs.performanceMode,
            customParallelism = prefs.customParallelism,
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
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        LocaleHelper.observeLanguage(application)
            .onEach { lang ->
                _uiState.update { it.copy(appLanguage = lang) }
            }
            .launchIn(viewModelScope)

        AppPreferences.themeRevision
            .onEach {
                _uiState.update {
                    it.copy(
                        dynamicColorEnabled = prefs.dynamicColorEnabled,
                        followSystemDarkMode = prefs.followSystemDarkMode,
                        manualDarkMode = prefs.manualDarkMode
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun updateAppLanguage(language: String) {
        prefs.appLanguage = language
        LocaleHelper.notifyLanguageChanged(language)
        _uiState.update { it.copy(appLanguage = language) }
    }

    fun updatePerformanceMode(mode: String) {
        prefs.performanceMode = mode
        prefs.lowRamModeEnabled = (mode == PerformanceMode.STRICT.id)
        _uiState.update {
            it.copy(
                performanceMode = mode,
                lowRamModeEnabled = (mode == PerformanceMode.STRICT.id)
            )
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        prefs.dynamicColorEnabled = enabled
        _uiState.update { it.copy(dynamicColorEnabled = enabled) }
    }

    fun updateFollowSystemDarkMode(enabled: Boolean) {
        prefs.followSystemDarkMode = enabled
        _uiState.update { it.copy(followSystemDarkMode = enabled) }
    }

    fun updateManualDarkMode(isDark: Boolean) {
        prefs.manualDarkMode = isDark
        _uiState.update { it.copy(manualDarkMode = isDark) }
    }

    fun updateLowRamMode(enabled: Boolean) {
        prefs.lowRamModeEnabled = enabled
        val targetMode = if (enabled) PerformanceMode.STRICT.id else PerformanceMode.TUNED.id
        prefs.performanceMode = targetMode
        _uiState.update { it.copy(lowRamModeEnabled = enabled, performanceMode = targetMode) }
    }

    fun updateWakeLock(enabled: Boolean) {
        prefs.wakeLockEnabled = enabled
        _uiState.update { it.copy(wakeLockEnabled = enabled) }
    }

    fun updateKeepOriginalNbt(enabled: Boolean) {
        prefs.keepOriginalNbt = enabled
        _uiState.update { it.copy(keepOriginalNbt = enabled) }
    }

    fun updateVibration(enabled: Boolean) {
        prefs.vibrationEnabled = enabled
        _uiState.update { it.copy(vibrationEnabled = enabled) }
    }

    fun updateMaxMemory(mb: Float) {
        prefs.maxMemoryMb = mb
        _uiState.update { it.copy(maxMemoryMb = mb) }
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

    fun resetRootFs() {
        JavaRuntimeEnvironment(getApplication()).rootfsDir.deleteRecursively()
    }

    fun clearCache() {
        ArchiveManager(getApplication()).cleanWorkspace()
    }
}

data class SettingsUiState(
    val dynamicColorEnabled: Boolean = true,
    val followSystemDarkMode: Boolean = true,
    val manualDarkMode: Boolean = false,
    val lowRamModeEnabled: Boolean = false,
    val wakeLockEnabled: Boolean = true,
    val keepOriginalNbt: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val maxMemoryMb: Float = 4096f,
    val appLanguage: String = "system",
    val performanceMode: String = "tuned",
    val customParallelism: Int = 2,
    val customUseSerialGc: Boolean = false,
    val archiveMaxEntryMb: Int = 512,
    val archiveMaxTotalMb: Int = 2048,
    val archiveMaxEntries: Int = 100_000,
    val nbtArrayPageSize: Int = 100,
    val packMaxFrames: Int = 100_000,
    val midiMaxFileMb: Int = 128,
    val midiMaxNotes: Int = 1_000_000
)
