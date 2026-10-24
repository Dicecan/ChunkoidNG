package com.noches.chunkoidng.core.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.annotation.StringRes
import com.noches.chunkoidng.R
import java.io.File

enum class DeviceTier(@get:StringRes val titleRes: Int) {
    FLAGSHIP(R.string.welcome_tier_flagship),
    HIGH_END(R.string.welcome_tier_high_end),
    MID_RANGE(R.string.welcome_tier_mid_range),
    ENTRY_LEVEL(R.string.welcome_tier_entry)
}

enum class PerformanceMode(val id: String, @get:StringRes val titleRes: Int, @get:StringRes val descRes: Int) {
    TUNED("tuned", R.string.welcome_mode_tuned_title, R.string.welcome_mode_tuned_desc),
    STRICT("strict", R.string.welcome_mode_strict_title, R.string.welcome_mode_strict_desc),
    CUSTOM("custom", R.string.welcome_mode_custom_title, R.string.welcome_mode_custom_desc);

    companion object {
        fun fromId(id: String): PerformanceMode {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: TUNED
        }
    }
}

data class DeviceHardwareInfo(
    val model: String,
    val manufacturer: String,
    val cpuCores: Int,
    val cpuMaxFreqMhz: Int,
    val computeScore: Int,
    val totalRamMb: Int,
    val availRamMb: Int,
    val is64Bit: Boolean,
    val isLowRamDevice: Boolean,
    val freeStorageMb: Long,
    val androidVersion: Int,
    val tier: DeviceTier
)

data class PerformanceLimits(
    val jvmMaxMemoryMb: Int,
    val parallelism: Int,
    val useSerialGc: Boolean,
    val minHeapFreeRatio: Int? = null,
    val maxHeapFreeRatio: Int? = null,
    val archiveMaxEntryBytes: Long,
    val archiveMaxTotalBytes: Long,
    val archiveMaxEntries: Int,
    val nbtArrayPageSize: Int,
    val packMaxFrames: Int,
    val midiMaxFileBytes: Long,
    val midiMaxNotes: Int
)

object DevicePerformanceEvaluator {

    private fun readMaxCpuFreqMhz(): Int {
        var maxFreqKhz = 0
        try {
            for (i in 0 until 16) {
                val f = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                if (f.exists()) {
                    val line = f.readText().trim()
                    val khz = line.toIntOrNull() ?: 0
                    if (khz > maxFreqKhz) maxFreqKhz = khz
                }
            }
        } catch (_: Exception) {
        }
        return if (maxFreqKhz > 0) (maxFreqKhz / 1000).coerceIn(1000, 4500) else 2400
    }

    private fun measureComputeScore(cpuCores: Int, maxFreqMhz: Int): Int {
        return try {
            val start = System.nanoTime()
            var hash = 0x811c9dc5.toInt()
            val iterations = 400_000
            for (i in 0 until iterations) {
                hash = (hash xor i) * 16777619
            }
            val buf = ByteArray(1024 * 512)
            for (i in 0 until 1024 step 32) {
                buf[i] = (hash and 0xFF).toByte()
            }
            val elapsedNs = (System.nanoTime() - start).coerceAtLeast(1)
            val elapsedMs = elapsedNs / 1_000_000f
            val baseSpeed = (1200f / elapsedMs).coerceIn(100f, 1500f)
            val coreMultiplier = 1.0f + (cpuCores - 1) * 0.45f
            val freqMultiplier = (maxFreqMhz / 2200f).coerceIn(0.7f, 1.6f)
            (baseSpeed * coreMultiplier * freqMultiplier).toInt().coerceIn(300, 6000)
        } catch (_: Exception) {
            (cpuCores * 250).coerceIn(500, 2500)
        }
    }

    fun evaluate(context: Context): DeviceHardwareInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        var totalRamMb = 4096
        var availRamMb = 2048
        var isLowRam = false

        if (activityManager != null) {
            activityManager.getMemoryInfo(memInfo)
            totalRamMb = (memInfo.totalMem / (1024 * 1024)).toInt().coerceAtLeast(512)
            availRamMb = (memInfo.availMem / (1024 * 1024)).toInt().coerceAtLeast(256)
            isLowRam = activityManager.isLowRamDevice
        }

        val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        val maxFreqMhz = readMaxCpuFreqMhz()
        val computeScore = measureComputeScore(cpuCores, maxFreqMhz)

        val freeStorageMb = try {
            val stat = Environment.getDataDirectory()
            stat.freeSpace / (1024 * 1024)
        } catch (_: Exception) {
            10240L
        }

        val tier = when {
            totalRamMb >= 9000 && computeScore >= 2800 && is64Bit && !isLowRam -> DeviceTier.FLAGSHIP
            totalRamMb >= 5800 && computeScore >= 1800 && is64Bit && !isLowRam -> DeviceTier.HIGH_END
            totalRamMb >= 3500 && computeScore >= 900 -> DeviceTier.MID_RANGE
            else -> DeviceTier.ENTRY_LEVEL
        }

        val model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".trim()

