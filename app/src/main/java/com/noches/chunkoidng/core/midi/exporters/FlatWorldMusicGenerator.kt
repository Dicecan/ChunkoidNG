package com.noches.chunkoidng.core.midi.exporters

import br.com.gamemods.nbtmanipulator.*
import com.noches.chunkoidng.core.leveldb.BedrockBlockState
import com.noches.chunkoidng.core.leveldb.BedrockChunkHelper
import com.noches.chunkoidng.core.leveldb.SafeEnv
import com.noches.chunkoidng.core.leveldb.SubChunkEncoder
import com.noches.chunkoidng.core.midi.NoteBlockSong
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.DbImpl
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FlatWorldMusicGenerator {

    private val MINIMAL_JPEG = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xDB.toByte(), 0x00, 0x43, 0x00, 0x08,
        0x06, 0x06, 0x07, 0x06, 0x05, 0x08, 0x07, 0x07, 0x07, 0x09, 0x09, 0x08,
        0x0A, 0x0C, 0x14, 0x0D, 0x0C, 0x0B, 0x0B, 0x0C, 0x19, 0x12, 0x13, 0x0F,
        0x14, 0x1D, 0x1A, 0x1F, 0x1E, 0x1D, 0x1A, 0x1C, 0x1C, 0x20, 0x24, 0x2E,
        0x27, 0x20, 0x22, 0x2C, 0x23, 0x1C, 0x1C, 0x28, 0x37, 0x29, 0x2C, 0x30,
        0x31, 0x34, 0x34, 0x34, 0x1F, 0x27, 0x39, 0x3D, 0x38, 0x32, 0x3C, 0x2E,
        0x33, 0x34, 0x32, 0xFF.toByte(), 0xC0.toByte(), 0x00, 0x0B, 0x08, 0x00, 0x01, 0x00,
        0x01, 0x01, 0x01, 0x11, 0x00, 0xFF.toByte(), 0xC4.toByte(), 0x00, 0x1F, 0x00, 0x00,
        0x01, 0x05, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
        0x09, 0x0A, 0x0B, 0xFF.toByte(), 0xDA.toByte(), 0x00, 0x08, 0x01, 0x01, 0x00, 0x00,
        0x3F, 0x00, 0xBF.toByte(), 0x00, 0xFF.toByte(), 0xD9.toByte()
    )

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
            onProgress?.invoke("正在构建红石音乐实体结构布局...")
            val layout = StructureExporter.buildContraption(song)

            onProgress?.invoke("正在写入 levelname.txt 与 level.dat 超平坦元数据...")
            val cleanTitle = song.title.trim { it <= ' ' || it == '\u0000' }.ifBlank { "Redstone Music" }
            File(tempDir, "levelname.txt").writeText("Redstone Music - $cleanTitle")
            File(tempDir, "world_icon.jpeg").writeBytes(MINIMAL_JPEG)

            val flatLayersJson = """{"biome_id":1,"block_layers":[{"block_name":"minecraft:bedrock","count":1},{"block_name":"minecraft:dirt","count":2},{"block_name":"minecraft:grass_block","count":1}],"encoding_version":6,"structure_options":null,"world_version":"version.post_1_18"}"""

            val levelCompound = NbtCompound().apply {
                this["LevelName"] = NbtString(cleanTitle)
                this["StorageVersion"] = NbtInt(10)
                this["NetworkVersion"] = NbtInt(0)
                this["Platform"] = NbtInt(2)
                this["PlatformBroadcastIntent"] = NbtInt(3)
                this["GameType"] = NbtInt(1)
                this["Difficulty"] = NbtInt(1)
                this["Generator"] = NbtInt(2)
                this["SpawnX"] = NbtInt(layout.buttonPos.first)
                this["SpawnY"] = NbtInt(-60)
                this["SpawnZ"] = NbtInt(layout.buttonPos.third - 3)
                this["RandomSeed"] = NbtLong(123456789L)
                this["Time"] = NbtLong(1000L)
                this["DayCycleStopTime"] = NbtLong(-1L)
                this["LastPlayed"] = NbtLong(System.currentTimeMillis() / 1000L)
                this["commandsEnabled"] = NbtByte(1)
                this["cheatsEnabled"] = NbtByte(1)
                this["hasBeenLoadedInCreative"] = NbtByte(1)
                this["immutableWorld"] = NbtByte(0)
                this["MultiplayerGame"] = NbtByte(1)
                this["LANBroadcast"] = NbtByte(1)
                this["XBLBroadcastIntent"] = NbtInt(3)
                this["FlatWorldLayers"] = NbtString(flatLayersJson)
                this["experiments"] = NbtCompound().apply {
                    this["experiments_ever_used"] = NbtByte(0)
                    this["saved_with_toggled_experiments"] = NbtByte(0)
                }
            }

            val levelDatFile = File(tempDir, "level.dat")
            val nbtFile = NbtFile("", levelCompound).apply {
                version = 10
            }
            NbtIO.writeNbtFile(levelDatFile, nbtFile, compressed = false, littleEndian = true, writeHeaders = true)
            File(tempDir, "level.dat_old").writeBytes(levelDatFile.readBytes())

            onProgress?.invoke("正在写入 LevelDB 世界区块与实体红石音乐方块...")
            val dbDir = File(tempDir, "db").apply { mkdirs() }
            val dbOptions = Options().apply { createIfMissing(true) }
            val db = DbImpl(dbOptions, dbDir.absolutePath, SafeEnv())

            try {
                val minChunkX = minOf(0, ((layout.blocks.minOfOrNull { it.x } ?: 0) shr 4) - 1)
                val maxChunkX = maxOf(0, ((layout.blocks.maxOfOrNull { it.x } ?: 0) shr 4) + 1)
                val minChunkZ = minOf(-1, ((layout.blocks.minOfOrNull { it.z } ?: 0) shr 4) - 1)
                val maxChunkZ = maxOf(0, ((layout.blocks.maxOfOrNull { it.z } ?: 0) shr 4) + 1)

                for (cx in minChunkX..maxChunkX) {
                    for (cz in minChunkZ..maxChunkZ) {
                        val vKey = BedrockChunkHelper.createChunkVersionKey(cx, cz)
                        db.put(vKey, byteArrayOf(BedrockChunkHelper.OVERWORLD_VERSION_VALUE))

                        val d2Key = BedrockChunkHelper.create2DDataKey(cx, cz)
                        db.put(d2Key, BedrockChunkHelper.createDefault2DData(-60))

                        val subBlocks = Array(SubChunkEncoder.SUBCHUNK_SIZE) { BedrockBlockState.AIR }

                        for (lx in 0..15) {
                            for (lz in 0..15) {
                                subBlocks[SubChunkEncoder.getLocalIndex(lx, 0, lz)] = BedrockBlockState.BEDROCK
                                subBlocks[SubChunkEncoder.getLocalIndex(lx, 1, lz)] = BedrockBlockState.DIRT
                                subBlocks[SubChunkEncoder.getLocalIndex(lx, 2, lz)] = BedrockBlockState.DIRT
                                subBlocks[SubChunkEncoder.getLocalIndex(lx, 3, lz)] = BedrockBlockState.GRASS_BLOCK
                            }
                        }

                        val blockEntities = mutableListOf<NbtCompound>()

                        for (block in layout.blocks) {
                            val worldX = block.x
                            val worldY = -61 + block.y
                            val worldZ = block.z

                            if ((worldX shr 4) == cx && (worldZ shr 4) == cz) {
                                val lx = worldX and 15
                                val lz = worldZ and 15
                                val ly = worldY + 64

                                if (ly in 0..15) {
                                    val effectiveStates = if (block.bedrockName == "minecraft:dirt" && block.bedrockStates.isEmpty()) {
                                        mapOf("dirt_type" to "normal")
                                    } else {
                                        block.bedrockStates
                                    }
                                    subBlocks[SubChunkEncoder.getLocalIndex(lx, ly, lz)] = BedrockBlockState(
                                        name = block.bedrockName,
                                        states = effectiveStates
                                    )
                                }

                                if (block.notePitch != null) {
                                    val entity = NbtCompound().apply {
                                        this["id"] = NbtString("Music")
                                        this["note"] = NbtByte(block.notePitch.toByte())
                                        this["x"] = NbtInt(worldX)
                                        this["y"] = NbtInt(worldY)
                                        this["z"] = NbtInt(worldZ)
                                        this["isMovable"] = NbtByte(1)
                                    }
                                    blockEntities.add(entity)
                                }
                            }
                        }

                        val subKey = BedrockChunkHelper.createSubChunkKey(cx, cz, -4)
                        val encodedSub = SubChunkEncoder.encode(subBlocks)
                        db.put(subKey, encodedSub)

                        if (blockEntities.isNotEmpty()) {
                            val beKey = BedrockChunkHelper.createBlockEntityKey(cx, cz)
                            db.put(beKey, BedrockChunkHelper.serializeBlockEntities(blockEntities))
                        }
                    }
                }
            } finally {
                db.close()
            }

            onProgress?.invoke("正在导出物理结构备份 (.mcstructure)...")
            val structuresDir = File(tempDir, "structures").apply { mkdirs() }
            val mcstructureFile = File(structuresDir, "music.mcstructure")
            StructureExporter.exportBedrockMcStructure(song, mcstructureFile)

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
