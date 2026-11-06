package com.noches.chunkoidng.maid.tuning

import android.content.Context
import com.noches.chunkoidng.maid.model.DeviceHardwareInfo
import com.noches.chunkoidng.maid.model.PerformanceLimits

class BudgetGovernor(private val hardwareButler: HardwareButler) {

    fun clampMaxMemory(context: Context, requestedMb: Int): Int {
        val hw = hardwareButler.inspect(context)
        val totalRamMb = hw.totalRamMb
        val reservedRamMb = (totalRamMb * 0.25f).toInt().coerceAtLeast(1024)
        val deviceLimitMb = (totalRamMb - reservedRamMb).coerceAtLeast(512)
        return requestedMb.coerceIn(256, deviceLimitMb)
    }

    fun buildJvmArguments(
        context: Context,
        limits: PerformanceLimits,
        cacheDir: java.io.File,
        customJvmOptions: List<String> = emptyList()
    ): List<String> {
        val maxMemMb = clampMaxMemory(context, limits.jvmMaxMemoryMb)
        val initialHeapMb = if (maxMemMb <= 768) 64 else 128

        val args = mutableListOf(
            "-Xms${initialHeapMb}m",
            "-Xmx${maxMemMb}m",
            "-Djava.io.tmpdir=${cacheDir.absolutePath}"
        )

        if (limits.useSerialGc) {
            args.add("-XX:+UseSerialGC")
            limits.minHeapFreeRatio?.let { args.add("-XX:MinHeapFreeRatio=$it") }
            limits.maxHeapFreeRatio?.let { args.add("-XX:MaxHeapFreeRatio=$it") }
        }

        val parallelism = limits.parallelism.coerceAtLeast(1)
        args.add("-Djava.util.concurrent.ForkJoinPool.common.parallelism=$parallelism")

        args.addAll(customJvmOptions)
        return args
    }
}
