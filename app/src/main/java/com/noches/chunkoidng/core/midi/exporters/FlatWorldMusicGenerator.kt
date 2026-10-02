package com.noches.chunkoidng.core.midi.exporters

import br.com.gamemods.nbtmanipulator.*
import com.noches.chunkoidng.core.midi.NoteBlockSong
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FlatWorldMusicGenerator {

    fun generateFlatWorld(
        song: NoteBlockSong,
        outputMcworldFile: File,
        onProgress: ((String) -> Unit)? = null
    ) {
        val tempDir = File.createTempFile("chunkoid_flat_", "").apply {
            delete()
            mkdirs()
        }

        try {
            onProgress?.invoke("正在写入 levelname.txt 与 level.dat 世界元数据...")
            File(tempDir, "levelname.txt").writeText("Redstone Music - ${song.title.ifBlank { "Untitled" }}")

            val levelCompound = NbtCompound().apply {
                this["LevelName"] = NbtString(song.title.ifBlank { "Redstone Music" })
                this["StorageVersion"] = NbtInt(10)
                this["NetworkVersion"] = NbtInt(0)
                this["Platform"] = NbtInt(2)
                this["GameType"] = NbtInt(1)
                this["Generator"] = NbtInt(2)
                this["SpawnX"] = NbtInt(0)
                this["SpawnY"] = NbtInt(5)
                this["SpawnZ"] = NbtInt(-3)
                this["Time"] = NbtLong(6000L)
                this["DayCycleStopTime"] = NbtInt(6000)
                this["commandsEnabled"] = NbtByte(1)
                this["cheatsEnabled"] = NbtByte(1)
                this["FlatWorldLayers"] = NbtString(
                    """{"biome_id":1,"block_layers":[{"block_name":"minecraft:bedrock","count":1},{"block_name":"minecraft:dirt","count":2},{"block_name":"minecraft:grass_block","count":1}],"encoding_version":6,"structure_options":null}"""
                )
                this["RandomSeed"] = NbtLong(12345678L)
            }

            val levelDatFile = File(tempDir, "level.dat")
            val nbtFile = NbtFile("", levelCompound).apply {
                version = 10
            }
            NbtIO.writeNbtFile(levelDatFile, nbtFile, compressed = false, littleEndian = true, writeHeaders = true)

            onProgress?.invoke("正在生成基岩版物理结构文件 (.mcstructure)...")
            val structuresDir = File(tempDir, "structures").apply { mkdirs() }
            val mcstructureFile = File(structuresDir, "music.mcstructure")
            StructureExporter.exportBedrockMcStructure(song, mcstructureFile)

            onProgress?.invoke("正在配置行为包与自动启动函数...")
            val packUuid = UUID.randomUUID().toString()
            val moduleUuid = UUID.randomUUID().toString()

            val bpDir = File(tempDir, "behavior_packs/music_loader").apply { mkdirs() }
            val bpManifest = File(bpDir, "manifest.json")
            bpManifest.writeText(
                """
                {
                  "format_version": 2,
                  "header": {
                    "description": "ChunkoidNG Redstone Music Loader",
                    "name": "Redstone Music Loader",
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
                    "music_init"
                  ]
                }
                """.trimIndent()
            )

            File(functionsDir, "music_init.mcfunction").writeText(
                """
                execute as @a[tag=!music_inited] at @s run structure load "music" 0 4 0
                execute as @a[tag=!music_inited] at @s run tp @s 0 5 -3 0 0
                execute as @a[tag=!music_inited] at @s run tellraw @s {"rawtext":[{"text":"§a♪ Redstone Music Loaded! Press the button ahead to play ♪"}]}
                tag @a[tag=!music_inited] add music_inited
                """.trimIndent()
            )

            File(tempDir, "world_behavior_packs.json").writeText(
                """
                [
                  {
                    "pack_id": "$packUuid",
                    "version": [1, 0, 0]
                  }
                ]
                """.trimIndent()
            )

            onProgress?.invoke("正在压缩打包为 .mcworld 即听世界文件...")
            outputMcworldFile.parentFile?.mkdirs()
            zipDirectory(tempDir, outputMcworldFile)
            onProgress?.invoke("即听世界打包完成！")

        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun zipDirectory(sourceDir: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile).buffered()).use { zos ->
            val prefixLen = sourceDir.absolutePath.length + 1
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.absolutePath.substring(prefixLen).replace('\\', '/')
                    zos.putNextEntry(ZipEntry(entryName))
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
    }
}
