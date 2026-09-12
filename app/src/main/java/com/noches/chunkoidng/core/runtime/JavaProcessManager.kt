package com.noches.chunkoidng.core.runtime

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Manages the execution of Java processes in the sandbox.
 */
class JavaProcessManager(private val context: Context, private val runtimeEnv: JavaRuntimeEnvironment) {
    private val TAG = "JavaProcessManager"
    private var maxMemoryMB = 2048

    fun setMaxMemory(mb: Int) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        val deviceLimitMb = if (activityManager != null) {
            activityManager.getMemoryInfo(memInfo)
            val totalPhysicalRamMb = (memInfo.totalMem / (1024 * 1024)).toInt()
            // Leave room for Android OS, Chunkoid foreground app and system services.
            val reservedRamMb = (totalPhysicalRamMb * 0.25f).toInt().coerceAtLeast(1024)
            (totalPhysicalRamMb - reservedRamMb).coerceAtLeast(512)
        } else {
            mb
        }
        this.maxMemoryMB = mb.coerceIn(256, deviceLimitMb)
    }

    /**
     * Executes the CLI jar with arguments and streams the output log in real-time.
     */
    fun runCliJar(jarFile: File, vararg arguments: String): Flow<String> {
        return runCliJarAdvanced(jarFile, emptyList(), arguments.toList())
    }

    /**
     * Executes the CLI jar with custom JVM options and CLI arguments.
     * Supports cooperative coroutine cancellation and process handles.
     */
    fun runCliJarAdvanced(
        jarFile: File,
        jvmOptions: List<String> = emptyList(),
        cliArguments: List<String> = emptyList(),
        onProcessCreated: ((Process) -> Unit)? = null
    ): Flow<String> = flow {
        if (!runtimeEnv.isRootfsReady()) {
            emit("[SYSTEM] Error: RootFS environment is not ready.")
            return@flow
        }

        val javaBin = runtimeEnv.findJavaBinary()
        if (javaBin == null || !javaBin.exists()) {
            emit("[SYSTEM] Error: Java binary not found.")
            return@flow
        }

        val initialHeapMb = if (maxMemoryMB <= 768) 64 else 128
        val args = mutableListOf(
            "-Xms${initialHeapMb}m",
            "-Xmx${maxMemoryMB}m",
            "-Djava.io.tmpdir=${context.cacheDir.absolutePath}"
        )
        args.addAll(jvmOptions)
        args.add("-jar")
        args.add(jarFile.absolutePath)
        args.addAll(cliArguments)

        val linker = if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) "/system/bin/linker64" else "/system/bin/linker"
        val cmd = mutableListOf(linker, javaBin.absolutePath)
        cmd.addAll(args)

        val pb = ProcessBuilder(cmd)
        setupEnvironment(pb.environment())
        pb.directory(context.filesDir)
        pb.redirectErrorStream(true)

        var process: Process? = null
        try {
            process = pb.start()
            onProcessCreated?.invoke(process)
        } catch (e: Exception) {
            // If linker call fails directly, try direct binary execution as fallback
            try {
                val directCmd = mutableListOf(javaBin.absolutePath)
                directCmd.addAll(args)
                val directPb = ProcessBuilder(directCmd)
                setupEnvironment(directPb.environment())
                directPb.directory(context.filesDir)
                directPb.redirectErrorStream(true)
                process = directPb.start()
                onProcessCreated?.invoke(process)
            } catch (ex: Exception) {
                emit("[SYSTEM] Critical Error: Failed to start Java process: ${e.message}")
                emit(ex.stackTraceToString())
                return@flow
            }
        }

        if (process != null) {
            try {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        emit(line ?: "")
                    }
                }
                val exitCode = process.waitFor()
                emit("[SYSTEM] Process exited with code: $exitCode")
                } finally {
                    if (process.isAlive) process.destroyForcibly()
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Executes commands in the sandbox terminal.
     * Ported faithfully from old Chunkoid's TerminalActivity.kt & JavaCommandExecutor.java.
     */
    fun executeShellCommand(rawCommand: String): Flow<String> = flow {
        if (!runtimeEnv.isRootfsReady()) {
            emit("Error: Termux environment not initialized")
            return@flow
        }

        val command = rawCommand.trim()
        val appFilesDir = context.filesDir.absolutePath

        when {
            // Ported from old Chunkoid executeLs()
            command.equals("ls", ignoreCase = true) -> {
                val files = context.filesDir.listFiles()
                if (files != null && files.isNotEmpty()) {
                    files.forEach { emit(if (it.isDirectory) "${it.name}/" else it.name) }
                } else {
                    emit("(filesDir is empty)")
                }
            }

            // Ported from old Chunkoid executePwd()
            command.equals("pwd", ignoreCase = true) -> {
                emit(appFilesDir)
            }

            // Ported from old Chunkoid executeJavaCommand()
            command.startsWith("java") -> {
                runtimeEnv.ensureCliJar()
                val cliJarPath = File(context.filesDir, "cli.jar").absolutePath
                var modifiedCommand = command
                if (modifiedCommand.contains("cli.jar")) {
                    modifiedCommand = modifiedCommand.replace("cli.jar", cliJarPath)
                }

                val rawArgs = modifiedCommand.removePrefix("java").trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
                val javaBin = runtimeEnv.findJavaBinary()
                if (javaBin == null || !javaBin.exists()) {
                    emit("Error: Java binary not found in rootfs")
                    return@flow
                }

                val linker = if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) "/system/bin/linker64" else "/system/bin/linker"
                val initialHeapMb = if (maxMemoryMB <= 768) 64 else 128
                val jvmArgs = mutableListOf(
                    "-Xms${initialHeapMb}m",
                    "-Xmx${maxMemoryMB}m",
                    "-Djava.io.tmpdir=${context.cacheDir.absolutePath}"
                )
                jvmArgs.addAll(rawArgs)

                // Execute with linker directly (same as old Chunkoid executeJavaWithLinker)
                val directCmd = mutableListOf(linker, javaBin.absolutePath)
                directCmd.addAll(jvmArgs)

                val pb = ProcessBuilder(directCmd)
                setupEnvironment(pb.environment())
                pb.directory(context.filesDir)
                pb.redirectErrorStream(true)

                var process: Process? = null
                try {
                    process = pb.start()
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            emit(line ?: "")
                        }
                    }
                    val exitCode = process.waitFor()
                    if (exitCode != 0) {
                        emit("[Process exited with code $exitCode]")
                    }
                } catch (e: Exception) {
                    emit("Error executing java: ${e.message}")
                } finally {
                    process?.let { if (it.isAlive) it.destroyForcibly() }
                }
            }

            // Fallback to native shell command
            else -> {
                val pb = ProcessBuilder("/system/bin/sh", "-c", command)
                setupEnvironment(pb.environment())
                pb.directory(context.filesDir)
                pb.redirectErrorStream(true)

                var process: Process? = null
                try {
                    process = pb.start()
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            emit(line ?: "")
                        }
                    }
                    val exitCode = process.waitFor()
                    if (exitCode != 0) {
                        emit("[Process exited with code $exitCode]")
                    }
                } catch (e: Exception) {
                    emit("Error executing command: ${e.message}")
                } finally {
                    process?.let { if (it.isAlive) it.destroyForcibly() }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun setupEnvironment(env: MutableMap<String, String>) {
        val rootfsDir = runtimeEnv.rootfsDir.absolutePath
        env["JAVA_HOME"] = runtimeEnv.getJavaHome()
        env["PROOT_TMP_DIR"] = context.cacheDir.absolutePath
        env["TMPDIR"] = context.cacheDir.absolutePath
        
        env["PATH"] = "$rootfsDir/bin:$rootfsDir/sbin:$rootfsDir/usr/bin:$rootfsDir/usr/sbin:/system/bin:/system/sbin:/vendor/bin"
        
        env["LD_LIBRARY_PATH"] = listOf(
            "$rootfsDir/lib",
            "$rootfsDir/lib/aarch64-linux-gnu",
            "$rootfsDir/usr/lib",
            "$rootfsDir/usr/lib/aarch64-linux-gnu",
            "${runtimeEnv.getJavaHome()}/lib",
            "/system/lib64",
            "/system/lib",
            "/vendor/lib64",
            "/vendor/lib"
        ).joinToString(":")
        
        env.remove("LD_PRELOAD")
        env["ANDROID_ROOT"] = "/system"
        env["ANDROID_DATA"] = "/data"
    }
}
