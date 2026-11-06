package com.noches.chunkoidng.maid.model

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
