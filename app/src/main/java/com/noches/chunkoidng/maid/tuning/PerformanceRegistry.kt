package com.noches.chunkoidng.maid.tuning

import android.content.Context
import com.noches.chunkoidng.core.settings.AppPreferences
import com.noches.chunkoidng.maid.model.DeviceHardwareInfo
import com.noches.chunkoidng.maid.model.DeviceTier
import com.noches.chunkoidng.maid.model.PerformanceLimits
import com.noches.chunkoidng.maid.model.PerformanceMode

class PerformanceRegistry(private val hardwareButler: HardwareButler) {

    fun getEffectiveLimits(context: Context): PerformanceLimits {
        val hw = hardwareButler.inspect(context)
        val prefs = AppPreferences(context)
        val mode = PerformanceMode.fromId(prefs.performanceMode)

        return when (mode) {
            PerformanceMode.TUNED -> getTunedLimits(hw)
            PerformanceMode.STRICT -> getStrictLimits(hw)
            PerformanceMode.CUSTOM -> getCustomLimits(prefs, hw)
        }
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
