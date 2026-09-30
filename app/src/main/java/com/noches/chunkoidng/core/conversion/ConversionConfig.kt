package com.noches.chunkoidng.core.conversion

import java.io.File
import java.util.UUID

enum class PruningProfile(val displayName: String, val subtitle: String) {
    FULL("全维度保留", "保留主世界、下界与末地所有数据"),
    OVERWORLD_ONLY("仅保留主世界", "剔除下界与末地，缩减 40%~60% 体积 (推荐)"),
    SPEED("极速构建模式", "仅保留主世界核心，耗时缩减一半以上"),
    CUSTOM("自定义维度", "自主勾选需要保留或裁剪的维度")
}

data class ConversionConfig(
    val taskId: String = UUID.randomUUID().toString(),
    val inputDir: File,
    val outputDir: File,
    val targetFormat: ChunkerFormat,
    val keepOriginalNbt: Boolean = false,
    val lowMemoryMode: Boolean = false,
    val maxMemoryMB: Int = 2048,

    val pruningProfile: PruningProfile = PruningProfile.FULL,
    val includeOverworld: Boolean = true,
    val includeNether: Boolean = true,
    val includeTheEnd: Boolean = true,

    val overrideWorldName: String? = null,
    val overrideGameMode: String? = null,
    val overrideDifficulty: String? = null
) {

    fun buildPruningJson(): String? {
        val exclusions = mutableListOf<String>()
        if (!includeOverworld) exclusions.add("\"OVERWORLD\": {\"include\": false}")
        if (!includeNether) exclusions.add("\"NETHER\": {\"include\": false}")
        if (!includeTheEnd) exclusions.add("\"THE_END\": {\"include\": false}")

        return if (exclusions.isNotEmpty()) {
            "{${exclusions.joinToString(", ")}}"
        } else null
    }

    fun buildWorldSettingsJson(): String? {
        val pairs = mutableListOf<String>()
        if (!overrideWorldName.isNullOrBlank()) {
            val safeName = overrideWorldName.replace("\"", "\\\"")
            pairs.add("\"world_name\": \"$safeName\"")
        }
        if (!overrideGameMode.isNullOrBlank() && overrideGameMode != "DEFAULT") {
            pairs.add("\"game_mode\": \"$overrideGameMode\"")
        }
        if (!overrideDifficulty.isNullOrBlank() && overrideDifficulty != "DEFAULT") {
            pairs.add("\"difficulty\": \"$overrideDifficulty\"")
        }

        return if (pairs.isNotEmpty()) {
            "{${pairs.joinToString(", ")}}"
        } else null
    }
}
