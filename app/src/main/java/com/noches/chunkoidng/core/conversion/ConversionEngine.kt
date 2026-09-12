package com.noches.chunkoidng.core.conversion

import android.content.Context
import android.util.Log
import com.noches.chunkoidng.core.runtime.JavaProcessManager
import com.noches.chunkoidng.core.runtime.JavaRuntimeEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

sealed class ConversionEvent {
    data class Progress(val percent: Int, val stage: String, val lastLog: String) : ConversionEvent()
    data class LogOutput(val line: String) : ConversionEvent()
    data class Success(val outputDir: File, val durationMs: Long) : ConversionEvent()
    data class Failure(val error: String, val exitCode: Int? = null) : ConversionEvent()
}

/**
 * Executes world conversions by running Chunker with tailored arguments and parsing progress output.
 */
class ConversionEngine(
    private val context: Context,
    private val runtimeEnv: JavaRuntimeEnvironment,
    private val processManager: JavaProcessManager
) {
    private val TAG = "ConversionEngine"
    private var activeProcess: Process? = null

    fun cancel() {
        try {
            activeProcess?.destroyForcibly()
            activeProcess = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling conversion process", e)
        }
    }

    fun execute(config: ConversionConfig): Flow<ConversionEvent> = flow {
        val startTime = System.currentTimeMillis()

        if (!config.inputDir.exists()) {
            emit(ConversionEvent.Failure("输入目录不存在: ${config.inputDir.absolutePath}"))
            return@flow
        }

        // Prepare empty output dir
        if (config.outputDir.exists()) {
            config.outputDir.deleteRecursively()
        }
        config.outputDir.mkdirs()

        runtimeEnv.ensureCliJar()
        val cliJar = File(context.filesDir, "cli.jar")
        if (!cliJar.exists()) {
            emit(ConversionEvent.Failure("核心转换库 cli.jar 缺失"))
            return@flow
        }

        emit(ConversionEvent.Progress(5, "正在初始化转换引擎...", "准备 Java 运行环境"))

        // JVM options
        processManager.setMaxMemory(config.maxMemoryMB)
        val jvmOpts = mutableListOf<String>()
        if (config.lowMemoryMode) {
            jvmOpts.add("-XX:+UseSerialGC")
            jvmOpts.add("-XX:MinHeapFreeRatio=10")
            jvmOpts.add("-XX:MaxHeapFreeRatio=20")
            jvmOpts.add("-Djava.util.concurrent.ForkJoinPool.common.parallelism=1")
        }

        // Chunker CLI arguments
        val cliArgs = mutableListOf(
            "-i", config.inputDir.absolutePath,
            "-o", config.outputDir.absolutePath,
            "-f", config.targetFormat.id
        )

        if (config.keepOriginalNbt) {
            cliArgs.add("-k")
        }

        val pruningJson = config.buildPruningJson()
        if (pruningJson != null) {
            cliArgs.add("-p")
            cliArgs.add(pruningJson)
        }

        val worldSettingsJson = config.buildWorldSettingsJson()
        if (worldSettingsJson != null) {
            cliArgs.add("-s")
            cliArgs.add(worldSettingsJson)
        }

        emit(ConversionEvent.Progress(15, "正在启动 Chunker 进程...", "指令参数: ${cliArgs.joinToString(" ")}"))

        var lastPercent = 15
        var exitCode: Int? = null
        var isSuccess = false

        try {
            processManager.runCliJarAdvanced(
                jarFile = cliJar,
                jvmOptions = jvmOpts,
                cliArguments = cliArgs,
                onProcessCreated = { process ->
                    activeProcess = process
                }
            ).collect { line ->
                // Filter noisy linker warnings
                if (line.contains("WARNING: linker:") || line.contains("has unsupported flags")) {
                    return@collect
                }

                emit(ConversionEvent.LogOutput(line))

                // Parse Chunker percentage regex (e.g. "15.4%")
                val percentMatch = Regex("(\\d+(?:\\.\\d+)?)%").find(line)
                if (percentMatch != null) {
                    val rawNum = percentMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                    // Scale inner 0~100% into 15%~85%
                    val calculated = (15 + (rawNum * 0.70)).toInt().coerceIn(15, 85)
                    if (calculated > lastPercent) {
                        lastPercent = calculated
                        emit(ConversionEvent.Progress(calculated, "正在处理区块与维度...", line.trim()))
                    }
                } else if (line.contains("Writing", ignoreCase = true) || line.contains("Writing world", ignoreCase = true)) {
                    if (lastPercent < 85) lastPercent = 85
                    emit(ConversionEvent.Progress(88, "正在写入目标世界数据...", line.trim()))
                } else if (line.contains("Finished converting", ignoreCase = true) || line.contains("Conversion complete", ignoreCase = true)) {
                    lastPercent = 95
                    emit(ConversionEvent.Progress(95, "正在整理并刷新输出...", line.trim()))
                }

                if (line.startsWith("[SYSTEM] Process exited with code:")) {
                    exitCode = line.substringAfterLast(':').trim().toIntOrNull()
                    if (exitCode == 0) {
                        isSuccess = true
                    }
                }
            }
        } catch (e: Exception) {
            emit(ConversionEvent.Failure("转换异常中止: ${e.message}"))
            return@flow
        } finally {
            activeProcess = null
        }

        val totalTime = System.currentTimeMillis() - startTime
        if (isSuccess || (exitCode == 0 && config.outputDir.exists() && config.outputDir.listFiles()?.isNotEmpty() == true)) {
            emit(ConversionEvent.Progress(100, "转换完成！", "耗时 ${totalTime / 1000} 秒"))
            emit(ConversionEvent.Success(config.outputDir, totalTime))
        } else {
            val errorMsg = if (exitCode != null && exitCode != 0) {
                "Chunker 进程异常退出 (错误码: $exitCode)"
            } else {
                "转换未完成，未检测到输出世界"
            }
            emit(ConversionEvent.Failure(errorMsg, exitCode))
        }
    }.flowOn(Dispatchers.IO)
}
