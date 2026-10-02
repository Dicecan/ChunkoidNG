package com.noches.chunkoidng.core.pack

import java.io.File

enum class PackPlatform {
    JAVA,
    BEDROCK
}

data class JavaPackFormat(
    val format: Int,
    val versionName: String,
    val description: String
) {
    companion object {
        val ALL = listOf(
            JavaPackFormat(3, "1.12.2", "Pre-flattening legacy format"),
            JavaPackFormat(4, "1.13 - 1.14.4", "The Flattening update"),
            JavaPackFormat(5, "1.15 - 1.16.1", "Buzzy Bees update"),
            JavaPackFormat(6, "1.16.2 - 1.16.5", "Nether update"),
            JavaPackFormat(7, "1.17 - 1.17.1", "Caves & Cliffs Part I"),
            JavaPackFormat(8, "1.18 - 1.18.2", "Caves & Cliffs Part II"),
            JavaPackFormat(9, "1.19 - 1.19.2", "The Wild Update"),
            JavaPackFormat(12, "1.19.3", "Vex model & creative tab update"),
            JavaPackFormat(13, "1.19.4", "Display entities & armor trim"),
            JavaPackFormat(15, "1.20 - 1.20.1", "Trails & Tales update"),
            JavaPackFormat(18, "1.20.2", "GUI sprites refactoring"),
            JavaPackFormat(22, "1.20.3 - 1.20.4", "Decorated pots & crafter"),
            JavaPackFormat(32, "1.20.5 - 1.20.6", "Armadillo & wolf armor"),
            JavaPackFormat(34, "1.21 - 1.21.1", "Tricky Trials update"),
            JavaPackFormat(42, "1.21.2 - 1.21.3", "Bundles & pale garden"),
            JavaPackFormat(46, "1.21.4", "Creaking & winter drop")
        )

        fun findByFormat(format: Int): JavaPackFormat {
            return ALL.find { it.format == format } ?: JavaPackFormat(format, "Custom (Format $format)", "")
        }

        fun defaultFormat(): JavaPackFormat = ALL.first { it.format == 34 }
    }
}

data class BedrockEngineVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) {
    fun toArray(): List<Int> = listOf(major, minor, patch)
    fun toVersionString(): String = "$major.$minor.$patch"

    companion object {
        val V_1_21_0 = BedrockEngineVersion(1, 21, 0)
        val V_1_20_0 = BedrockEngineVersion(1, 20, 0)
        val V_1_19_0 = BedrockEngineVersion(1, 19, 0)
        val V_1_18_0 = BedrockEngineVersion(1, 18, 0)
        val V_1_16_0 = BedrockEngineVersion(1, 16, 0)

        val ALL = listOf(V_1_21_0, V_1_20_0, V_1_19_0, V_1_18_0, V_1_16_0)
        fun defaultVersion(): BedrockEngineVersion = V_1_21_0
    }
}

data class PackMetadata(
    val platform: PackPlatform,
    val name: String,
    val description: String,
    val javaFormat: Int? = null,
    val bedrockEngineVersion: BedrockEngineVersion? = null,
    val iconFile: File? = null,
    val textureCount: Int = 0,
    val blockTextureCount: Int = 0,
    val itemTextureCount: Int = 0,
    val langCount: Int = 0,
    val animationCount: Int = 0,
    val soundCount: Int = 0
)

data class PackConversionConfig(
    val targetPlatform: PackPlatform,
    val targetJavaFormat: Int = 34,
    val targetBedrockVersion: BedrockEngineVersion = BedrockEngineVersion.V_1_21_0,
    val generateAtlases: Boolean = true,
    val convertAnimations: Boolean = true,
    val convertLanguages: Boolean = true,
    val convertSounds: Boolean = true,
    val convertArmorModels: Boolean = true,
    val convertSplashes: Boolean = true,
    val customPackName: String? = null,
    val customDescription: String? = null
)

data class PackConversionProgress(
    val stage: String,
    val current: Int,
    val total: Int,
    val log: String? = null
)
