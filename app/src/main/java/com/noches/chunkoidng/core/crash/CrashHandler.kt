package com.noches.chunkoidng.core.crash

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import com.noches.chunkoidng.ui.screens.crash.CrashActivity
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

class CrashHandler private constructor(private val app: Application) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
    private val isCrashing = AtomicBoolean(false)

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val currentProcessName = getProcessName(app)
        if (currentProcessName?.endsWith(":crash") == true) {
            defaultHandler?.uncaughtException(thread, throwable)
            return
        }

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

    private fun getProcessName(app: Application): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName()
        }
        return try {
            val pid = Process.myPid()
            val am = app.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.runningAppProcesses?.find { it.pid == pid }?.processName
                ?: File("/proc/self/cmdline").readText().trim('\u0000', ' ', '\n', '\r')
        } catch (_: Exception) {
            null
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
