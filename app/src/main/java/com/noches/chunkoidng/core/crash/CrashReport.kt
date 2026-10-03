package com.noches.chunkoidng.core.crash

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CrashReport(
    val timestamp: Long = System.currentTimeMillis(),
    val appVersionName: String,
    val appVersionCode: Long,
    val packageName: String,
    val threadName: String,
    val isMainThread: Boolean,
    val exceptionClass: String,
    val exceptionMessage: String?,
    val stackTrace: String,
    val deviceManufacturer: String = Build.MANUFACTURER ?: "Unknown",
    val deviceModel: String = Build.MODEL ?: "Unknown",
    val deviceBrand: String = Build.BRAND ?: "Unknown",
    val deviceProduct: String = Build.PRODUCT ?: "Unknown",
    val deviceHardware: String = Build.HARDWARE ?: "Unknown",
    val androidVersion: String = Build.VERSION.RELEASE ?: "Unknown",
    val sdkInt: Int = Build.VERSION.SDK_INT,
    val supportedAbis: List<String> = Build.SUPPORTED_ABIS?.toList() ?: emptyList(),
    val fingerprint: String = Build.FINGERPRINT ?: "Unknown",
    val totalMemoryMb: Long = Runtime.getRuntime().totalMemory() / (1024 * 1024),
    val freeMemoryMb: Long = Runtime.getRuntime().freeMemory() / (1024 * 1024),
    val maxMemoryMb: Long = Runtime.getRuntime().maxMemory() / (1024 * 1024),
    val availableStorageMb: Long = getAvailableStorageMb()
) {
    fun formatFormattedText(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(timestamp))
        return buildString {
            appendLine("==================== CHUNKOID CRASH REPORT ====================")
            appendLine("Timestamp: $formattedDate ($timestamp)")
            appendLine()
            appendLine("[App Info]")
            appendLine("Package: $packageName")
            appendLine("Version: $appVersionName ($appVersionCode)")
            appendLine()
            appendLine("[Device Info]")
            appendLine("Manufacturer: $deviceManufacturer")
            appendLine("Model: $deviceModel")
            appendLine("Brand: $deviceBrand")
            appendLine("Product: $deviceProduct")
            appendLine("Hardware: $deviceHardware")
            appendLine("Android: $androidVersion (API $sdkInt)")
            appendLine("ABIs: ${supportedAbis.joinToString(", ")}")
            appendLine("Fingerprint: $fingerprint")
            appendLine()
            appendLine("[System Resources]")
            appendLine("Heap: total=${totalMemoryMb}MB, free=${freeMemoryMb}MB, max=${maxMemoryMb}MB")
            appendLine("Internal Storage Free: ${availableStorageMb}MB")
            appendLine()
            appendLine("[Thread & Exception]")
            appendLine("Thread: $threadName (isMainThread=$isMainThread)")
            appendLine("Exception: $exceptionClass")
            appendLine("Message: ${exceptionMessage ?: "N/A"}")
            appendLine()
            appendLine("[Stack Trace]")
            appendLine(stackTrace.trim())
            appendLine("================================================================")
        }
    }

    fun formatSummary(): String {
        return "$exceptionClass: ${exceptionMessage ?: "No message"}"
    }

    companion object {
        fun buildFromThrowable(context: Context, thread: Thread, throwable: Throwable): CrashReport {
            val pInfo = try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (_: Exception) {
                null
            }
            val versionName = pInfo?.versionName ?: "CANARY 0.4"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo?.longVersionCode ?: 4L
            } else {
                @Suppress("DEPRECATION")
                pInfo?.versionCode?.toLong() ?: 4L
            }

            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            pw.flush()

            return CrashReport(
                appVersionName = versionName,
                appVersionCode = versionCode,
                packageName = context.packageName,
                threadName = thread.name,
                isMainThread = thread == android.os.Looper.getMainLooper().thread,
                exceptionClass = throwable.javaClass.name,
                exceptionMessage = throwable.localizedMessage ?: throwable.message,
                stackTrace = sw.toString()
            )
        }

        private fun getAvailableStorageMb(): Long {
            return try {
                val dataDir = Environment.getDataDirectory() ?: return -1L
                val stat = StatFs(dataDir.path)
                (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
            } catch (_: Throwable) {
                -1L
            }
        }
    }
}
