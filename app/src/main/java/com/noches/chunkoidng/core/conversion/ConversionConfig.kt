package com.noches.chunkoidng.core.conversion

import java.io.File

/**
 * Configuration options for world conversion.
 */
data class ConversionConfig(
    val inputDir: File,
    val outputDir: File,
    val targetFormat: ChunkerFormat,
    val keepOriginalNbt: Boolean = false,
    val lowMemoryMode: Boolean = false,
    val maxMemoryMB: Int = 2048,
    // Pruning
    val includeOverworld: Boolean = true,
    val includeNether: Boolean = true,
    val includeTheEnd: Boolean = true,
    // World settings overrides
    val overrideWorldName: String? = null,
    val overrideGameMode: String? = null,
    val overrideDifficulty: String? = null
) {
    /**
     * Builds the JSON string for the -p / --pruning option if any dimension is excluded.
     */
    fun buildPruningJson(): String? {
        val exclusions = mutableListOf<String>()
        if (!includeOverworld) exclusions.add("\"OVERWORLD\": {\"include\": false}")
        if (!includeNether) exclusions.add("\"NETHER\": {\"include\": false}")
        if (!includeTheEnd) exclusions.add("\"THE_END\": {\"include\": false}")

        return if (exclusions.isNotEmpty()) {
            "{${exclusions.joinToString(", ")}}"
        } else null
    }

    /**
     * Builds the JSON string for the -s / --worldSettings option if any overrides are specified.
     */
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
