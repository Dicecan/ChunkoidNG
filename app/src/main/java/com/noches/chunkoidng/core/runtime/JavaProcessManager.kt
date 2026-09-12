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
        this.maxMemoryMB = mb
    }

    /**
     * Executes the CLI jar with arguments and streams the output log in real-time.
     */
    fun runCliJar(jarFile: File, vararg arguments: String): Flow<String> = flow {
        if (!runtimeEnv.isRootfsReady()) {
            emit("[SYSTEM] Error: RootFS environment is not ready.")
            return@flow
        }

        val javaBin = runtimeEnv.findJavaBinary()
        if (javaBin == null || !javaBin.exists()) {
            emit("[SYSTEM] Error: Java binary not found.")
            return@flow
        }

        val args = mutableListOf(
            "-Xms256m",
            "-Xmx${maxMemoryMB}m",
            "-Djava.io.tmpdir=${context.cacheDir.absolutePath}", // Fix for modern Android
            "-jar",
            jarFile.absolutePath
        )
        args.addAll(arguments)

        val cmd = mutableListOf(javaBin.absolutePath)
        cmd.addAll(args)

        var pb = ProcessBuilder(cmd)
        setupEnvironment(pb.environment())
        pb.directory(context.filesDir)
        pb.redirectErrorStream(true)

        var process: Process? = null
        try {
            Log.d(TAG, "Starting Java process directly...")
            process = pb.start()
        } catch (e: Exception) {
            if (e.message?.contains("Permission denied") == true) {
                emit("[SYSTEM] Direct execution denied. Attempting fallback via linker...")
                Log.d(TAG, "Permission denied, trying with linker")
                val linker = if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) "/system/bin/linker64" else "/system/bin/linker"
                val fallbackCmd = mutableListOf(linker, javaBin.absolutePath)
                fallbackCmd.addAll(args)
                pb = ProcessBuilder(fallbackCmd)
                setupEnvironment(pb.environment())
                pb.directory(context.filesDir)
                pb.redirectErrorStream(true)
                try {
                    process = pb.start()
                } catch (ex: Exception) {
                    emit("[SYSTEM] Critical Error: Failed to start process even with linker.")
                    emit(ex.stackTraceToString())
                    return@flow
                }
            } else {
                emit("[SYSTEM] Critical Error: Failed to start process.")
                emit(e.stackTraceToString())
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
                process.destroy()
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
                val jvmArgs = mutableListOf(
                    "-Xms256m",
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
                    process?.destroy()
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
                    process?.destroy()
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
