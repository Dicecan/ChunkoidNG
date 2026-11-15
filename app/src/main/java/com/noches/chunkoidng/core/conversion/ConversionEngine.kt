package com.noches.chunkoidng.core.conversion

import android.content.Context
import com.noches.chunkoidng.R
import android.util.Log
import com.noches.chunkoidng.core.entity.BedrockActorBuilder
import com.noches.chunkoidng.core.entity.BedrockToJavaEntityPipeline
import com.noches.chunkoidng.core.entity.BedrockToBedrockEntityPipeline
import com.noches.chunkoidng.core.entity.EntityConversionPipeline
import com.noches.chunkoidng.core.entity.JavaToJavaEntityPipeline
import com.noches.chunkoidng.core.runtime.JavaProcessManager
import com.noches.chunkoidng.core.runtime.JavaRuntimeEnvironment
import com.noches.chunkoidng.core.settings.LocaleHelper
import com.noches.chunkoidng.core.world.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
        val l10n = LocaleHelper.localizedContext(context)

        if (!config.inputDir.exists()) {
            emit(ConversionEvent.Failure(config.taskId, l10n.getString(R.string.engine_error_input_not_exist, config.inputDir.absolutePath)))
            return@flow
        }

        val tempOutput = File(config.outputDir.parentFile, "${config.outputDir.name}.tmp.${config.taskId}")
        if (tempOutput.exists()) tempOutput.deleteRecursively()
        tempOutput.mkdirs()

        if (!runtimeEnv.ensureCliJar()) {
            tempOutput.deleteRecursively()
            emit(ConversionEvent.Failure(config.taskId, l10n.getString(R.string.engine_error_prep_cli)))
            return@flow
        }
        val cliJar = File(context.filesDir, "cli.jar")
        if (!cliJar.exists()) {
            tempOutput.deleteRecursively()
            emit(ConversionEvent.Failure(config.taskId, l10n.getString(R.string.engine_error_cli_missing)))
            return@flow
        }

        if (!runtimeEnv.isRootfsReady()) {
            emit(ConversionEvent.Progress(config.taskId, 2, l10n.getString(R.string.engine_status_deploying_rootfs), ""))
            try {
                runtimeEnv.extractRootFS().collect { pct ->
                    val scaled = (2 + (pct * 0.03f).toInt()).coerceIn(2, 5)
                    emit(ConversionEvent.Progress(config.taskId, scaled, l10n.getString(R.string.engine_status_deploying_rootfs), "$pct%"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Auto RootFS extraction failed", e)
            }
            if (!runtimeEnv.isRootfsReady()) {
                tempOutput.deleteRecursively()
                emit(ConversionEvent.Failure(config.taskId, l10n.getString(R.string.engine_error_rootfs_not_ready)))
                return@flow
            }
        }

        emit(ConversionEvent.Progress(config.taskId, 5, l10n.getString(R.string.engine_status_init), l10n.getString(R.string.engine_status_prep_jre)))

        processManager.setMaxMemory(config.maxMemoryMB)
        val jvmOpts = mutableListOf<String>()
        if (config.lowMemoryMode) {
            jvmOpts.add("-XX:+UseSerialGC")
            jvmOpts.add("-XX:MinHeapFreeRatio=10")
            jvmOpts.add("-XX:MaxHeapFreeRatio=20")
            jvmOpts.add("-Djava.util.concurrent.ForkJoinPool.common.parallelism=1")
        } else {
            if (config.useSerialGc == true) {
                jvmOpts.add("-XX:+UseSerialGC")
            }
            val threads = (config.parallelism ?: 2).coerceAtLeast(1)
            jvmOpts.add("-Djava.util.concurrent.ForkJoinPool.common.parallelism=$threads")
        }
        jvmOpts.addAll(config.customJvmOptions)

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

        val isInputJava = File(config.inputDir, "region").isDirectory ||
                File(config.inputDir, "entities").isDirectory ||
                File(config.inputDir, "dimensions").isDirectory
        val isInputBedrock = File(config.inputDir, "db").isDirectory
        val isInputTarget = config.targetFormat.id == "INPUT"
        val isTargetBedrock = if (isInputTarget) {
            isInputBedrock && !isInputJava
        } else {
            config.targetFormat.platform == Platform.BEDROCK
        }
        val isTargetJava = if (isInputTarget) {
            isInputJava
        } else {
            config.targetFormat.platform == Platform.JAVA
        }
        val shouldConvertJe2BeEntities = config.convertEntities && isTargetBedrock && isInputJava
        val shouldConvertBe2JeEntities = config.convertEntities && isTargetJava && isInputBedrock

        val shouldConvertJe2JeEntities = config.convertEntities && isTargetJava && isInputJava
        val shouldConvertBe2BeEntities = config.convertEntities && isTargetBedrock && isInputBedrock

        val entityPipeline = if (shouldConvertJe2BeEntities) {
            EntityConversionPipeline { _ -> }
        } else null

        val be2JePipeline = if (shouldConvertBe2JeEntities) {
            BedrockToJavaEntityPipeline { _ -> }
        } else null

        val je2JePipeline = if (shouldConvertJe2JeEntities) {
            JavaToJavaEntityPipeline { _ -> }
        } else null

        val be2BePipeline = if (shouldConvertBe2BeEntities) {
            BedrockToBedrockEntityPipeline { _ -> }
        } else null

        val enabledDimensions = mutableListOf<String>()
        if (config.includeOverworld) enabledDimensions.add("overworld")
        if (config.includeNether) enabledDimensions.add("the_nether")
        if (config.includeTheEnd) enabledDimensions.add("the_end")

        val entityExtractDeferred = if (entityPipeline != null) {
            CoroutineScope(Dispatchers.IO).async {
                try {
                    entityPipeline.preExtract(config.inputDir, enabledDimensions)
                } catch (e: Exception) {
                    Log.e(TAG, "Entity pre-extraction failed", e)
                }
            }
        } else null

        if (be2JePipeline != null) {
            try {
                be2JePipeline.scan(config.inputDir, enabledDimensions, config.targetFormat.id)
            } catch (e: Exception) {
                Log.e(TAG, "Bedrock entity scanning failed", e)
            }
        }

        if (je2JePipeline != null) {
            try {
                je2JePipeline.scan(config.inputDir, enabledDimensions, config.targetFormat.id)
            } catch (e: Exception) {
                Log.e(TAG, "Java to Java entity scanning failed", e)
            }
        }

        if (be2BePipeline != null) {
            try {
                be2BePipeline.scan(config.inputDir, enabledDimensions)
            } catch (e: Exception) {
                Log.e(TAG, "Bedrock to Bedrock entity scanning failed", e)
            }
        }

        emit(ConversionEvent.Progress(config.taskId, 15, l10n.getString(R.string.engine_status_starting_chunker), l10n.getString(R.string.engine_status_args, cliArgs.joinToString(" "))))

        var lastPercent = 15
        var exitCode: Int? = null
        var isSuccess = false
        var lastSystemError: String? = null
        var lastDiagnosticLine: String? = null

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

                val trimmedLine = line.trim()
                when {
                    trimmedLine.startsWith("[SYSTEM] Critical Error:") -> {
                        lastSystemError = trimmedLine.removePrefix("[SYSTEM] Critical Error:").trim()
                    }
                    trimmedLine.startsWith("[SYSTEM] Error:") -> {
                        lastSystemError = trimmedLine.removePrefix("[SYSTEM] Error:").trim()
                    }
                    trimmedLine.contains("Failed to find suitable reader", ignoreCase = true) ||
                        trimmedLine.contains("Failed to find suitable writer", ignoreCase = true) ||
                        trimmedLine.contains("Original NBT is not available", ignoreCase = true) ||
                        trimmedLine.contains("Failed with exception", ignoreCase = true) ||
                        trimmedLine.contains("Error occurred during initialization of VM", ignoreCase = true) ||
                        trimmedLine.contains("Could not reserve enough space", ignoreCase = true) ||
                        trimmedLine.contains("OutOfMemoryError", ignoreCase = true) ||
                        trimmedLine.contains("No space left on device", ignoreCase = true) ||
                        trimmedLine.contains("CANNOT LINK EXECUTABLE", ignoreCase = true) ||
                        trimmedLine.contains("Exec format error", ignoreCase = true) ||
                        trimmedLine.contains("is for EM_", ignoreCase = true) ||
                        trimmedLine.contains("Exception in thread", ignoreCase = true) ||
                        trimmedLine.contains("UnsatisfiedLinkError", ignoreCase = true) -> {
                        lastDiagnosticLine = trimmedLine
                    }
                }

                val percentMatch = Regex("(\\d+(?:\\.\\d+)?)%").find(line)
                if (percentMatch != null) {
                    val rawNum = percentMatch.groupValues[1].toDoubleOrNull() ?: 0.0

                    val calculated = (15 + (rawNum * 0.70)).toInt().coerceIn(15, 85)
                    if (calculated > lastPercent) {
                        lastPercent = calculated
                        emit(ConversionEvent.Progress(config.taskId, calculated, l10n.getString(R.string.engine_status_processing_chunks), trimmedLine))
                    }
                } else if (line.contains("Writing", ignoreCase = true) || line.contains("Writing world", ignoreCase = true)) {
                    if (lastPercent < 85) lastPercent = 85
                    emit(ConversionEvent.Progress(config.taskId, 88, l10n.getString(R.string.engine_status_writing_target), trimmedLine))
                } else if (line.contains("Finished converting", ignoreCase = true) || line.contains("Conversion complete", ignoreCase = true)) {
                    lastPercent = 92
                    emit(ConversionEvent.Progress(config.taskId, 92, l10n.getString(R.string.engine_status_finishing_output), trimmedLine))
                }

                if (line.startsWith("[SYSTEM] Process exited with code:")) {
                    exitCode = line.substringAfterLast(':').trim().toIntOrNull()
                    if (exitCode == 0) {
                        isSuccess = true
                    }
                }
            }
        } catch (e: Exception) {
            entityExtractDeferred?.cancel()
            tempOutput.deleteRecursively()
            emit(ConversionEvent.Failure(config.taskId, l10n.getString(R.string.engine_error_aborted, e.message ?: "")))
            return@flow
        } finally {
            activeProcess = null
            if (!isSuccess || exitCode != 0) {
                entityExtractDeferred?.cancel()
            }
        }

        if (isSuccess && exitCode == 0 && entityPipeline != null) {
            try {
                emit(ConversionEvent.Progress(config.taskId, 94, l10n.getString(R.string.engine_status_entity_waiting), ""))
                entityExtractDeferred?.await()

                emit(ConversionEvent.Progress(config.taskId, 96, l10n.getString(R.string.engine_status_entity_writing_be), ""))
                val stats = entityPipeline.flushToLevelDb(tempOutput)
                emit(ConversionEvent.LogOutput(config.taskId, l10n.getString(R.string.engine_log_entity_be_success, stats.totalActors, stats.skippedModded)))
            } catch (e: Exception) {
                Log.e(TAG, "Error flushing entities to LevelDB", e)
                emit(ConversionEvent.LogOutput(config.taskId, l10n.getString(R.string.engine_log_entity_warn, e.message ?: "")))
            }
        }

        if (isSuccess && exitCode == 0 && be2JePipeline != null) {
            try {
                emit(ConversionEvent.Progress(config.taskId, 96, l10n.getString(R.string.engine_status_entity_writing_je), ""))
                val stats = be2JePipeline.flushToJava(tempOutput, config.targetFormat.id, config.inputDir)
                emit(ConversionEvent.LogOutput(config.taskId, l10n.getString(R.string.engine_log_entity_je_success, stats.totalActors, stats.chunksWritten, stats.skippedModded)))
            } catch (e: Exception) {
                Log.e(TAG, "Error flushing entities to Java MCA", e)
                emit(ConversionEvent.LogOutput(config.taskId, l10n.getString(R.string.engine_log_entity_warn, e.message ?: "")))
            }
        }

        if (isSuccess && exitCode == 0 && je2JePipeline != null) {
            try {
                emit(ConversionEvent.Progress(config.taskId, 96, l10n.getString(R.string.engine_status_entity_writing_je), ""))
                val stats = je2JePipeline.flushToJava(tempOutput, config.targetFormat.id, config.inputDir)
                emit(ConversionEvent.LogOutput(config.taskId, l10n.getString(R.string.engine_log_entity_je_success, stats.totalActors, stats.chunksWritten, stats.skippedModded)))
            } catch (e: Exception) {
                Log.e(TAG, "Error flushing entities to Java MCA", e)
                emit(ConversionEvent.LogOutput(config.taskId, l10n.getString(R.string.engine_log_entity_warn, e.message ?: "")))
            }
        }

        if (isSuccess && exitCode == 0 && be2BePipeline != null) {
            try {
                emit(ConversionEvent.Progress(config.taskId, 96, l10n.getString(R.string.engine_status_entity_writing_be), ""))
                val stats = be2BePipeline.flushToLevelDb(tempOutput)
                emit(ConversionEvent.LogOutput(config.taskId, l10n.getString(R.string.engine_log_entity_be_success, stats.totalActors, stats.skippedModded)))
            } catch (e: Exception) {
                Log.e(TAG, "Error flushing entities to LevelDB", e)
                emit(ConversionEvent.LogOutput(config.taskId, l10n.getString(R.string.engine_log_entity_warn, e.message ?: "")))
            }
        }

        if (isSuccess && exitCode == 0) {
            try {
                emit(ConversionEvent.Progress(config.taskId, 98, l10n.getString(R.string.engine_status_gamerule_syncing), ""))
                val synced = GameRuleSync.sync(config.inputDir, tempOutput, config.targetFormat)
                if (synced > 0) {
                    emit(ConversionEvent.LogOutput(config.taskId, l10n.getString(R.string.engine_log_gamerule_synced, synced)))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing gamerules", e)
                emit(ConversionEvent.LogOutput(config.taskId, l10n.getString(R.string.engine_log_gamerule_warn, e.message ?: "")))
            }
        }

        val totalTime = System.currentTimeMillis() - startTime
        val outputValid = tempOutput.exists() && tempOutput.listFiles()?.isNotEmpty() == true &&
                (File(tempOutput, "level.dat").isFile || File(tempOutput, "db").isDirectory)
        if (isSuccess && exitCode == 0 && outputValid) {
            if (config.outputDir.exists()) config.outputDir.deleteRecursively()
            val committed = tempOutput.renameTo(config.outputDir) || (
                tempOutput.copyRecursively(config.outputDir, overwrite = true).also {
                    tempOutput.deleteRecursively()
                }
            )
            check(committed) { l10n.getString(R.string.engine_error_commit_output) }
            emit(ConversionEvent.Progress(config.taskId, 100, l10n.getString(R.string.engine_status_completed), l10n.getString(R.string.engine_status_time_elapsed, totalTime / 1000)))
            emit(ConversionEvent.Success(config.taskId, config.outputDir, totalTime))
        } else {
            entityExtractDeferred?.cancel()
            tempOutput.deleteRecursively()
            val detail = lastSystemError ?: lastDiagnosticLine
            val errorMsg = when {
                exitCode != null && exitCode != 0 && !detail.isNullOrBlank() ->
                    l10n.getString(R.string.engine_error_exit_code_detail, exitCode, detail)
                exitCode != null && exitCode != 0 ->
                    l10n.getString(R.string.engine_error_exit_code, exitCode)
                !detail.isNullOrBlank() ->
                    l10n.getString(R.string.engine_error_no_output_detail, detail)
                else ->
                    l10n.getString(R.string.engine_error_no_output)
            }
            emit(ConversionEvent.Failure(config.taskId, errorMsg, exitCode))
        }
    }.flowOn(Dispatchers.IO)
}
