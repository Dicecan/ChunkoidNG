package com.noches.chunkoidng.maid.audit

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import com.noches.chunkoidng.maid.model.DeviceTier
import com.noches.chunkoidng.maid.model.MaidAuditReport
import com.noches.chunkoidng.maid.model.PerformanceLimits
import com.noches.chunkoidng.maid.tuning.HardwareButler
import com.noches.chunkoidng.maid.tuning.PerformanceRegistry

class HealthDiagnostic(
    private val securityAuditor: SecurityAuditor,
    private val hardwareButler: HardwareButler,
    private val performanceRegistry: PerformanceRegistry
) {

    fun generateReport(context: Context): MaidAuditReport {
        val hw = hardwareButler.inspect(context)
        val limits = performanceRegistry.getEffectiveLimits(context)

        val rt = Runtime.getRuntime()
        val heapTotalMb = rt.totalMemory() / (1024 * 1024)
        val heapFreeMb = rt.freeMemory() / (1024 * 1024)
        val heapUsedMb = (heapTotalMb - heapFreeMb).coerceAtLeast(0)
        val heapMaxMb = rt.maxMemory() / (1024 * 1024)

        val freeStorageMb = try {
            Environment.getDataDirectory().freeSpace / (1024 * 1024)
        } catch (_: Exception) {
            hw.freeStorageMb
        }

        return MaidAuditReport(
            deviceModel = hw.model,
            deviceTier = hw.tier,
            cpuCores = hw.cpuCores,
            cpuMaxFreqMhz = hw.cpuMaxFreqMhz,
            computeScore = hw.computeScore,
            totalRamMb = hw.totalRamMb,
            freeRamMb = hw.availRamMb,
            currentHeapUsedMb = heapUsedMb,
            currentHeapMaxMb = heapMaxMb,
            freeStorageMb = freeStorageMb,
            activeLimits = limits,
            recentViolations = securityAuditor.getRecentViolations()
        )
    }

    fun isLowMemoryCondition(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(mi)
        return mi.lowMemory
    }
}
