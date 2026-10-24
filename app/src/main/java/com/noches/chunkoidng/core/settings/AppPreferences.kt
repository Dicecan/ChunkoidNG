package com.noches.chunkoidng.core.settings

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("chunkoid_prefs", Context.MODE_PRIVATE)

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    var lastSeenVersionCode: Long
        get() = prefs.getLong("last_seen_version_code", 0L)
        set(value) = prefs.edit().putLong("last_seen_version_code", value).apply()

    fun getAppVersionCode(): Long {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (_: Exception) {
            1L
        }
    }

    val isFirstLaunchOrUpdate: Boolean
        get() {
            val currentCode = getAppVersionCode()
            val last = lastSeenVersionCode
            if (!onboardingCompleted) return true
            if (last < currentCode) return true
            return false
        }

    val isUpdate: Boolean
        get() {
            val currentCode = getAppVersionCode()
            val last = lastSeenVersionCode
            return onboardingCompleted && last in 1 until currentCode
        }

    fun markWelcomeCompleted() {
        onboardingCompleted = true
        lastSeenVersionCode = getAppVersionCode()
    }

    var performanceMode: String
        get() = prefs.getString("performance_mode", PerformanceMode.TUNED.id) ?: PerformanceMode.TUNED.id
        set(value) = prefs.edit().putString("performance_mode", value).apply()

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

    var appLanguage: String
        get() = prefs.getString("app_language", "system") ?: "system"
        set(value) = prefs.edit().putString("app_language", value).apply()

    var notificationPermissionRequested: Boolean
        get() = prefs.getBoolean("notification_permission_requested", false)
        set(value) = prefs.edit().putBoolean("notification_permission_requested", value).apply()

    var customParallelism: Int
        get() = prefs.getInt("custom_parallelism", 2)
        set(value) = prefs.edit().putInt("custom_parallelism", value).apply()

    var customUseSerialGc: Boolean
        get() = prefs.getBoolean("custom_use_serial_gc", false)
        set(value) = prefs.edit().putBoolean("custom_use_serial_gc", value).apply()

    var archiveMaxEntryMb: Int
        get() = prefs.getInt("archive_max_entry_mb", 512)
        set(value) = prefs.edit().putInt("archive_max_entry_mb", value).apply()

    var archiveMaxTotalMb: Int
        get() = prefs.getInt("archive_max_total_mb", 2048)
        set(value) = prefs.edit().putInt("archive_max_total_mb", value).apply()

    var archiveMaxEntries: Int
        get() = prefs.getInt("archive_max_entries", 100_000)
        set(value) = prefs.edit().putInt("archive_max_entries", value).apply()

    var nbtArrayPageSize: Int
        get() = prefs.getInt("nbt_array_page_size", 100)
        set(value) = prefs.edit().putInt("nbt_array_page_size", value).apply()

    var packMaxFrames: Int
        get() = prefs.getInt("pack_max_frames", 100_000)
        set(value) = prefs.edit().putInt("pack_max_frames", value).apply()

    var midiMaxFileMb: Int
        get() = prefs.getInt("midi_max_file_mb", 128)
        set(value) = prefs.edit().putInt("midi_max_file_mb", value).apply()

    var midiMaxNotes: Int
        get() = prefs.getInt("midi_max_notes", 1_000_000)
        set(value) = prefs.edit().putInt("midi_max_notes", value).apply()

    fun getEffectiveLimits(): PerformanceLimits {
        val hardwareInfo = DevicePerformanceEvaluator.evaluate(context)
        return when (PerformanceMode.fromId(performanceMode)) {
            PerformanceMode.TUNED -> DevicePerformanceEvaluator.getTunedLimits(hardwareInfo)
            PerformanceMode.STRICT -> DevicePerformanceEvaluator.getStrictLimits(hardwareInfo)
            PerformanceMode.CUSTOM -> DevicePerformanceEvaluator.getCustomLimits(this, hardwareInfo)
        }
    }
}
