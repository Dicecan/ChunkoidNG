package com.noches.chunkoidng.core.crash

import android.app.Application
import android.content.Intent
import android.os.Process
import com.noches.chunkoidng.ui.screens.crash.CrashActivity
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

class CrashHandler private constructor(private val app: Application) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
    private val isCrashing = AtomicBoolean(false)

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (!isCrashing.compareAndSet(false, true)) {
            defaultHandler?.uncaughtException(thread, throwable)
            return
        }

        try {
            val report = CrashReport.buildFromThrowable(app, thread, throwable)
            val logFile = CrashLogManager.saveCrashLog(app, report)

            val intent = Intent(app, CrashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(CrashActivity.EXTRA_CRASH_FILE, logFile.absolutePath)
                putExtra(CrashActivity.EXTRA_CRASH_SUMMARY, report.formatSummary())
            }
            app.startActivity(intent)
        } catch (_: Throwable) {
            defaultHandler?.uncaughtException(thread, throwable)
            return
        } finally {
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    companion object {
        @Volatile
        private var instance: CrashHandler? = null

        fun init(app: Application): CrashHandler {
            return instance ?: synchronized(this) {
                instance ?: CrashHandler(app).also {
                    instance = it
                    Thread.setDefaultUncaughtExceptionHandler(it)
                }
            }
        }
    }
}
