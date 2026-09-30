package com.noches.chunkoidng.core.conversion

import android.content.Context
import com.noches.chunkoidng.R
import android.util.Log
import com.noches.chunkoidng.core.runtime.JavaProcessManager
import com.noches.chunkoidng.core.runtime.JavaRuntimeEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

sealed class ConversionEvent {
    abstract val taskId: String
    data class Progress(override val taskId: String, val percent: Int, val stage: String, val lastLog: String) : ConversionEvent()
    data class LogOutput(override val taskId: String, val line: String) : ConversionEvent()
    data class Success(override val taskId: String, val outputDir: File, val durationMs: Long) : ConversionEvent()
    data class Failure(override val taskId: String, val error: String, val exitCode: Int? = null) : ConversionEvent()
}

class ConversionEngine(
    private val context: Context,
    private val runtimeEnv: JavaRuntimeEnvironment,
    private val processManager: JavaProcessManager
) {
    private val TAG = "ConversionEngine"
    @Volatile private var activeProcess: Process? = null

    @Synchronized
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
            emit(ConversionEvent.Failure(config.taskId, context.getString(R.string.engine_error_input_not_exist, config.inputDir.absolutePath)))
            return@flow
        }

        val tempOutput = File(config.outputDir.parentFile, "${config.outputDir.name}.tmp.${config.taskId}")
        if (tempOutput.exists()) tempOutput.deleteRecursively()
        tempOutput.mkdirs()

        if (!runtimeEnv.ensureCliJar()) {
            tempOutput.deleteRecursively()
            emit(ConversionEvent.Failure(config.taskId, context.getString(R.string.engine_error_prep_cli)))
            return@flow
        }
        val cliJar = File(context.filesDir, "cli.jar")
        if (!cliJar.exists()) {
            emit(ConversionEvent.Failure(config.taskId, context.getString(R.string.engine_error_cli_missing)))
            return@flow
        }

        emit(ConversionEvent.Progress(config.taskId, 5, context.getString(R.string.engine_status_init), context.getString(R.string.engine_status_prep_jre)))

        processManager.setMaxMemory(config.maxMemoryMB)
        val jvmOpts = mutableListOf<String>()
        if (config.lowMemoryMode) {
            jvmOpts.add("-XX:+UseSerialGC")
            jvmOpts.add("-XX:MinHeapFreeRatio=10")
            jvmOpts.add("-XX:MaxHeapFreeRatio=20")
            jvmOpts.add("-Djava.util.concurrent.ForkJoinPool.common.parallelism=1")
        }

        val cliArgs = mutableListOf(
            "-i", config.inputDir.absolutePath,
            "-o", tempOutput.absolutePath,
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

        emit(ConversionEvent.Progress(config.taskId, 15, context.getString(R.string.engine_status_starting_chunker), context.getString(R.string.engine_status_args, cliArgs.joinToString(" "))))

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

                if (line.contains("WARNING: linker:") || line.contains("has unsupported flags")) {
                    return@collect
                }

                emit(ConversionEvent.LogOutput(config.taskId, line))

                val percentMatch = Regex("(\\d+(?:\\.\\d+)?)%").find(line)
                if (percentMatch != null) {
                    val rawNum = percentMatch.groupValues[1].toDoubleOrNull() ?: 0.0

                    val calculated = (15 + (rawNum * 0.70)).toInt().coerceIn(15, 85)
                    if (calculated > lastPercent) {
                        lastPercent = calculated
                        emit(ConversionEvent.Progress(config.taskId, calculated, context.getString(R.string.engine_status_processing_chunks), line.trim()))
                    }
                } else if (line.contains("Writing", ignoreCase = true) || line.contains("Writing world", ignoreCase = true)) {
                    if (lastPercent < 85) lastPercent = 85
                    emit(ConversionEvent.Progress(config.taskId, 88, context.getString(R.string.engine_status_writing_target), line.trim()))
                } else if (line.contains("Finished converting", ignoreCase = true) || line.contains("Conversion complete", ignoreCase = true)) {
                    lastPercent = 95
                    emit(ConversionEvent.Progress(config.taskId, 95, context.getString(R.string.engine_status_finishing_output), line.trim()))
                }

                if (line.startsWith("[SYSTEM] Process exited with code:")) {
                    exitCode = line.substringAfterLast(':').trim().toIntOrNull()
                    if (exitCode == 0) {
                        isSuccess = true
                    }
                }
            }
        } catch (e: Exception) {
            tempOutput.deleteRecursively()
            emit(ConversionEvent.Failure(config.taskId, context.getString(R.string.engine_error_aborted, e.message ?: "")))
            return@flow
        } finally {
            activeProcess = null
        }

        val totalTime = System.currentTimeMillis() - startTime
        val outputValid = tempOutput.exists() && tempOutput.listFiles()?.isNotEmpty() == true &&
                (File(tempOutput, "level.dat").isFile || File(tempOutput, "db").isDirectory)
        if (isSuccess && exitCode == 0 && outputValid) {
            if (config.outputDir.exists()) config.outputDir.deleteRecursively()
            check(tempOutput.renameTo(config.outputDir)) { context.getString(R.string.engine_error_commit_output) }
            emit(ConversionEvent.Progress(config.taskId, 100, context.getString(R.string.engine_status_completed), context.getString(R.string.engine_status_time_elapsed, totalTime / 1000)))
            emit(ConversionEvent.Success(config.taskId, config.outputDir, totalTime))
        } else {
            tempOutput.deleteRecursively()
            val errorMsg = if (exitCode != null && exitCode != 0) {
                context.getString(R.string.engine_error_exit_code, exitCode)
            } else {
                context.getString(R.string.engine_error_no_output)
            }
            emit(ConversionEvent.Failure(config.taskId, errorMsg, exitCode))
        }
    }.flowOn(Dispatchers.IO)
}
