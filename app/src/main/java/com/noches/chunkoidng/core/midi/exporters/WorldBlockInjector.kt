package com.noches.chunkoidng.core.midi.exporters

import com.noches.chunkoidng.core.midi.NoteBlockSong
import com.noches.chunkoidng.core.world.Platform
import com.noches.chunkoidng.core.world.WorldMetadataReader
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class WorldInjectionConfig(
    val targetX: Int = 0,
    val targetY: Int = 64,
    val targetZ: Int = 0,
    val autoLoadOnJoin: Boolean = true,
    val createBackup: Boolean = true
)

data class WorldInjectionResult(
    val success: Boolean,
    val worldName: String,
    val platform: Platform,
    val backupFile: File?,
    val structureFile: File?,
    val inGameCommand: String,
    val message: String
)

object WorldBlockInjector {

    private const val MAX_WORLD_BACKUP_BYTES = 2L * 1024L * 1024L * 1024L

    fun injectSongIntoWorld(
        worldDir: File,
        song: NoteBlockSong,
        config: WorldInjectionConfig = WorldInjectionConfig()
    ): WorldInjectionResult {
        if (!worldDir.exists() || !worldDir.isDirectory) {
            return WorldInjectionResult(
                success = false,
                worldName = worldDir.name,
                platform = Platform.BEDROCK,
                backupFile = null,
                structureFile = null,
                inGameCommand = "",
                message = "Target world directory does not exist: ${worldDir.absolutePath}"
            )
        }
        require(worldDir.canonicalFile.parentFile != null) { "Invalid target world directory" }

        val worldInfo = WorldMetadataReader.inspectWorld(worldDir)
        val platform = worldInfo.platform

        var backupFile: File? = null
        if (config.createBackup) {
            backupFile = createWorldBackup(worldDir, worldInfo.name)
        }

        val sanitizedTitle = song.title.ifBlank { "music" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .lowercase()
            .take(64)
            .ifBlank { "music" }

        return if (platform == Platform.BEDROCK) {
            injectBedrockWorld(worldDir, worldInfo.name, song, sanitizedTitle, config, backupFile)
        } else {
            injectJavaWorld(worldDir, worldInfo.name, song, sanitizedTitle, config, backupFile)
        }
    }

    private fun injectBedrockWorld(
        worldDir: File,
        worldName: String,
        song: NoteBlockSong,
        slug: String,
        config: WorldInjectionConfig,
        backupFile: File?
    ): WorldInjectionResult {
        val structuresDir = File(worldDir, "structures").apply { mkdirs() }
        val mcstructureFile = File(structuresDir, "$slug.mcstructure")
        StructureExporter.exportBedrockMcStructure(song, mcstructureFile)

        if (config.autoLoadOnJoin) {
            val bpDir = File(worldDir, "behavior_packs/music_$slug").apply { mkdirs() }
            val packUuid = UUID.nameUUIDFromBytes("chunkoidng:music:$slug".toByteArray()).toString()
            val moduleUuid = UUID.nameUUIDFromBytes("chunkoidng:music:$slug:module".toByteArray()).toString()

            val bpManifest = File(bpDir, "manifest.json")
            bpManifest.writeText(
                """
                {
                  "format_version": 2,
                  "header": {
                    "description": "ChunkoidNG Redstone Music Injected Loader",
                    "name": "Redstone Music - $slug",
                    "uuid": "$packUuid",
                    "version": [1, 0, 0],
                    "min_engine_version": [1, 16, 0]
                  },
                  "modules": [
                    {
                      "description": "Script / Data Module",
                      "type": "data",
                      "uuid": "$moduleUuid",
                      "version": [1, 0, 0]
                    }
                  ]
                }
                """.trimIndent()
            )

            val functionsDir = File(bpDir, "functions").apply { mkdirs() }
            File(functionsDir, "tick.json").writeText(
                """
                {
                  "values": [
                    "load_$slug"
                  ]
                }
                """.trimIndent()
            )

            File(functionsDir, "load_$slug.mcfunction").writeText(
                """
                execute as @a[tag=!music_${slug}_inited] at @s run structure load "$slug" ${config.targetX} ${config.targetY} ${config.targetZ}
                execute as @a[tag=!music_${slug}_inited] at @s run tellraw @s {"rawtext":[{"text":"§a♪ Redstone music '$slug' injected at ${config.targetX}, ${config.targetY}, ${config.targetZ}! ♪"}]}
                tag @a[tag=!music_${slug}_inited] add music_${slug}_inited
                """.trimIndent()
            )

            val worldBpJsonFile = File(worldDir, "world_behavior_packs.json")
            val bpArray = if (worldBpJsonFile.exists()) {
                try {
                    JSONArray(worldBpJsonFile.readText())
                } catch (cause: Exception) {
                    throw IllegalArgumentException("Existing world_behavior_packs.json is invalid", cause)
                }
            } else {
                JSONArray()
            }

            var alreadyRegistered = false
            for (i in 0 until bpArray.length()) {
                val obj = bpArray.optJSONObject(i)
                if (obj?.optString("pack_id").equals(packUuid, ignoreCase = true)) {
                    alreadyRegistered = true
                    break
                }
            }

            if (!alreadyRegistered) {
                val newEntry = JSONObject().apply {
                    put("pack_id", packUuid)
                    put("version", JSONArray(listOf(1, 0, 0)))
                }
                bpArray.put(newEntry)
                worldBpJsonFile.writeText(bpArray.toString(2))
            }
        }

        val inGameCmd = "/structure load \"$slug\" ${config.targetX} ${config.targetY} ${config.targetZ}"

        return WorldInjectionResult(
            success = true,
            worldName = worldName,
            platform = Platform.BEDROCK,
            backupFile = backupFile,
            structureFile = mcstructureFile,
            inGameCommand = inGameCmd,
            message = "Successfully injected redstone music structure into Bedrock world."
        )
    }

    private fun injectJavaWorld(
        worldDir: File,
        worldName: String,
        song: NoteBlockSong,
        slug: String,
        config: WorldInjectionConfig,
        backupFile: File?
    ): WorldInjectionResult {
        val schemDir = File(worldDir, "schematics").apply { mkdirs() }
        val schemFile = File(schemDir, "$slug.schem")
        StructureExporter.exportJavaSchematic(song, schemFile)

        val dpDir = File(worldDir, "datapacks/music_$slug").apply { mkdirs() }
        McFunctionMusicExporter.exportScheduledDatapack(
            song = song,
            namespace = "music_$slug",
            outputDir = dpDir
        )

        val inGameCmd = "/function music_$slug:play or use WorldEdit //schem load $slug"

        return WorldInjectionResult(
            success = true,
            worldName = worldName,
            platform = Platform.JAVA,
            backupFile = backupFile,
            structureFile = schemFile,
            inGameCommand = inGameCmd,
            message = "Successfully injected redstone music schematic and datapack into Java world."
        )
    }

    private fun createWorldBackup(worldDir: File, worldName: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sanitized = worldName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val backupDir = File(worldDir.parentFile ?: worldDir, "backups").apply { mkdirs() }
        val backupZip = File(backupDir, "${sanitized}_backup_$timeStamp.zip")

        var totalBytes = 0L
        ZipOutputStream(FileOutputStream(backupZip).buffered()).use { zos ->
            val prefixLen = worldDir.absolutePath.length + 1
            worldDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    totalBytes += file.length()
                    require(totalBytes <= MAX_WORLD_BACKUP_BYTES) { "World is too large to back up" }
                    val entryName = file.absolutePath.substring(prefixLen).replace('\\', '/')
                    zos.putNextEntry(ZipEntry(entryName))
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
        return backupZip
    }
}
