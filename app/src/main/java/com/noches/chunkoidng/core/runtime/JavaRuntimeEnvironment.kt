package com.noches.chunkoidng.core.runtime

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Manages the installation and initialization of the OpenJDK 17 rootfs environment.
 */
class JavaRuntimeEnvironment(private val context: Context) {
    private val TAG = "JavaRuntimeEnvironment"
    private val ROOTFS_ZIP = "rootfs.zip"

    val rootfsDir: File
        get() = File(context.filesDir.parentFile, "files/rootfs")

    /**
     * Extracts the rootfs from assets. Emits progress (0 to 100).
     */
    fun extractRootFS(): Flow<Int> = flow {
        emit(0)
        Log.d(TAG, "Starting RootFS extraction...")

        if (isRootfsReady()) {
            Log.d(TAG, "RootFS is already extracted and valid.")
            emit(100)
            return@flow
        }

        // Clean up any broken state
        if (rootfsDir.exists()) {
            rootfsDir.deleteRecursively()
        }
        rootfsDir.mkdirs()

        try {
            val assetManager = context.assets
            assetManager.open(ROOTFS_ZIP).use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    val buffer = ByteArray(65536)
                    var extractedEntries = 0
                    val estimatedTotal = 57 // minimal zip has around 57 files
                    var lastReportedProgress = 0

                    while (true) {
                        val entry = zis.nextEntry ?: break
                        val destFile = File(rootfsDir, entry.name)
                        
                        // Security check for zip slip
                        val destDirPath = rootfsDir.canonicalPath
                        val destFilePath = destFile.canonicalPath
                        if (!destFilePath.startsWith(destDirPath + File.separator)) {
                            continue
                        }

                        if (entry.isDirectory) {
                            destFile.mkdirs()
                        } else {
                            destFile.parentFile?.mkdirs()
                            FileOutputStream(destFile).use { fos ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                            
                            // Set permissions for executables
                            val name = entry.name.lowercase()
                            if (name.contains("bin/") || name.contains("lib/") || 
                                name.endsWith(".so") || name.endsWith(".elf") || name.endsWith(".sh")) {
                                destFile.setExecutable(true, false)
                                destFile.setReadable(true, false)
                            }
                        }
                        zis.closeEntry()
                        extractedEntries++

                        val progress = ((extractedEntries.toFloat() / estimatedTotal) * 100).toInt().coerceIn(1, 99)
                        if (progress > lastReportedProgress) {
                            lastReportedProgress = progress
                            emit(progress)
                        }
                    }
                }
            }
            ensureCliJar()
            emit(100)
            Log.d(TAG, "Extraction completed successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed", e)
            rootfsDir.deleteRecursively()
            throw e
        }
    }.flowOn(Dispatchers.IO)

    fun isRootfsReady(): Boolean {
        if (!rootfsDir.exists()) return false
        val javaBin = findJavaBinary()
        val cliJar = File(context.filesDir, "cli.jar")
        return javaBin != null && javaBin.exists() && javaBin.length() > 0 && javaBin.canExecute() && cliJar.exists() && cliJar.length() > 1024 * 1024
    }

    fun findJavaBinary(): File? {
        val paths = listOf(
            "bin/java",
            "usr/lib/jvm/java-17-openjdk/bin/java",
            "usr/lib/jvm/openjdk-17/bin/java",
            "usr/bin/java"
        )
        for (path in paths) {
            val file = File(rootfsDir, path)
            if (file.exists() && file.length() > 0) {
                return file
            }
        }
        return null
    }

    fun getJavaHome(): String {
        val paths = listOf(
            "lib/jvm/java-17-openjdk",
            "usr/lib/jvm/java-17-openjdk",
            "usr/lib/jvm/openjdk-17"
        )
        for (path in paths) {
            val file = File(rootfsDir, path)
            if (file.exists()) {
                return file.absolutePath
            }
        }
        return rootfsDir.absolutePath
    }

    /**
     * Ensures cli.jar is extracted from assets to filesDir.
     * Ported directly from old Chunkoid SplashActivity.
     */
    fun ensureCliJar(): Boolean {
        val targetFile = File(context.filesDir, "cli.jar")
        if (targetFile.exists() && targetFile.length() > 1024 * 1024) {
            return true
        }

        val tempFile = File(context.filesDir, "cli.jar.tmp")
        return try {
            if (tempFile.exists()) tempFile.delete()
            context.assets.open("cli.jar").use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(65536)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }
            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy cli.jar from assets", e)
            false
        }
    }
}
