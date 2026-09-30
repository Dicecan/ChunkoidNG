package com.noches.chunkoidng.core.version

import com.noches.chunkoidng.core.world.Platform

data class MinecraftVersion(
    val platform: Platform,
    val major: Int,
    val minor: Int,
    val patch: Int = 0,
    val build: Int = 0,
    val dataVersion: Int? = null,
    val rawString: String? = null
) : Comparable<MinecraftVersion> {

    val epoch: MinecraftEpoch
        get() = MinecraftEpoch.fromMinor(minor)

    val displayString: String
        get() = when {
            rawString != null && rawString.isNotBlank() -> rawString
            platform == Platform.BEDROCK && build > 0 -> "$major.$minor.$patch.$build"
            patch > 0 -> "$major.$minor.$patch"
            else -> "$major.$minor"
        }

    fun toComparableLong(): Long {
        return (major.toLong() shl 48) or
                (minor.toLong() shl 32) or
                (patch.toLong() shl 16) or
                (build.toLong())
    }

    override fun compareTo(other: MinecraftVersion): Int {
        if (this.platform == other.platform) {
            return this.toComparableLong().compareTo(other.toComparableLong())
        }
        return this.epoch.minorVersion.compareTo(other.epoch.minorVersion)
    }

    fun isNewerThan(other: MinecraftVersion): Boolean = this > other
    fun isOlderThan(other: MinecraftVersion): Boolean = this < other
    fun isSameEpoch(other: MinecraftVersion): Boolean = this.epoch == other.epoch

    companion object {
        fun parse(
            versionStr: String?,
            platform: Platform,
            dataVersion: Int? = null
        ): MinecraftVersion {
            if (platform == Platform.JAVA && dataVersion != null && dataVersion > 1000) {
                val mapped = mapDataVersionToJava(dataVersion)
                if (mapped != null) {
                    return mapped.copy(rawString = versionStr ?: mapped.displayString)
                }
            }

            if (versionStr.isNullOrBlank()) {
                val fallbackMinor = if (platform == Platform.JAVA) 21 else 21
                return MinecraftVersion(
                    platform = platform,
                    major = 1,
                    minor = fallbackMinor,
                    patch = 0,
                    dataVersion = dataVersion,
                    rawString = "1.$fallbackMinor"
                )
            }

            val cleaned = versionStr.trim()
                .replace(Regex("(?i)^minecraft\\s*"), "")
                .replace(Regex("(?i)^bedrock\\s*"), "")
                .replace(Regex("(?i)^java\\s*"), "")
                .replace(Regex("(?i)^v"), "")
                .trim()

            val parts = cleaned.split(".").map { token ->
                token.takeWhile { ch -> ch.isDigit() }.toIntOrNull()
            }
            if (parts.isEmpty() || parts.any { it == null }) {
                return MinecraftVersion(platform, 1, 21, 0, 0, dataVersion, versionStr)
            }
            val numericParts = parts.filterNotNull()

            return when {
                numericParts.isEmpty() -> MinecraftVersion(platform, 1, 21, 0, 0, dataVersion, versionStr)
                numericParts.size == 1 -> MinecraftVersion(platform, numericParts[0], 0, 0, 0, dataVersion, versionStr)
                numericParts.size == 2 -> MinecraftVersion(platform, numericParts[0], numericParts[1], 0, 0, dataVersion, versionStr)
                numericParts.size == 3 -> MinecraftVersion(platform, numericParts[0], numericParts[1], numericParts[2], 0, dataVersion, versionStr)
                else -> MinecraftVersion(platform, numericParts[0], numericParts[1], numericParts[2], numericParts[3], dataVersion, versionStr)
            }
        }

        fun parseFromChunkerId(id: String): MinecraftVersion? {
            if (id == "INPUT") return null
            val isJava = id.startsWith("JAVA_")
            val isBedrock = id.startsWith("BEDROCK_")
            if (!isJava && !isBedrock) return null

            val platform = if (isJava) Platform.JAVA else Platform.BEDROCK
            val sub = if (isJava) id.removePrefix("JAVA_") else id.removePrefix("BEDROCK_")
            val parts = sub.split("_").mapNotNull { it.toIntOrNull() }
            if (parts.isEmpty()) return null

            return when (parts.size) {
                1 -> MinecraftVersion(platform, parts[0], 0, 0)
                2 -> MinecraftVersion(platform, parts[0], parts[1], 0)
                3 -> MinecraftVersion(platform, parts[0], parts[1], parts[2])
                else -> MinecraftVersion(platform, parts[0], parts[1], parts[2], parts[3])
            }
        }

        private fun mapDataVersionToJava(dataVersion: Int): MinecraftVersion? {
            return when {
                dataVersion >= 3953 -> MinecraftVersion(Platform.JAVA, 1, 21, 0, dataVersion = dataVersion)
                dataVersion >= 3839 -> MinecraftVersion(Platform.JAVA, 1, 20, 5, dataVersion = dataVersion)
                dataVersion >= 3700 -> MinecraftVersion(Platform.JAVA, 1, 20, 4, dataVersion = dataVersion)
                dataVersion >= 3465 -> MinecraftVersion(Platform.JAVA, 1, 20, 0, dataVersion = dataVersion)
                dataVersion >= 3337 -> MinecraftVersion(Platform.JAVA, 1, 19, 4, dataVersion = dataVersion)
                dataVersion >= 3120 -> MinecraftVersion(Platform.JAVA, 1, 19, 0, dataVersion = dataVersion)
                dataVersion >= 2975 -> MinecraftVersion(Platform.JAVA, 1, 18, 2, dataVersion = dataVersion)
                dataVersion >= 2865 -> MinecraftVersion(Platform.JAVA, 1, 18, 0, dataVersion = dataVersion)
                dataVersion >= 2730 -> MinecraftVersion(Platform.JAVA, 1, 17, 1, dataVersion = dataVersion)
                dataVersion >= 2586 -> MinecraftVersion(Platform.JAVA, 1, 16, 5, dataVersion = dataVersion)
                dataVersion >= 2567 -> MinecraftVersion(Platform.JAVA, 1, 16, 0, dataVersion = dataVersion)
                dataVersion >= 2230 -> MinecraftVersion(Platform.JAVA, 1, 15, 2, dataVersion = dataVersion)
                dataVersion >= 1976 -> MinecraftVersion(Platform.JAVA, 1, 14, 4, dataVersion = dataVersion)
                dataVersion >= 1631 -> MinecraftVersion(Platform.JAVA, 1, 13, 2, dataVersion = dataVersion)
                dataVersion >= 1343 -> MinecraftVersion(Platform.JAVA, 1, 12, 2, dataVersion = dataVersion)
                dataVersion >= 1139 -> MinecraftVersion(Platform.JAVA, 1, 11, 2, dataVersion = dataVersion)
                dataVersion >= 922  -> MinecraftVersion(Platform.JAVA, 1, 10, 2, dataVersion = dataVersion)
                dataVersion >= 169  -> MinecraftVersion(Platform.JAVA, 1, 9, 0, dataVersion = dataVersion)
                else -> null
            }
        }
    }
}
