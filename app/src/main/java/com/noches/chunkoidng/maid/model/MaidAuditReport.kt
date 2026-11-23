package com.noches.chunkoidng.maid.model

import android.os.Build

data class MaidAuditReport(
    val timestamp: Long = System.currentTimeMillis(),
    val deviceModel: String,
    val androidVersion: String = "Android ${Build.VERSION.RELEASE ?: "Unknown"} (API ${Build.VERSION.SDK_INT})",
    val deviceTier: DeviceTier,
    val cpuCores: Int,
    val cpuMaxFreqMhz: Int,
    val computeScore: Int,
    val totalRamMb: Int,
    val freeRamMb: Int,
    val currentHeapUsedMb: Long,
    val currentHeapMaxMb: Long,
    val freeStorageMb: Long,
    val activeLimits: PerformanceLimits,
    val recentViolations: List<SecurityViolation>
)
