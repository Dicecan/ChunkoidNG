package com.noches.chunkoidng.maid

import android.content.Context
import com.noches.chunkoidng.maid.audit.HealthDiagnostic
import com.noches.chunkoidng.maid.audit.SecurityAuditor
import com.noches.chunkoidng.maid.defense.ArchiveShield
import com.noches.chunkoidng.maid.defense.BinaryGuard
import com.noches.chunkoidng.maid.defense.PathSanitizer
import com.noches.chunkoidng.maid.model.MaidAuditReport
import com.noches.chunkoidng.maid.model.PerformanceLimits
import com.noches.chunkoidng.maid.tuning.BudgetGovernor
import com.noches.chunkoidng.maid.tuning.HardwareButler
import com.noches.chunkoidng.maid.tuning.PagingAdvisor
import com.noches.chunkoidng.maid.tuning.PerformanceRegistry

object Maid {

    class DefenseSubsystem internal constructor(
        val pathSanitizer: PathSanitizer,
        val archiveShield: ArchiveShield,
        val binaryGuard: BinaryGuard
    )

    class TuningSubsystem internal constructor(
        val hardwareButler: HardwareButler,
        val budgetGovernor: BudgetGovernor,
        val pagingAdvisor: PagingAdvisor,
        val performanceRegistry: PerformanceRegistry,
        private val contextProvider: () -> Context?
    ) {
        val limits: PerformanceLimits
            get() {
                val ctx = contextProvider() ?: error("Maid must be initialized with context before accessing limits")
                return performanceRegistry.getEffectiveLimits(ctx)
            }
    }

    class AuditSubsystem internal constructor(
        val securityAuditor: SecurityAuditor,
        val healthDiagnostic: HealthDiagnostic,
        private val contextProvider: () -> Context?
    ) {
        fun createReport(): MaidAuditReport {
            val ctx = contextProvider() ?: error("Maid must be initialized with context before generating report")
            return healthDiagnostic.generateReport(ctx)
        }
    }

    @Volatile
    private var appContext: Context? = null

    val securityAuditor = SecurityAuditor(capacity = 100)
    val hardwareButler = HardwareButler()
    val performanceRegistry = PerformanceRegistry(hardwareButler)
    val budgetGovernor = BudgetGovernor(hardwareButler)
    val pagingAdvisor = PagingAdvisor(securityAuditor)
    val pathSanitizer = PathSanitizer(securityAuditor)
    val archiveShield = ArchiveShield(pathSanitizer, securityAuditor)
    val binaryGuard = BinaryGuard(securityAuditor)
    val healthDiagnostic = HealthDiagnostic(securityAuditor, hardwareButler, performanceRegistry)

    val defense = DefenseSubsystem(pathSanitizer, archiveShield, binaryGuard)
    val tuning = TuningSubsystem(hardwareButler, budgetGovernor, pagingAdvisor, performanceRegistry) { appContext }
    val audit = AuditSubsystem(securityAuditor, healthDiagnostic) { appContext }

    fun init(context: Context) {
        if (appContext == null) {
            synchronized(this) {
                if (appContext == null) {
                    val app = context.applicationContext ?: context
                    appContext = app
                    hardwareButler.inspect(app)
                }
            }
        }
    }
}
