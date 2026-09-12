package com.noches.chunkoidng.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.noches.chunkoidng.core.settings.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            dynamicColorEnabled = prefs.dynamicColorEnabled,
            lowRamModeEnabled = prefs.lowRamModeEnabled,
            wakeLockEnabled = prefs.wakeLockEnabled,
            keepOriginalNbt = prefs.keepOriginalNbt,
            vibrationEnabled = prefs.vibrationEnabled,
            maxMemoryMb = prefs.maxMemoryMb
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateDynamicColor(enabled: Boolean) {
        prefs.dynamicColorEnabled = enabled
        _uiState.update { it.copy(dynamicColorEnabled = enabled) }
    }

    fun updateLowRamMode(enabled: Boolean) {
        prefs.lowRamModeEnabled = enabled
        _uiState.update { it.copy(lowRamModeEnabled = enabled) }
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
}

data class SettingsUiState(
    val dynamicColorEnabled: Boolean = true,
    val lowRamModeEnabled: Boolean = false,
    val wakeLockEnabled: Boolean = true,
    val keepOriginalNbt: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val maxMemoryMb: Float = 4096f
)

