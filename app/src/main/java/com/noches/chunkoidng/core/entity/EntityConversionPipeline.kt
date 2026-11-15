package com.noches.chunkoidng.core.entity

import android.util.Log
import br.com.gamemods.nbtmanipulator.AnvilRegionFile
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtIO
import com.noches.chunkoidng.core.leveldb.SafeEnv
import org.iq80.leveldb.CompressionType
import org.iq80.leveldb.DB
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.DbImpl
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

    data class PendingKnot(
        val dimensionId: Int,
        val blockX: Int,
        val blockY: Int,
        val blockZ: Int,
        val knotId: Long
    )

    internal val stagedChunks = ConcurrentHashMap<ChunkKey, MutableList<ActorRecord>>()
    internal val pendingKnots = ConcurrentHashMap<ChunkKey, MutableList<PendingKnot>>()
    internal val leashKnots = ConcurrentHashMap<Long, IntArray>()
    private val totalExtracted = AtomicInteger(0)
    private val skippedModdedCount = AtomicInteger(0)
    private val fallbackIdCounter = AtomicLong(0x1000L)

    fun preExtract(inputDir: File, dimensions: List<String> = listOf("overworld", "the_nether", "the_end")) {
        stagedChunks.clear()
        pendingKnots.clear()
        leashKnots.clear()
        totalExtracted.set(0)
        skippedModdedCount.set(0)

        log("开始预提取 Java 存档实体...")

        val tasks = collectTasks(inputDir, dimensions)
        if (tasks.isEmpty()) {
            log("未发现任何包含实体的 region/entities 文件，跳过实体提取。")
            return
        }

        log("预扫描拴绳绳结位置...")
        indexLeashKnots(tasks)

        log("发现 ${tasks.size} 个区域文件，开始解析实体数据...")

        for (task in tasks) {
            try {
                processRegionFile(task)
            } catch (e: Exception) {
                log("解析区域文件失败 [${task.file.name}]: ${e.message}")
            }
        }

        log("实体预提取完成：成功构建 ${totalExtracted.get()} 个实体，记录 ${pendingKnots.values.sumOf { it.size }} 个栅栏绳结，跳过 ${skippedModdedCount.get()} 个未知/模组实体。")
    }

    fun flushToLevelDb(bedrockWorldDir: File): PipelineStats {
        val total = totalExtracted.get()
        val totalKnots = pendingKnots.values.sumOf { it.size }
        if ((total == 0 && totalKnots == 0) || (stagedChunks.isEmpty() && pendingKnots.isEmpty())) {
            log("无待写入的实体记录，跳过 LevelDB 写入。")
            return PipelineStats(0, skippedModdedCount.get(), 0)
        }

        val dbDir = File(bedrockWorldDir, "db")
        if (!dbDir.isDirectory) {
            log("目标目录未发现有效的 LevelDB (db/)，无法写入实体数据。")
            return PipelineStats(0, skippedModdedCount.get(), 0)
        }

        log("正在向基岩版 LevelDB 写入 ${total + totalKnots} 个实体与区块索引...")

        val options = Options()
        options.compressionType(CompressionType.ZLIB_RAW)
        options.createIfMissing(false)

        var writtenActors = 0
        var writtenChunks = 0

        var db: DB? = null
        try {
            db = DbImpl(options, dbDir.absolutePath, SafeEnv())
            val batch = db.createWriteBatch()
            try {
                val allChunkKeys = (stagedChunks.keys + pendingKnots.keys).distinct()
                for (chunkKey in allChunkKeys) {
                    val records = stagedChunks[chunkKey].orEmpty()
                    val knots = pendingKnots[chunkKey].orEmpty()
                    if (records.isEmpty() && knots.isEmpty()) continue

                    val digpKey = buildDigpKey(chunkKey.dimensionId, chunkKey.chunkX, chunkKey.chunkZ)
                    val existingDigp = try { db.get(digpKey) } catch (_: Exception) { null }
                    val digpStream = ByteArrayOutputStream((existingDigp?.size ?: 0) + (records.size + knots.size) * 8)
                    val existingKeys = mutableSetOf<Long>()
                    if (existingDigp != null) {
                        digpStream.write(existingDigp)
                        val buf = ByteBuffer.wrap(existingDigp).order(ByteOrder.BIG_ENDIAN)
                        while (buf.remaining() >= 8) {
                            existingKeys.add(buf.long)
                        }
                    }

                    for (record in records) {
                        val storageKeyLong = ByteBuffer.wrap(record.storageKey).order(ByteOrder.BIG_ENDIAN).long
                        if (!existingKeys.add(storageKeyLong)) continue
                        val actorKey = ACTOR_PREFIX + record.storageKey
                        batch.put(actorKey, record.nbtBytes)
                        digpStream.write(record.storageKey)
                        writtenActors++
                    }

                    for (knot in knots) {
                        val storageKeyBytes = EntityLinks.storageKey(knot.knotId)
                        val storageKeyLong = ByteBuffer.wrap(storageKeyBytes).order(ByteOrder.BIG_ENDIAN).long
                        if (!existingKeys.add(storageKeyLong)) continue

                        val knotActor = EntityLinks.buildKnotActor(knot.knotId, knot.blockX, knot.blockY, knot.blockZ)
                        val baos = ByteArrayOutputStream(512)
                        NbtIO.writeNbtFile(baos, NbtFile("", knotActor), compressed = false, littleEndian = true)
                        val actorKey = ACTOR_PREFIX + storageKeyBytes
                        batch.put(actorKey, baos.toByteArray())
                        digpStream.write(storageKeyBytes)
                        writtenActors++
                    }

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
            pendingKnots.clear()
            leashKnots.clear()
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

    private fun indexLeashKnots(tasks: List<RegionTask>) {
        for (task in tasks) {
            try {
                AnvilRegionFile(task.file).use { regionFile ->
                    val chunks = regionFile.getPopulatedChunks()
                    for (chunkInfo in chunks) {
                        val chunkNbt = try {
                            regionFile.readChunkNbt(chunkInfo.localX, chunkInfo.localZ)
                        } catch (_: Exception) {
                            null
                        } ?: continue

                        val rootTag = chunkNbt.compound
                        val entitiesList = rootTag.getNullableCompoundList("Entities")
                            ?: rootTag.getNullableCompound("Level")?.getNullableCompoundList("Entities")
                            ?: continue

                        for (e in entitiesList) {
                            if (EntityLinks.isLeashKnot(e)) {
                                val id = EntityLinks.uniqueIdFromUuid(e.getNullableIntArray("UUID"))
                                val pos = EntityLinks.position(e)
                                if (id != 0L && pos != null) {
                                    leashKnots[id] = pos
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
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
                    try {
                        processEntity(chunkKey, javaEntity, vehicleId = 0L)
                    } catch (e: Exception) {
                        log("处理单个实体异常: ${e.message}")
                    }
                }
            }
        }
    }

    internal fun processEntity(
        chunkKey: ChunkKey,
        javaEntity: NbtCompound,
        vehicleId: Long,
        preAssignedId: Long = 0L
    ) {
        if (EntityLinks.isLeashKnot(javaEntity)) {
            return
        }

        val rawId = javaEntity.getNullableString("id")
        if (EntityIdMapper.isHandledByChunkerNative(rawId, includeBedrockExclusiveNative = true)) {
            return
        }
        if (!EntityIdMapper.isVanilla(rawId)) {
            skippedModdedCount.incrementAndGet()
            return
        }

        val uniqueId = if (preAssignedId != 0L) {
            preAssignedId
        } else {
            val uuidArray = javaEntity.getNullableIntArray("UUID")
            if (uuidArray != null) {
                EntityLinks.uniqueIdFromUuid(uuidArray)
            } else {
                fallbackIdCounter.getAndIncrement() or (0xFFFFFFFFL shl 32)
            }
        }

        val rawPassengers = javaEntity.getNullableCompoundList("Passengers")
        val resolvedPassengers = rawPassengers?.mapNotNull { p ->
            val pRawId = p.getNullableString("id")
            if (EntityLinks.isLeashKnot(p) ||
                EntityIdMapper.isHandledByChunkerNative(pRawId, includeBedrockExclusiveNative = true) ||
                !EntityIdMapper.isVanilla(pRawId)
            ) {
                if (pRawId != null && !EntityIdMapper.isHandledByChunkerNative(pRawId, includeBedrockExclusiveNative = true) && !EntityIdMapper.isVanilla(pRawId)) {
                    skippedModdedCount.incrementAndGet()
                }
                null
            } else {
                val pUuid = p.getNullableIntArray("UUID")
                val pId = if (pUuid != null) {
                    EntityLinks.uniqueIdFromUuid(pUuid)
                } else {
                    fallbackIdCounter.getAndIncrement() or (0xFFFFFFFFL shl 32)
                }
                Pair(p, pId)
            }
        } ?: emptyList()

        val actor = try {
            BedrockActorBuilder.build(
                javaEntity = javaEntity,
                uniqueId = uniqueId,
                hasPassengers = resolvedPassengers.isNotEmpty(),
                isPassenger = vehicleId != 0L
            )
        } catch (e: Exception) {
            log("构建实体异常 [$rawId]: ${e.message}")
            null
        } ?: return

        val passengerIds = resolvedPassengers.map { it.second }
        EntityLinks.link(actor, uniqueId, vehicleId, passengerIds)

        val leash = EntityLinks.leash(javaEntity) { EntityLinks.uniqueIdFromUuid(it) }
        if (leash != null) {
            val block = when (leash) {
                is EntityLinks.LeashTarget.Block -> intArrayOf(leash.x, leash.y, leash.z)
                is EntityLinks.LeashTarget.Holder -> leashKnots[leash.uniqueId]
            }
            if (block != null) {
                val knotId = EntityLinks.knotId(block[0].toLong(), block[1].toLong(), block[2].toLong())
                EntityLinks.leashed(actor, knotId)
                registerKnot(chunkKey.dimensionId, block[0], block[1], block[2], knotId)
            } else if (leash is EntityLinks.LeashTarget.Holder) {
                EntityLinks.leashed(actor, leash.uniqueId)
            }
        }

        val baos = ByteArrayOutputStream(1024)
        NbtIO.writeNbtFile(baos, NbtFile("", actor), compressed = false, littleEndian = true)
        val nbtBytes = baos.toByteArray()

        val storageKey = EntityLinks.storageKey(uniqueId)
        val record = ActorRecord(uniqueId, storageKey, nbtBytes)

        stagedChunks.computeIfAbsent(chunkKey) { mutableListOf() }.add(record)
        totalExtracted.incrementAndGet()

        for ((passenger, pId) in resolvedPassengers) {
            processEntity(chunkKey, passenger, vehicleId = uniqueId, preAssignedId = pId)
        }
    }

    private fun registerKnot(dimensionId: Int, blockX: Int, blockY: Int, blockZ: Int, knotId: Long) {
        val chunkX = blockX shr 4
        val chunkZ = blockZ shr 4
        val knotChunkKey = ChunkKey(dimensionId, chunkX, chunkZ)
        pendingKnots.computeIfAbsent(knotChunkKey) { mutableListOf() }
            .add(PendingKnot(dimensionId, blockX, blockY, blockZ, knotId))
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
