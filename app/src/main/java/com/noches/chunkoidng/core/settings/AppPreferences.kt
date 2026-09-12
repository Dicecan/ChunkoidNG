package com.noches.chunkoidng.core.settings

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("chunkoid_prefs", Context.MODE_PRIVATE)

    var dynamicColorEnabled: Boolean
        get() = prefs.getBoolean("dynamic_color", true)
        set(value) = prefs.edit().putBoolean("dynamic_color", value).apply()

    var lowRamModeEnabled: Boolean
        get() = prefs.getBoolean("low_ram_mode", false)
        set(value) = prefs.edit().putBoolean("low_ram_mode", value).apply()

    var wakeLockEnabled: Boolean
        get() = prefs.getBoolean("wake_lock", true)
        set(value) = prefs.edit().putBoolean("wake_lock", value).apply()

    var keepOriginalNbt: Boolean
        get() = prefs.getBoolean("keep_original_nbt", false)
        set(value) = prefs.edit().putBoolean("keep_original_nbt", value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration", true)
        set(value) = prefs.edit().putBoolean("vibration", value).apply()

    var maxMemoryMb: Float
        get() = prefs.getFloat("max_memory_mb", 4096f)
        set(value) = prefs.edit().putFloat("max_memory_mb", value).apply()
}

