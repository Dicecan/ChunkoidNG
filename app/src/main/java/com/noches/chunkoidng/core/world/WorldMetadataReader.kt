package com.noches.chunkoidng.core.world

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtIO
import br.com.gamemods.nbtmanipulator.NbtInt
import br.com.gamemods.nbtmanipulator.NbtList
import java.io.File
import java.io.InputStream

/**
 * Reads Minecraft world metadata (version, world name, platform, icon) from level.dat and auxiliary files.
 */
object WorldMetadataReader {

    data class ParsedMetadata(
        val worldName: String?,
        val versionName: String?,
        val versionId: Int?,
        val platform: Platform,
        val gameType: String?
    )

    /**
     * Inspects a local world directory and extracts all available metadata.
     */
    fun inspectWorld(worldDir: File, fallbackName: String = worldDir.name): WorldInfo {
        val platform = detectPlatform(worldDir)
        val levelDatFile = File(worldDir, "level.dat")
        val parsed = if (levelDatFile.exists()) {
            readLevelDat(levelDatFile)
        } else null

        // Determine name: levelname.txt > NBT LevelName > fallback
        val nameFromTxt = readLevelNameTxt(worldDir)
        val resolvedName = nameFromTxt ?: parsed?.worldName ?: fallbackName

        val resolvedPlatform = parsed?.platform ?: platform
        val iconBitmap = loadWorldIcon(worldDir)
        val dirSize = calculateDirectorySize(worldDir)

        return WorldInfo(
            name = resolvedName,
            platform = resolvedPlatform,
            versionName = parsed?.versionName,
            versionId = parsed?.versionId,
            sizeBytes = dirSize,
            iconBitmap = iconBitmap,
            worldDir = worldDir,
            gameType = parsed?.gameType
        )
    }

    /**
     * Detects whether a world directory is Bedrock or Java based on file structure.
     */
    fun detectPlatform(worldDir: File): Platform {
        val dbDir = File(worldDir, "db")
        val regionDir = File(worldDir, "region")
        val dimensionsDir = File(worldDir, "dimensions")

        if (dbDir.exists() && dbDir.isDirectory) {
            return Platform.BEDROCK
        }
        if ((regionDir.exists() && regionDir.isDirectory) || (dimensionsDir.exists() && dimensionsDir.isDirectory)) {
            return Platform.JAVA
        }
        return Platform.BEDROCK
    }

    fun readLevelNameTxt(worldDir: File): String? {
        val txtFile = File(worldDir, "levelname.txt")
        return if (txtFile.exists() && txtFile.isFile) {
            try {
                txtFile.readText().trim().ifEmpty { null }
            } catch (_: Exception) {
                null
            }
        } else null
    }

    fun loadWorldIcon(worldDir: File): Bitmap? {
        val candidateNames = listOf("world_icon.jpeg", "world_icon.jpg", "icon.png")
        for (name in candidateNames) {
            val iconFile = File(worldDir, name)
            if (iconFile.exists() && iconFile.isFile) {
                try {
                    return BitmapFactory.decodeFile(iconFile.absolutePath)
                } catch (_: Exception) {}
            }
        }
        return null
    }

    fun readLevelDat(levelDatFile: File): ParsedMetadata? {
        return try {
            levelDatFile.inputStream().use { stream ->
                readLevelDatStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun readLevelDatStream(inputStream: InputStream): ParsedMetadata? {
        val bytes = inputStream.readBytes()
        val attempts = listOf(
            Triple(true, false, false),  // Java (GZIP)
            Triple(false, true, true),   // Bedrock (Header + LE)
            Triple(false, true, false),  // Bedrock (LE without header)
            Triple(true, false, true),
            Triple(true, true, false),
            Triple(true, true, true),
            Triple(false, false, false),
            Triple(false, false, true)
        )

        for ((compressed, littleEndian, readHeaders) in attempts) {
            try {
                val nbtFile = NbtIO.readNbtFile(bytes.inputStream(), compressed, littleEndian, readHeaders)
                val metadata = parseRootTag(nbtFile.tag as? NbtCompound)
                if (metadata != null) return metadata
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    private fun parseRootTag(root: NbtCompound?): ParsedMetadata? {
        if (root == null) return null

        // 1. Check for Java Edition
        val javaData = root.getNullableCompound("Data")
        if (javaData != null) {
            val javaVersion = javaData.getNullableCompound("Version")
            val worldName = javaData.getNullableString("LevelName")
            val gameTypeInt = javaData.getNullableInt("GameType")
            val gameType = mapGameType(gameTypeInt)

            if (javaVersion != null) {
                val vName = javaVersion.getNullableString("Name")
                val vId = javaVersion.getNullableInt("Id")
                return ParsedMetadata(worldName, vName, vId, Platform.JAVA, gameType)
            }
            if (worldName != null) {
                return ParsedMetadata(worldName, null, null, Platform.JAVA, gameType)
            }
        }

        // 2. Check for Bedrock Edition
        val bedrockWorldName = root.getNullableString("LevelName")
        val gameType = mapGameType(root.getNullableInt("GameType"))

        val lastOpenedObj = root["lastOpenedWithVersion"]
        if (lastOpenedObj is NbtList<*>) {
            val parts = lastOpenedObj.mapNotNull { (it as? NbtInt)?.value }
            if (parts.size >= 2) {
                val vString = "${parts[0]}.${parts[1]}" + (if (parts.size >= 3 && parts[2] > 0) ".${parts[2]}" else "")
                return ParsedMetadata(bedrockWorldName, vString, null, Platform.BEDROCK, gameType)
            }
        }

        val lastOpenedStr = root.getNullableString("lastOpenedWithVersion")
        if (!lastOpenedStr.isNullOrBlank()) {
            return ParsedMetadata(bedrockWorldName, lastOpenedStr, null, Platform.BEDROCK, gameType)
        }

        val worldVersionObj = root["WorldVersion"]
        if (worldVersionObj is NbtInt && worldVersionObj.value > 100) {
            val vName = mapBedrockVersionId(worldVersionObj.value)
            return ParsedMetadata(bedrockWorldName, vName, worldVersionObj.value, Platform.BEDROCK, gameType)
        }

        val worldVersionCompound = root.getNullableCompound("WorldVersion")
        if (worldVersionCompound != null) {
            val vName = worldVersionCompound.getNullableString("Name")
            val vId = worldVersionCompound.getNullableInt("Id")
            return ParsedMetadata(bedrockWorldName, vName, vId, Platform.BEDROCK, gameType)
        }

        if (bedrockWorldName != null) {
            return ParsedMetadata(bedrockWorldName, null, null, Platform.BEDROCK, gameType)
        }

        return null
    }

    private fun mapGameType(type: Int?): String? {
        return when (type) {
            0 -> "生存模式"
            1 -> "创造模式"
            2 -> "冒险模式"
            3 -> "旁观模式"
            else -> null
        }
    }

    private fun mapBedrockVersionId(versionId: Int): String {
        return when (versionId) {
            1134 -> "1.20.0"
            1143 -> "1.20.10"
            1158 -> "1.20.30"
            1189 -> "1.20.50"
            1210 -> "1.20.70"
            1220 -> "1.20.80"
            1230 -> "1.20.90"
            1240 -> "1.21.0"
            1250 -> "1.21.10"
            1260 -> "1.21.20"
            1270 -> "1.21.30"
            1280 -> "1.21.40"
            1290 -> "1.21.50"
            else -> "1.$versionId"
        }
    }

    fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        var total = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) total += file.length()
        }
        return total
    }
}
