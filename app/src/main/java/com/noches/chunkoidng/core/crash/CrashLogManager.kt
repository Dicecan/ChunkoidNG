package com.noches.chunkoidng.core.crash

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogManager {
    private const val MAX_SAVED_LOGS = 20
    private const val LATEST_LOG_NAME = "latest_crash.log"

    fun getCrashDirectory(context: Context): File {
        val dir = context.getExternalFilesDir("crash_logs") ?: File(context.filesDir, "crash_logs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun saveCrashLog(context: Context, report: CrashReport): File {
        val dir = getCrashDirectory(context)
        val timeStampStr = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(report.timestamp))
        val logFile = File(dir, "crash_$timeStampStr.log")
        val content = report.formatFormattedText()

        logFile.writeText(content)

        try {
            val latestFile = File(dir, LATEST_LOG_NAME)
            latestFile.writeText(content)
        } catch (_: Exception) {}

        pruneOldLogs(dir)
        return logFile
    }

    fun listCrashLogs(context: Context): List<File> {
        val dir = getCrashDirectory(context)
        val logs = listCrashLogsInDirectory(dir)
        // Keep an older, orphaned latest report visible so it can still be deleted.
        return logs.ifEmpty {
            listOfNotNull(File(dir, LATEST_LOG_NAME).takeIf { it.isFile })
        }
    }

    private fun listCrashLogsInDirectory(dir: File): List<File> {
        val files = dir.listFiles { file ->
            file.isFile && file.name.startsWith("crash_") && file.name.endsWith(".log")
        } ?: emptyArray()
        return files.sortedByDescending { it.lastModified() }
    }

    fun getLatestCrashLog(context: Context): File? {
        val dir = getCrashDirectory(context)
        val latest = File(dir, LATEST_LOG_NAME)
        if (latest.exists() && latest.length() > 0) {
            return latest
        }
        return listCrashLogs(context).firstOrNull()
    }

    fun readCrashLog(file: File): String {
        return try {
            file.readText()
        } catch (_: Exception) {
            ""
        }
    }

    fun deleteCrashLog(file: File): Boolean {
        return try {
            if (!file.delete()) return false
            val dir = file.parentFile ?: return true
            val latestFile = File(dir, LATEST_LOG_NAME)
            val remaining = listCrashLogsInDirectory(dir).firstOrNull()
            if (remaining != null) {
                remaining.copyTo(latestFile, overwrite = true)
                true
            } else {
                !latestFile.exists() || latestFile.delete()
            }
        } catch (_: Exception) {
            false
        }
    }

    fun clearAllCrashLogs(context: Context): Boolean {
        val dir = getCrashDirectory(context)
        val files = dir.listFiles() ?: return true
        var allDeleted = true
        for (f in files) {
            if (f.isFile && !f.delete()) {
                allDeleted = false
            }
        }
        return allDeleted
    }

    private fun pruneOldLogs(dir: File) {
        val files = dir.listFiles { file ->
            file.isFile && file.name.startsWith("crash_") && file.name.endsWith(".log")
        } ?: return

        if (files.size > MAX_SAVED_LOGS) {
            val sorted = files.sortedBy { it.lastModified() }
            val countToDelete = files.size - MAX_SAVED_LOGS
            for (i in 0 until countToDelete) {
                sorted[i].delete()
            }
        }
    }
}
