package com.noches.chunkoidng.core.runtime

import android.content.Context
import android.os.Build
import android.util.Log
import com.noches.chunkoidng.maid.Maid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class JavaProcessManager(private val context: Context, private val runtimeEnv: JavaRuntimeEnvironment) {
    private val TAG = "JavaProcessManager"
    private var maxMemoryMB = 2048

    fun setMaxMemory(mb: Int) {
        this.maxMemoryMB = Maid.tuning.budgetGovernor.clampMaxMemory(context, mb)
    }

    fun runCliJar(jarFile: File, vararg arguments: String): Flow<String> {
        return runCliJarAdvanced(jarFile, emptyList(), arguments.toList())
    }

    private fun buildJavaLaunchCandidates(javaBin: File, jvmArgs: List<String>): List<List<String>> {
        return buildLaunchCandidates(
            javaBinPath = javaBin.absolutePath,
            jvmArgs = jvmArgs,
            sdkInt = Build.VERSION.SDK_INT,
            has64BitAbi = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        )
    }

    companion object {
        internal fun buildLaunchCandidates(
            javaBinPath: String,
            jvmArgs: List<String>,
            sdkInt: Int,
            has64BitAbi: Boolean
        ): List<List<String>> {
            val linker = if (has64BitAbi) "/system/bin/linker64" else "/system/bin/linker"
            val directCmd = listOf(javaBinPath) + jvmArgs
            val linkerCmd = listOf(linker, javaBinPath) + jvmArgs
            return if (sdkInt < Build.VERSION_CODES.Q) {
                listOf(directCmd, linkerCmd)
            } else {
                listOf(linkerCmd, directCmd)
            }
        }
    }

    private fun startJavaProcess(
        javaBin: File,
        jvmArgs: List<String>,
        onProcessCreated: ((Process) -> Unit)? = null
    ): Result<Process> {
        var lastError: Exception? = null
        for (cmd in buildJavaLaunchCandidates(javaBin, jvmArgs)) {
            try {
                val pb = ProcessBuilder(cmd)
                setupEnvironment(pb.environment())
                pb.directory(context.filesDir)
                pb.redirectErrorStream(true)
                val process = pb.start()
                onProcessCreated?.invoke(process)
                return Result.success(process)
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Failed to launch JVM with command ${cmd.firstOrNull()}: ${e.message}")
            }
        }
        return Result.failure(lastError ?: IllegalStateException("No viable launch command"))
    }

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

        val startResult = startJavaProcess(javaBin, args, onProcessCreated)
        val process = startResult.getOrNull()
        if (process == null) {
            val err = startResult.exceptionOrNull()
            val supportedAbis = Build.SUPPORTED_ABIS.joinToString(",")
            val abiNote = if (Build.SUPPORTED_ABIS.none { it.equals("arm64-v8a", ignoreCase = true) }) {
                " (Device ABIs: [$supportedAbis]; embedded OpenJDK 17 requires arm64-v8a)"
            } else ""
            emit("[SYSTEM] Critical Error: Failed to start Java process: ${err?.message ?: "unknown"}$abiNote")
            err?.let { emit(it.stackTraceToString()) }
            return@flow
        }

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
    }.flowOn(Dispatchers.IO)

    fun executeShellCommand(rawCommand: String): Flow<String> = flow {
        if (!runtimeEnv.isRootfsReady()) {
            emit("Error: Termux environment not initialized")
            return@flow
        }

        val command = rawCommand.trim()
        val appFilesDir = context.filesDir.absolutePath

        when {
            command.equals("ls", ignoreCase = true) -> {
                val files = context.filesDir.listFiles()
                if (files != null && files.isNotEmpty()) {
                    files.forEach { emit(if (it.isDirectory) "${it.name}/" else it.name) }
                } else {
                    emit("(filesDir is empty)")
                }
            }

            command.equals("pwd", ignoreCase = true) -> {
                emit(appFilesDir)
            }

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

                val initialHeapMb = if (maxMemoryMB <= 768) 64 else 128
                val jvmArgs = mutableListOf(
                    "-Xms${initialHeapMb}m",
                    "-Xmx${maxMemoryMB}m",
                    "-Djava.io.tmpdir=${context.cacheDir.absolutePath}"
                )
                jvmArgs.addAll(rawArgs)

                var process: Process? = null
                try {
                    val startResult = startJavaProcess(javaBin, jvmArgs)
                    process = startResult.getOrThrow()
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
