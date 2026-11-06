package com.noches.chunkoidng.maid.model

data class MaidAuditReport(
    val timestamp: Long = System.currentTimeMillis(),
    val deviceModel: String,
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
