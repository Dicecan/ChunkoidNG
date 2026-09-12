package com.noches.chunkoidng.core.conversion

import com.noches.chunkoidng.core.world.Platform

/**
 * Format representations for target conversions supported by Chunker CLI.
 */
data class ChunkerFormat(
    val id: String,
    val platform: Platform,
    val group: String,
    val displayName: String,
    val isPopular: Boolean = false
) {
    companion object {
        val FORMAT_INPUT = ChunkerFormat(
            id = "INPUT",
            platform = Platform.BEDROCK, // Matches source
            group = "原格式",
            displayName = "保持原版本 (INPUT)",
            isPopular = true
        )

        val BEDROCK_FORMATS: List<ChunkerFormat> = listOf(
            // 1.21
            ChunkerFormat("BEDROCK_1_21_130", Platform.BEDROCK, "1.21.x", "Bedrock 1.21.130", true),
            ChunkerFormat("BEDROCK_1_21_120", Platform.BEDROCK, "1.21.x", "Bedrock 1.21.120"),
            ChunkerFormat("BEDROCK_1_21_110", Platform.BEDROCK, "1.21.x", "Bedrock 1.21.110"),
            ChunkerFormat("BEDROCK_1_21_100", Platform.BEDROCK, "1.21.x", "Bedrock 1.21.100"),
            ChunkerFormat("BEDROCK_1_21_90", Platform.BEDROCK, "1.21.x", "Bedrock 1.21.90"),
            ChunkerFormat("BEDROCK_1_21_80", Platform.BEDROCK, "1.21.x", "Bedrock 1.21.80"),
            ChunkerFormat("BEDROCK_1_21_70", Platform.BEDROCK, "1.21.x", "Bedrock 1.21.70"),
            ChunkerFormat("BEDROCK_1_21_60", Platform.BEDROCK, "1.21.x", "Bedrock 1.21.60"),
            ChunkerFormat("BEDROCK_1_21_50", Platform.BEDROCK, "1.21.x", "Bedrock 1.21.50", true),
            ChunkerFormat("BEDROCK_1_21_40", Platform.BEDROCK, "1.21.x", "Bedrock 1.21.40"),
            ChunkerFormat("BEDROCK_1_21_30", Platform.BEDROCK, "1.21.x", "Bedrock 1.21.30"),
            ChunkerFormat("BEDROCK_1_21_20", Platform.BEDROCK, "1.21.x", "Bedrock 1.21.20"),
            ChunkerFormat("BEDROCK_1_21_0", Platform.BEDROCK, "1.21.x", "Bedrock 1.21.0", true),
            // 1.20
            ChunkerFormat("BEDROCK_1_20_80", Platform.BEDROCK, "1.20.x", "Bedrock 1.20.80", true),
            ChunkerFormat("BEDROCK_1_20_70", Platform.BEDROCK, "1.20.x", "Bedrock 1.20.70"),
            ChunkerFormat("BEDROCK_1_20_60", Platform.BEDROCK, "1.20.x", "Bedrock 1.20.60"),
            ChunkerFormat("BEDROCK_1_20_50", Platform.BEDROCK, "1.20.x", "Bedrock 1.20.50"),
            ChunkerFormat("BEDROCK_1_20_40", Platform.BEDROCK, "1.20.x", "Bedrock 1.20.40"),
            ChunkerFormat("BEDROCK_1_20_30", Platform.BEDROCK, "1.20.x", "Bedrock 1.20.30"),
            ChunkerFormat("BEDROCK_1_20_20", Platform.BEDROCK, "1.20.x", "Bedrock 1.20.20"),
            ChunkerFormat("BEDROCK_1_20_10", Platform.BEDROCK, "1.20.x", "Bedrock 1.20.10"),
            ChunkerFormat("BEDROCK_1_20_0", Platform.BEDROCK, "1.20.x", "Bedrock 1.20.0"),
            // 1.19
            ChunkerFormat("BEDROCK_1_19_80", Platform.BEDROCK, "1.19.x", "Bedrock 1.19.80"),
            ChunkerFormat("BEDROCK_1_19_70", Platform.BEDROCK, "1.19.x", "Bedrock 1.19.70"),
            ChunkerFormat("BEDROCK_1_19_60", Platform.BEDROCK, "1.19.x", "Bedrock 1.19.60"),
            ChunkerFormat("BEDROCK_1_19_50", Platform.BEDROCK, "1.19.x", "Bedrock 1.19.50"),
            ChunkerFormat("BEDROCK_1_19_40", Platform.BEDROCK, "1.19.x", "Bedrock 1.19.40"),
            ChunkerFormat("BEDROCK_1_19_30", Platform.BEDROCK, "1.19.x", "Bedrock 1.19.30"),
            ChunkerFormat("BEDROCK_1_19_20", Platform.BEDROCK, "1.19.x", "Bedrock 1.19.20"),
            ChunkerFormat("BEDROCK_1_19_10", Platform.BEDROCK, "1.19.x", "Bedrock 1.19.10"),
            ChunkerFormat("BEDROCK_1_19_0", Platform.BEDROCK, "1.19.x", "Bedrock 1.19.0"),
            // 1.18
            ChunkerFormat("BEDROCK_1_18_30", Platform.BEDROCK, "1.18.x", "Bedrock 1.18.30"),
            ChunkerFormat("BEDROCK_1_18_20", Platform.BEDROCK, "1.18.x", "Bedrock 1.18.20"),
            ChunkerFormat("BEDROCK_1_18_10", Platform.BEDROCK, "1.18.x", "Bedrock 1.18.10"),
            ChunkerFormat("BEDROCK_1_18_0", Platform.BEDROCK, "1.18.x", "Bedrock 1.18.0"),
            // 1.17
            ChunkerFormat("BEDROCK_1_17_40", Platform.BEDROCK, "1.17.x", "Bedrock 1.17.40"),
            ChunkerFormat("BEDROCK_1_17_30", Platform.BEDROCK, "1.17.x", "Bedrock 1.17.30"),
            ChunkerFormat("BEDROCK_1_17_20", Platform.BEDROCK, "1.17.x", "Bedrock 1.17.20"),
            ChunkerFormat("BEDROCK_1_17_10", Platform.BEDROCK, "1.17.x", "Bedrock 1.17.10"),
            ChunkerFormat("BEDROCK_1_17_0", Platform.BEDROCK, "1.17.x", "Bedrock 1.17.0"),
            // 1.16
            ChunkerFormat("BEDROCK_1_16_220", Platform.BEDROCK, "1.16.x", "Bedrock 1.16.220"),
            ChunkerFormat("BEDROCK_1_16_210", Platform.BEDROCK, "1.16.x", "Bedrock 1.16.210"),
            ChunkerFormat("BEDROCK_1_16_200", Platform.BEDROCK, "1.16.x", "Bedrock 1.16.200"),
            ChunkerFormat("BEDROCK_1_16_100", Platform.BEDROCK, "1.16.x", "Bedrock 1.16.100"),
            ChunkerFormat("BEDROCK_1_16_20", Platform.BEDROCK, "1.16.x", "Bedrock 1.16.20"),
            ChunkerFormat("BEDROCK_1_16_0", Platform.BEDROCK, "1.16.x", "Bedrock 1.16.0"),
            // 1.14
            ChunkerFormat("BEDROCK_1_14_60", Platform.BEDROCK, "1.14.x", "Bedrock 1.14.60"),
            ChunkerFormat("BEDROCK_1_14_30", Platform.BEDROCK, "1.14.x", "Bedrock 1.14.30"),
            ChunkerFormat("BEDROCK_1_14_20", Platform.BEDROCK, "1.14.x", "Bedrock 1.14.20"),
            ChunkerFormat("BEDROCK_1_14_1", Platform.BEDROCK, "1.14.x", "Bedrock 1.14.1"),
            ChunkerFormat("BEDROCK_1_14_0", Platform.BEDROCK, "1.14.x", "Bedrock 1.14.0"),
            // 1.13 & 1.12
            ChunkerFormat("BEDROCK_1_13_0", Platform.BEDROCK, "1.13 / 1.12", "Bedrock 1.13.0"),
            ChunkerFormat("BEDROCK_1_12_0", Platform.BEDROCK, "1.13 / 1.12", "Bedrock 1.12.0"),
            // 1.26
            ChunkerFormat("BEDROCK_1_26_50", Platform.BEDROCK, "1.26.x", "Bedrock 1.26.50"),
            ChunkerFormat("BEDROCK_1_26_30", Platform.BEDROCK, "1.26.x", "Bedrock 1.26.30"),
            ChunkerFormat("BEDROCK_1_26_20", Platform.BEDROCK, "1.26.x", "Bedrock 1.26.20"),
            ChunkerFormat("BEDROCK_1_26_10", Platform.BEDROCK, "1.26.x", "Bedrock 1.26.10"),
            ChunkerFormat("BEDROCK_1_26_0", Platform.BEDROCK, "1.26.x", "Bedrock 1.26.0")
        )

        val JAVA_FORMATS: List<ChunkerFormat> = listOf(
            // 1.21
            ChunkerFormat("JAVA_1_21_11", Platform.JAVA, "1.21.x", "Java 1.21.11", true),
            ChunkerFormat("JAVA_1_21_10", Platform.JAVA, "1.21.x", "Java 1.21.10"),
            ChunkerFormat("JAVA_1_21_9", Platform.JAVA, "1.21.x", "Java 1.21.9"),
            ChunkerFormat("JAVA_1_21_8", Platform.JAVA, "1.21.x", "Java 1.21.8"),
            ChunkerFormat("JAVA_1_21_7", Platform.JAVA, "1.21.x", "Java 1.21.7"),
            ChunkerFormat("JAVA_1_21_6", Platform.JAVA, "1.21.x", "Java 1.21.6"),
            ChunkerFormat("JAVA_1_21_5", Platform.JAVA, "1.21.x", "Java 1.21.5"),
            ChunkerFormat("JAVA_1_21_4", Platform.JAVA, "1.21.x", "Java 1.21.4", true),
            ChunkerFormat("JAVA_1_21_3", Platform.JAVA, "1.21.x", "Java 1.21.3"),
            ChunkerFormat("JAVA_1_21_2", Platform.JAVA, "1.21.x", "Java 1.21.2"),
            ChunkerFormat("JAVA_1_21_1", Platform.JAVA, "1.21.x", "Java 1.21.1"),
            ChunkerFormat("JAVA_1_21", Platform.JAVA, "1.21.x", "Java 1.21.0"),
            // 1.20
            ChunkerFormat("JAVA_1_20_6", Platform.JAVA, "1.20.x", "Java 1.20.6"),
            ChunkerFormat("JAVA_1_20_5", Platform.JAVA, "1.20.x", "Java 1.20.5"),
            ChunkerFormat("JAVA_1_20_4", Platform.JAVA, "1.20.x", "Java 1.20.4", true),
            ChunkerFormat("JAVA_1_20_3", Platform.JAVA, "1.20.x", "Java 1.20.3"),
            ChunkerFormat("JAVA_1_20_2", Platform.JAVA, "1.20.x", "Java 1.20.2"),
            ChunkerFormat("JAVA_1_20_1", Platform.JAVA, "1.20.x", "Java 1.20.1", true),
            ChunkerFormat("JAVA_1_20", Platform.JAVA, "1.20.x", "Java 1.20.0"),
            // 1.19
            ChunkerFormat("JAVA_1_19_4", Platform.JAVA, "1.19.x", "Java 1.19.4"),
            ChunkerFormat("JAVA_1_19_3", Platform.JAVA, "1.19.x", "Java 1.19.3"),
            ChunkerFormat("JAVA_1_19_2", Platform.JAVA, "1.19.x", "Java 1.19.2", true),
            ChunkerFormat("JAVA_1_19_1", Platform.JAVA, "1.19.x", "Java 1.19.1"),
            ChunkerFormat("JAVA_1_19", Platform.JAVA, "1.19.x", "Java 1.19.0"),
            // 1.18
            ChunkerFormat("JAVA_1_18_2", Platform.JAVA, "1.18.x", "Java 1.18.2", true),
            ChunkerFormat("JAVA_1_18_1", Platform.JAVA, "1.18.x", "Java 1.18.1"),
            ChunkerFormat("JAVA_1_18", Platform.JAVA, "1.18.x", "Java 1.18.0"),
            // 1.17
            ChunkerFormat("JAVA_1_17_1", Platform.JAVA, "1.17.x", "Java 1.17.1"),
            ChunkerFormat("JAVA_1_17", Platform.JAVA, "1.17.x", "Java 1.17.0"),
            // 1.16
            ChunkerFormat("JAVA_1_16_5", Platform.JAVA, "1.16.x", "Java 1.16.5", true),
            ChunkerFormat("JAVA_1_16_4", Platform.JAVA, "1.16.x", "Java 1.16.4"),
            ChunkerFormat("JAVA_1_16_3", Platform.JAVA, "1.16.x", "Java 1.16.3"),
            ChunkerFormat("JAVA_1_16_2", Platform.JAVA, "1.16.x", "Java 1.16.2"),
            ChunkerFormat("JAVA_1_16_1", Platform.JAVA, "1.16.x", "Java 1.16.1"),
            ChunkerFormat("JAVA_1_16", Platform.JAVA, "1.16.x", "Java 1.16.0"),
            // 1.15
            ChunkerFormat("JAVA_1_15_2", Platform.JAVA, "1.15.x", "Java 1.15.2"),
            ChunkerFormat("JAVA_1_15_1", Platform.JAVA, "1.15.x", "Java 1.15.1"),
            ChunkerFormat("JAVA_1_15", Platform.JAVA, "1.15.x", "Java 1.15.0"),
            // 1.14
            ChunkerFormat("JAVA_1_14_4", Platform.JAVA, "1.14.x", "Java 1.14.4"),
            ChunkerFormat("JAVA_1_14_3", Platform.JAVA, "1.14.x", "Java 1.14.3"),
            ChunkerFormat("JAVA_1_14_2", Platform.JAVA, "1.14.x", "Java 1.14.2"),
            ChunkerFormat("JAVA_1_14_1", Platform.JAVA, "1.14.x", "Java 1.14.1"),
            ChunkerFormat("JAVA_1_14", Platform.JAVA, "1.14.x", "Java 1.14.0"),
            // 1.13
            ChunkerFormat("JAVA_1_13_2", Platform.JAVA, "1.13.x", "Java 1.13.2"),
            ChunkerFormat("JAVA_1_13_1", Platform.JAVA, "1.13.x", "Java 1.13.1"),
            ChunkerFormat("JAVA_1_13", Platform.JAVA, "1.13.x", "Java 1.13.0"),
            // 1.12
            ChunkerFormat("JAVA_1_12_2", Platform.JAVA, "1.12.x", "Java 1.12.2", true),
            ChunkerFormat("JAVA_1_12_1", Platform.JAVA, "1.12.x", "Java 1.12.1"),
            ChunkerFormat("JAVA_1_12", Platform.JAVA, "1.12.x", "Java 1.12.0"),
            // 1.11, 1.10, 1.9, 1.8.8
            ChunkerFormat("JAVA_1_11_2", Platform.JAVA, "1.11 - 1.8", "Java 1.11.2"),
            ChunkerFormat("JAVA_1_10_2", Platform.JAVA, "1.11 - 1.8", "Java 1.10.2"),
            ChunkerFormat("JAVA_1_9_3", Platform.JAVA, "1.11 - 1.8", "Java 1.9.3"),
            ChunkerFormat("JAVA_1_8_8", Platform.JAVA, "1.11 - 1.8", "Java 1.8.8", true),
            // 26.x
            ChunkerFormat("JAVA_26_3", Platform.JAVA, "26.x", "Java 26.3"),
            ChunkerFormat("JAVA_26_2", Platform.JAVA, "26.x", "Java 26.2"),
            ChunkerFormat("JAVA_26_1", Platform.JAVA, "26.x", "Java 26.1")
        )

        fun findById(id: String): ChunkerFormat? {
            if (id == "INPUT") return FORMAT_INPUT
            return BEDROCK_FORMATS.find { it.id == id } ?: JAVA_FORMATS.find { it.id == id }
        }

        fun getDefaultFormatForOpposite(sourcePlatform: Platform): ChunkerFormat {
            return if (sourcePlatform == Platform.JAVA) {
                // Java -> Bedrock default: 1.21.50
                BEDROCK_FORMATS.first { it.id == "BEDROCK_1_21_50" }
            } else {
                // Bedrock -> Java default: 1.21.4
                JAVA_FORMATS.first { it.id == "JAVA_1_21_4" }
            }
        }
    }
}
