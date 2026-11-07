package com.noches.chunkoidng.core.entity

import android.util.Log
import br.com.gamemods.nbtmanipulator.AnvilRegionFile
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtIO
import br.com.gamemods.nbtmanipulator.NbtList
import org.iq80.leveldb.CompressionType
import org.iq80.leveldb.DB
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern

class EntityConversionPipeline(
    private val onLog: ((String) -> Unit)? = null
) {
    private val TAG = "EntityConversionPipeline"
    private val REGION_FILE_PATTERN = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca")

    private val ACTOR_PREFIX = "actorprefix".toByteArray(StandardCharsets.UTF_8)
    private val DIGP_PREFIX = "digp".toByteArray(StandardCharsets.UTF_8)

    data class ActorRecord(
        val uniqueId: Long,
        val storageKey: ByteArray,
        val nbtBytes: ByteArray
    )

    data class ChunkKey(
        val dimensionId: Int,
        val chunkX: Int,
        val chunkZ: Int
    )


    internal val stagedChunks = ConcurrentHashMap<ChunkKey, MutableList<ActorRecord>>()
    private val totalExtracted = AtomicInteger(0)
    private val skippedModdedCount = AtomicInteger(0)
    private val fallbackIdCounter = AtomicLong(0x1000L)

    fun preExtract(inputDir: File, dimensions: List<String> = listOf("overworld", "the_nether", "the_end")) {
        stagedChunks.clear()
        totalExtracted.set(0)
        skippedModdedCount.set(0)

        log("开始预提取 Java 存档实体...")

        val tasks = collectTasks(inputDir, dimensions)
        if (tasks.isEmpty()) {
            log("未发现任何包含实体的 region/entities 文件，跳过实体提取。")
            return
        }

        log("发现 ${tasks.size} 个区域文件，开始解析实体数据...")

        for (task in tasks) {
            try {
                processRegionFile(task)
            } catch (e: Exception) {
                log("解析区域文件失败 [${task.file.name}]: ${e.message}")
            }
        }

        log("实体预提取完成：成功构建 ${totalExtracted.get()} 个实体，跳过 ${skippedModdedCount.get()} 个未知/模组实体。")
    }

    fun flushToLevelDb(bedrockWorldDir: File): PipelineStats {
        val total = totalExtracted.get()
        if (total == 0 || stagedChunks.isEmpty()) {
            log("无待写入的实体记录，跳过 LevelDB 写入。")
            return PipelineStats(0, skippedModdedCount.get(), 0)
        }

        val dbDir = File(bedrockWorldDir, "db")
        if (!dbDir.isDirectory) {
            log("目标目录未发现有效的 LevelDB (db/)，无法写入实体数据。")
            return PipelineStats(0, skippedModdedCount.get(), 0)
        }

        log("正在向基岩版 LevelDB 写入 $total 个实体与区块索引...")

        val options = Options()
        options.compressionType(CompressionType.ZLIB_RAW)
        options.createIfMissing(false)

        var writtenActors = 0
        var writtenChunks = 0

        var db: DB? = null
        try {
            db = Iq80DBFactory.factory.open(dbDir, options)
            val batch = db.createWriteBatch()
            try {
                for ((chunkKey, records) in stagedChunks) {
                    if (records.isEmpty()) continue

                    val digpStream = ByteArrayOutputStream(records.size * 8)
                    for (record in records) {
                        val actorKey = ACTOR_PREFIX + record.storageKey
                        batch.put(actorKey, record.nbtBytes)
                        digpStream.write(record.storageKey)
                        writtenActors++
                    }

                    val digpKey = buildDigpKey(chunkKey.dimensionId, chunkKey.chunkX, chunkKey.chunkZ)
                    batch.put(digpKey, digpStream.toByteArray())
                    writtenChunks++
                }

                db.write(batch)
            } finally {
                batch.close()
            }
        } catch (e: Exception) {
            log("写入 LevelDB 发生异常: ${e.message}")
            Log.e(TAG, "LevelDB write error", e)
        } finally {
            try {
                db?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing LevelDB", e)
            }
            stagedChunks.clear()
        }

        log("LevelDB 写入完成：成功持久化 $writtenActors 个实体，关联 $writtenChunks 个区块。")
        return PipelineStats(writtenActors, skippedModdedCount.get(), writtenChunks)
    }

    private data class RegionTask(val dimensionId: Int, val file: File)

    private fun collectTasks(inputDir: File, dimensions: List<String>): List<RegionTask> {
        val tasks = mutableListOf<RegionTask>()

        if ("overworld" in dimensions) {
            addTasksForCandidateDirs(tasks, 0, inputDir, "dimensions/minecraft/overworld", "")
        }

        if ("the_nether" in dimensions) {
            addTasksForCandidateDirs(tasks, 1, inputDir, "dimensions/minecraft/the_nether", "DIM-1")
        }

        if ("the_end" in dimensions) {
            addTasksForCandidateDirs(tasks, 2, inputDir, "dimensions/minecraft/the_end", "DIM1")
        }

        return tasks
    }

    private fun addTasksForCandidateDirs(
        tasks: MutableList<RegionTask>,
        dimId: Int,
        baseDir: File,
        vararg relativePaths: String
    ) {
        for (rel in relativePaths) {
            val folder = if (rel.isEmpty()) baseDir else File(baseDir, rel)
            val entitiesDir = File(folder, "entities")
            val targetDir = if (entitiesDir.isDirectory) entitiesDir else File(folder, "region")
            if (!targetDir.isDirectory) continue

            val files = targetDir.listFiles { _, name -> REGION_FILE_PATTERN.matcher(name).matches() }
            if (files != null && files.isNotEmpty()) {
                for (f in files) {
                    tasks.add(RegionTask(dimId, f))
                }
                return
            }
        }
    }

    private fun processRegionFile(task: RegionTask) {
        val matcher = REGION_FILE_PATTERN.matcher(task.file.name)
        if (!matcher.matches()) return

        val regionX = matcher.group(1)?.toIntOrNull() ?: return
        val regionZ = matcher.group(2)?.toIntOrNull() ?: return

        AnvilRegionFile(task.file).use { regionFile ->
            val chunks = regionFile.getPopulatedChunks()
            for (chunkInfo in chunks) {
                val chunkNbt = try {
                    regionFile.readChunkNbt(chunkInfo.localX, chunkInfo.localZ)
                } catch (e: Exception) {
                    null
                } ?: continue

                val rootTag = chunkNbt.compound

                val entitiesList = rootTag.getNullableCompoundList("Entities")
                    ?: rootTag.getNullableCompound("Level")?.getNullableCompoundList("Entities")

                if (entitiesList.isNullOrEmpty()) continue

                val chunkGlobalX = chunkInfo.getGlobalX(regionX)
                val chunkGlobalZ = chunkInfo.getGlobalZ(regionZ)
                val chunkKey = ChunkKey(task.dimensionId, chunkGlobalX, chunkGlobalZ)

                for (javaEntity in entitiesList) {
                    processEntity(chunkKey, javaEntity, vehicleId = 0L)
                }
            }
        }
    }

    internal fun processEntity(chunkKey: ChunkKey, javaEntity: NbtCompound, vehicleId: Long) {
        val rawId = javaEntity.getNullableString("id")
        if (EntityIdMapper.isHandledByChunkerNative(rawId)) {
            return
        }
        if (!EntityIdMapper.isVanilla(rawId)) {
            skippedModdedCount.incrementAndGet()
            return
        }

        val uuidArray = javaEntity.getNullableIntArray("UUID")
        val uniqueId = if (uuidArray != null) {
            EntityLinks.uniqueIdFromUuid(uuidArray)
        } else {
            fallbackIdCounter.getAndIncrement() or (0xFFFFFFFFL shl 32)
        }

        val actor = try {
            BedrockActorBuilder.build(javaEntity, uniqueId)
        } catch (e: Exception) {
            log("构建实体异常 [$rawId]: ${e.message}")
            null
        } ?: return

        val passengers = javaEntity.getNullableCompoundList("Passengers")
        if (vehicleId != 0L) {
            EntityLinks.riding(actor, riderId = uniqueId, vehicleId = vehicleId)
        }
        if (!passengers.isNullOrEmpty()) {
            val passengerIds = passengers.mapNotNull { p ->
                val pUuid = p.getNullableIntArray("UUID")
                if (pUuid != null) EntityLinks.uniqueIdFromUuid(pUuid) else null
            }
            EntityLinks.carried(actor, vehicleId = uniqueId, riderIds = passengerIds)
        }

        val leashHolderId = EntityLinks.leashHolder(javaEntity)
        if (leashHolderId != 0L) {
            EntityLinks.leashed(actor, leashHolderId)
        }

        val baos = ByteArrayOutputStream(1024)
        NbtIO.writeNbtFile(baos, NbtFile("", actor), compressed = false, littleEndian = true)
        val nbtBytes = baos.toByteArray()

        val storageKey = EntityLinks.storageKey(uniqueId)
        val record = ActorRecord(uniqueId, storageKey, nbtBytes)

        stagedChunks.computeIfAbsent(chunkKey) { mutableListOf() }.add(record)
        totalExtracted.incrementAndGet()

        if (!passengers.isNullOrEmpty()) {
            for (passenger in passengers) {
                processEntity(chunkKey, passenger, vehicleId = uniqueId)
            }
        }
    }

    fun buildDigpKey(dimensionId: Int, chunkX: Int, chunkZ: Int): ByteArray {
        val hasDim = dimensionId != 0
        val buffer = ByteBuffer.allocate(DIGP_PREFIX.size + 4 + 4 + (if (hasDim) 4 else 0))
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(DIGP_PREFIX)
        buffer.putInt(chunkX)
        buffer.putInt(chunkZ)
        if (hasDim) {
            buffer.putInt(dimensionId)
        }
        return buffer.array()
    }

    private fun log(message: String) {
        onLog?.invoke(message)
        Log.i(TAG, message)
    }
}
