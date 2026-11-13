package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.NbtIO
import java.io.File
import java.io.FileInputStream

data class JavaTargetProfile(
    val dataVersion: Int,
    val usesEntitiesDir: Boolean,
    val usesDimensionNamespaces: Boolean,
    val usesModernComponents: Boolean,
    val usesFlatComponents: Boolean,
    val usesModernEquipment: Boolean,
    val usesModernPotionIds: Boolean,
    val usesShortFuse: Boolean
)

/**
 * Resolves Java version traits, DataVersion values, and world entity directory layouts
 * consistently across BE->JE and JE->JE pipelines.
 */
object JavaTargetFormatHelper {

    private data class SemVer(val major: Int, val minor: Int, val patch: Int)

    fun resolveProfile(
        targetFormatId: String,
        outputDir: File? = null,
        inputDir: File? = null
    ): JavaTargetProfile {
        val levelDataVersion = readLevelDataVersion(outputDir)
            ?: if (targetFormatId == "INPUT") readLevelDataVersion(inputDir) else null

        val semVer = parseVersion(targetFormatId, levelDataVersion)
        val dataVersion = levelDataVersion ?: resolveDataVersion(targetFormatId, semVer)

        val usesDimNamespace = semVer.major >= 26 ||
            hasDimNamespaceLayout(outputDir) ||
            (targetFormatId == "INPUT" && hasDimNamespaceLayout(inputDir))

        val usesEntitiesDir = semVer.major > 1 ||
            semVer.minor >= 17 ||
            hasEntitiesDirLayout(outputDir) ||
            (targetFormatId == "INPUT" && hasEntitiesDirLayout(inputDir))

        val modernComponents = semVer.major > 1 ||
            semVer.minor > 20 ||
            (semVer.minor == 20 && semVer.patch >= 5)

        val flatOr1215 = semVer.major > 1 ||
            semVer.minor > 21 ||
            (semVer.minor == 21 && semVer.patch >= 5)

        val shortFuse = semVer.major > 1 ||
            semVer.minor > 20 ||
            (semVer.minor == 20 && semVer.patch >= 4)

        return JavaTargetProfile(
            dataVersion = dataVersion,
            usesEntitiesDir = usesEntitiesDir,
            usesDimensionNamespaces = usesDimNamespace,
            usesModernComponents = modernComponents,
            usesFlatComponents = flatOr1215,
            usesModernEquipment = flatOr1215,
            usesModernPotionIds = flatOr1215,
            usesShortFuse = shortFuse
        )
    }

    fun resolveDataVersion(targetFormatId: String): Int {
        val semVer = parseVersion(targetFormatId, null)
        return resolveDataVersion(targetFormatId, semVer)
    }

    fun getEntitiesDirectory(worldDir: File, dimensionId: Int, useDimNamespace: Boolean): File {
        return if (useDimNamespace) {
            when (dimensionId) {
                1 -> File(worldDir, "dimensions/minecraft/the_nether/entities")
                2 -> File(worldDir, "dimensions/minecraft/the_end/entities")
                else -> File(worldDir, "dimensions/minecraft/overworld/entities")
            }
        } else {
            when (dimensionId) {
                1 -> File(worldDir, "DIM-1/entities")
                2 -> File(worldDir, "DIM1/entities")
                else -> File(worldDir, "entities")
            }
        }
    }

    fun getRegionDirectory(worldDir: File, dimensionId: Int, useDimNamespace: Boolean = false): File {
        return if (useDimNamespace) {
            when (dimensionId) {
                1 -> File(worldDir, "dimensions/minecraft/the_nether/region")
                2 -> File(worldDir, "dimensions/minecraft/the_end/region")
                else -> File(worldDir, "dimensions/minecraft/overworld/region")
            }
        } else {
            when (dimensionId) {
                1 -> File(worldDir, "DIM-1/region")
                2 -> File(worldDir, "DIM1/region")
                else -> File(worldDir, "region")
            }
        }
    }

