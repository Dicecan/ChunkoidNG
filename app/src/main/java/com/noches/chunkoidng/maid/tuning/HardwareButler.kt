package com.noches.chunkoidng.maid.tuning

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import com.noches.chunkoidng.maid.model.DeviceHardwareInfo
import com.noches.chunkoidng.maid.model.DeviceTier
import java.io.File

class HardwareButler {

    @Volatile
    private var cachedInfo: DeviceHardwareInfo? = null
    private val lock = Any()

    fun inspect(context: Context, forceRefresh: Boolean = false): DeviceHardwareInfo {
        if (!forceRefresh) {
            cachedInfo?.let { return it }
        }
        return synchronized(lock) {
            if (!forceRefresh) {
                cachedInfo?.let { return it }
            }
            val evaluated = evaluateHardware(context)
            cachedInfo = evaluated
            evaluated
        }
    }

    private fun evaluateHardware(context: Context): DeviceHardwareInfo {
        val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val maxFreqMhz = readMaxCpuFreqMhz()
        val computeScore = measureComputeScore(cpuCores, maxFreqMhz)

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val totalRamMb = (memInfo.totalMem / (1024 * 1024)).toInt()
        val availRamMb = (memInfo.availMem / (1024 * 1024)).toInt()
        val isLowRam = am?.isLowRamDevice ?: (totalRamMb < 3500)
        val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

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
        } catch (_: Exception) {}
        return if (maxFreqKhz > 0) (maxFreqKhz / 1000).coerceIn(1000, 4500) else 2400
    }

    private fun measureComputeScore(cpuCores: Int, maxFreqMhz: Int): Int {
        return try {
            val start = System.nanoTime()
            var acc = 0L
            val iterations = 80_000
            for (i in 0 until iterations) {
                acc = (acc * 31L + i.toLong()) xor 0x5DEECE66DL
                acc = (acc ushr 16) or (acc shl 48)
            }
            val elapsedNs = (System.nanoTime() - start).coerceAtLeast(1L)
            val elapsedMs = elapsedNs / 1_000_000.0

            val rawMipsEstimate = (iterations / elapsedMs) * 10.0
            val frequencyScale = (maxFreqMhz / 2000.0).coerceIn(0.5, 2.5)
            val coreScale = Math.sqrt(cpuCores.toDouble()).coerceIn(1.0, 3.5)

            ((rawMipsEstimate * 0.4 + 1000.0) * frequencyScale * 0.6 * coreScale * 0.5).toInt().coerceIn(300, 6000)
        } catch (_: Exception) {
            1500
        }
    }
}
