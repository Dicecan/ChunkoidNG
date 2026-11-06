package com.noches.chunkoidng.core.settings

import android.content.Context
import com.noches.chunkoidng.maid.Maid

typealias DeviceTier = com.noches.chunkoidng.maid.model.DeviceTier
typealias PerformanceMode = com.noches.chunkoidng.maid.model.PerformanceMode
typealias DeviceHardwareInfo = com.noches.chunkoidng.maid.model.DeviceHardwareInfo
typealias PerformanceLimits = com.noches.chunkoidng.maid.model.PerformanceLimits

object DevicePerformanceEvaluator {

    fun evaluate(context: Context, forceRefresh: Boolean = false): DeviceHardwareInfo {
        return Maid.hardwareButler.inspect(context, forceRefresh)
    }

    fun getTunedLimits(info: DeviceHardwareInfo): PerformanceLimits {
        return Maid.performanceRegistry.getTunedLimits(info)
    }

    fun getStrictLimits(info: DeviceHardwareInfo): PerformanceLimits {
        return Maid.performanceRegistry.getStrictLimits(info)
    }

    fun getCustomLimits(prefs: AppPreferences, info: DeviceHardwareInfo): PerformanceLimits {
        return Maid.performanceRegistry.getCustomLimits(prefs, info)
    }
}