    private fun resolveDataVersion(targetFormatId: String, semVer: SemVer): Int {
        if (targetFormatId.startsWith("JAVA_26_")) {
            return when {
                targetFormatId == "JAVA_26_3" -> 5001
                targetFormatId == "JAVA_26_2" -> 4903
                targetFormatId == "JAVA_26_1_2" -> 4790
                targetFormatId == "JAVA_26_1_1" -> 4788
                targetFormatId == "JAVA_26_1" -> 4786
                semVer.minor >= 3 -> 5001
                semVer.minor == 2 -> 4903
                else -> 4786
            }
        }
        if (semVer.major >= 26) {
            return when {
                semVer.minor >= 3 -> 5001
                semVer.minor == 2 -> 4903
                else -> 4786
            }
        }
        if (semVer.major == 1 && semVer.minor == 21) {
            return when {
                semVer.patch >= 11 -> 4671
                semVer.patch == 10 -> 4556
                semVer.patch >= 8 -> 4440
                semVer.patch >= 5 -> 4325
                else -> 3953
            }
        }
        if (semVer.major == 1 && semVer.minor == 20) {
            return when {
                semVer.patch >= 6 -> 3839
                semVer.patch == 5 -> 3837
                semVer.patch == 4 -> 3700
                semVer.patch == 3 -> 3698
                semVer.patch == 2 -> 3578
                else -> 3465
            }
        }
        return when (semVer.minor) {
            19 -> 3337
            18 -> 2975
            17 -> 2730
            16 -> 2586
            15 -> 2230
            14 -> 1976
            13 -> 1631
            12 -> 1343
            else -> 4671
        }
    }

    private fun parseVersion(targetFormatId: String, dataVersionHint: Int?): SemVer {
        if (targetFormatId.startsWith("JAVA_")) {
            val parts = targetFormatId.removePrefix("JAVA_").split("_")
            val major = parts.getOrNull(0)?.toIntOrNull()
            val minor = parts.getOrNull(1)?.toIntOrNull()
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            if (major != null) {
                return SemVer(major, minor ?: if (major >= 26) 3 else 21, patch)
            }
        }
        if (dataVersionHint != null) {
            return when {
                dataVersionHint >= 5000 -> SemVer(26, 3, 0)
                dataVersionHint >= 4900 -> SemVer(26, 2, 0)
                dataVersionHint >= 4780 -> SemVer(26, 1, 0)
                dataVersionHint >= 4670 -> SemVer(1, 21, 11)
                dataVersionHint >= 4550 -> SemVer(1, 21, 10)
                dataVersionHint >= 4435 -> SemVer(1, 21, 8)
                dataVersionHint >= 4320 -> SemVer(1, 21, 5)
                dataVersionHint >= 3950 -> SemVer(1, 21, 0)
                dataVersionHint >= 3837 -> SemVer(1, 20, 5)
                dataVersionHint >= 3460 -> SemVer(1, 20, 0)
                dataVersionHint >= 3100 -> SemVer(1, 19, 0)
                dataVersionHint >= 2860 -> SemVer(1, 18, 0)
                dataVersionHint >= 2720 -> SemVer(1, 17, 0)
                else -> SemVer(1, 16, 5)
            }
        }
        return SemVer(1, 21, 11)
    }

    private fun readLevelDataVersion(worldDir: File?): Int? {
        if (worldDir == null) return null
        val levelDat = File(worldDir, "level.dat")
        if (!levelDat.isFile) return null
        return try {
            FileInputStream(levelDat).use { fis ->
                val nbt = NbtIO.readNbtFile(fis, compressed = true, littleEndian = false)
                val data = nbt.compound.getNullableCompound("Data") ?: nbt.compound
                data.getNullableInt("DataVersion")
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun hasDimNamespaceLayout(worldDir: File?): Boolean {
        if (worldDir == null) return false
        return File(worldDir, "dimensions/minecraft/overworld").isDirectory
    }

    private fun hasEntitiesDirLayout(worldDir: File?): Boolean {
        if (worldDir == null) return false
        return File(worldDir, "entities").isDirectory ||
            File(worldDir, "dimensions/minecraft/overworld/entities").isDirectory
    }
}