        return DeviceHardwareInfo(
            model = model,
            manufacturer = Build.MANUFACTURER,
            cpuCores = cpuCores,
            cpuMaxFreqMhz = maxFreqMhz,
            computeScore = computeScore,
            totalRamMb = totalRamMb,
            availRamMb = availRamMb,
            is64Bit = is64Bit,
            isLowRamDevice = isLowRam,
            freeStorageMb = freeStorageMb,
            androidVersion = Build.VERSION.SDK_INT,
            tier = tier
        )
    }

    fun getTunedLimits(info: DeviceHardwareInfo): PerformanceLimits {
        return when (info.tier) {
            DeviceTier.FLAGSHIP -> PerformanceLimits(
                jvmMaxMemoryMb = ((info.totalRamMb * 0.55f).toInt()).coerceIn(4096, 7168),
                parallelism = (info.cpuCores - 2).coerceIn(2, 4),
                useSerialGc = false,
                archiveMaxEntryBytes = 1024L * 1024 * 1024,
                archiveMaxTotalBytes = 4096L * 1024 * 1024,
                archiveMaxEntries = 200_000,
                nbtArrayPageSize = 200,
                packMaxFrames = 150_000,
                midiMaxFileBytes = 256L * 1024 * 1024,
                midiMaxNotes = 2_000_000
            )
            DeviceTier.HIGH_END -> PerformanceLimits(
                jvmMaxMemoryMb = ((info.totalRamMb * 0.5f).toInt()).coerceIn(3072, 5120),
                parallelism = (info.cpuCores - 2).coerceIn(2, 3),
                useSerialGc = false,
                archiveMaxEntryBytes = 512L * 1024 * 1024,
                archiveMaxTotalBytes = 2048L * 1024 * 1024,
                archiveMaxEntries = 120_000,
                nbtArrayPageSize = 100,
                packMaxFrames = 100_000,
                midiMaxFileBytes = 128L * 1024 * 1024,
                midiMaxNotes = 1_000_000
            )
            DeviceTier.MID_RANGE -> PerformanceLimits(
                jvmMaxMemoryMb = ((info.totalRamMb * 0.45f).toInt()).coerceIn(2048, 3072),
                parallelism = 2,
                useSerialGc = true,
                minHeapFreeRatio = 15,
                maxHeapFreeRatio = 30,
                archiveMaxEntryBytes = 384L * 1024 * 1024,
                archiveMaxTotalBytes = 1536L * 1024 * 1024,
                archiveMaxEntries = 80_000,
                nbtArrayPageSize = 100,
                packMaxFrames = 80_000,
                midiMaxFileBytes = 64L * 1024 * 1024,
                midiMaxNotes = 600_000
            )
            DeviceTier.ENTRY_LEVEL -> PerformanceLimits(
                jvmMaxMemoryMb = ((info.totalRamMb * 0.35f).toInt()).coerceIn(512, 1280),
                parallelism = 1,
                useSerialGc = true,
                minHeapFreeRatio = 10,
                maxHeapFreeRatio = 20,
                archiveMaxEntryBytes = 256L * 1024 * 1024,
                archiveMaxTotalBytes = 1024L * 1024 * 1024,
                archiveMaxEntries = 40_000,
                nbtArrayPageSize = 50,
                packMaxFrames = 30_000,
                midiMaxFileBytes = 32L * 1024 * 1024,
                midiMaxNotes = 250_000
            )
        }
    }

    fun getStrictLimits(info: DeviceHardwareInfo): PerformanceLimits {
        return PerformanceLimits(
            jvmMaxMemoryMb = ((info.totalRamMb * 0.35f).toInt()).coerceIn(512, 1280),
            parallelism = 1,
            useSerialGc = true,
            minHeapFreeRatio = 10,
            maxHeapFreeRatio = 20,
            archiveMaxEntryBytes = 256L * 1024 * 1024,
            archiveMaxTotalBytes = 1024L * 1024 * 1024,
            archiveMaxEntries = 30_000,
            nbtArrayPageSize = 50,
            packMaxFrames = 25_000,
            midiMaxFileBytes = 32L * 1024 * 1024,
            midiMaxNotes = 200_000
        )
    }

    fun getCustomLimits(prefs: AppPreferences, info: DeviceHardwareInfo): PerformanceLimits {
        val safeMaxMb = prefs.maxMemoryMb.toInt().coerceIn(256, (info.totalRamMb * 0.8f).toInt())
        return PerformanceLimits(
            jvmMaxMemoryMb = safeMaxMb,
            parallelism = prefs.customParallelism.coerceIn(1, info.cpuCores),
            useSerialGc = prefs.customUseSerialGc,
            minHeapFreeRatio = if (prefs.customUseSerialGc) 10 else null,
            maxHeapFreeRatio = if (prefs.customUseSerialGc) 25 else null,
            archiveMaxEntryBytes = prefs.archiveMaxEntryMb.toLong() * 1024L * 1024L,
            archiveMaxTotalBytes = prefs.archiveMaxTotalMb.toLong() * 1024L * 1024L,
            archiveMaxEntries = prefs.archiveMaxEntries,
            nbtArrayPageSize = prefs.nbtArrayPageSize,
            packMaxFrames = prefs.packMaxFrames,
            midiMaxFileBytes = prefs.midiMaxFileMb.toLong() * 1024L * 1024L,
            midiMaxNotes = prefs.midiMaxNotes
        )
    }
}
