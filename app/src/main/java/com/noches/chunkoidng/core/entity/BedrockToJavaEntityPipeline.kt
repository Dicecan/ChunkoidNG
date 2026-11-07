package com.noches.chunkoidng.core.entity

import android.util.Log
import br.com.gamemods.nbtmanipulator.*
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class BedrockToJavaEntityPipeline(
    private val onLog: ((String) -> Unit)? = null
) {
    private val TAG = "BedrockToJavaPipeline"
    private val ACTOR_PREFIX = "actorprefix".toByteArray(StandardCharsets.UTF_8)

    data class ChunkKey(
        val dimensionId: Int,
        val chunkX: Int,
        val chunkZ: Int
    )

    data class StagedEntity(
        val uniqueId: Long,
        val vehicleUniqueId: Long,
        val javaCompound: NbtCompound
    )


    internal val stagedChunks = ConcurrentHashMap<ChunkKey, MutableList<StagedEntity>>()
    private val totalExtracted = AtomicInteger(0)
    private val skippedModdedCount = AtomicInteger(0)

    fun scan(bedrockWorldDir: File, enabledDimensions: List<String> = listOf("overworld", "the_nether", "the_end")) {
        stagedChunks.clear()
        totalExtracted.set(0)
        skippedModdedCount.set(0)

        val dbDir = File(bedrockWorldDir, "db")
        if (!dbDir.isDirectory) {
            log("未找到基岩版数据库目录 (db/)，跳过实体扫描。")
            return
        }

        log("开始扫描基岩版 LevelDB 实体数据...")

        val options = Options().createIfMissing(false)
        val factory = Iq80DBFactory.factory

        try {
            factory.open(dbDir, options).use { db ->
                val iterator = db.iterator()
                iterator.seekToFirst()

                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val key = entry.key
                    if (key.size != 19 || !matchesPrefix(key, ACTOR_PREFIX)) continue

                    val value = entry.value
                    if (value.isEmpty()) continue

                    try {
                        val bis = ByteArrayInputStream(value)
                        val nbtFile = NbtIO.readNbtFile(bis, compressed = false, littleEndian = true)
                        val actor = nbtFile.compound

                        processBedrockActor(actor, enabledDimensions)
                    } catch (e: Exception) {
                        log("解析单个基岩版实体异常: ${e.message}")
                    }
                }
                iterator.close()
            }
        } catch (e: Exception) {
            log("读取基岩版 LevelDB 失败: ${e.message}")
        }

        log("基岩版实体扫描完成：已提取 ${totalExtracted.get()} 个实体，跳过 ${skippedModdedCount.get()} 个未知/模组实体。")
    }

    internal fun processBedrockActor(actor: NbtCompound, enabledDimensions: List<String>) {
        val bedrockId = actor.getNullableString("identifier")
        if (EntityIdMapper.isHandledByChunkerNative(bedrockId)) return

        val uniqueId = actor.getNullableLong("UniqueID") ?: 0L

        val dim = getInt(actor, "DimensionId", getInt(actor, "dimension", 0))
        if (!isDimensionEnabled(dim, enabledDimensions)) return

        val posList = actor.getNullableFloatList("Pos")
        val posX = if (posList != null && posList.size > 0) posList[0].value else 0f
        val posZ = if (posList != null && posList.size > 2) posList[2].value else 0f

        val blockX = Math.floor(posX.toDouble()).toInt()
        val blockZ = Math.floor(posZ.toDouble()).toInt()
        val chunkX = blockX shr 4
        val chunkZ = blockZ shr 4

        val javaCompound = JavaEntityBuilder.build(actor)
        if (javaCompound == null) {
            skippedModdedCount.incrementAndGet()
            return
        }

        var vehicleId = 0L
        val links = actor.getNullableCompoundList("LinksTag")
        if (links != null) {
            for (link in links) {
                val rideId = link.getNullableLong("RideID") ?: 0L
                val vId = link.getNullableLong("VehicleID") ?: 0L
                if (rideId == uniqueId && vId != 0L && vId != uniqueId) {
                    vehicleId = vId
                    break
                }
            }
        }

        val chunkKey = ChunkKey(dim, chunkX, chunkZ)
        val entity = StagedEntity(uniqueId, vehicleId, javaCompound)
        stagedChunks.computeIfAbsent(chunkKey) { mutableListOf() }.add(entity)
        totalExtracted.incrementAndGet()
    }

    fun flushToJava(javaWorldDir: File): PipelineStats {
        val total = totalExtracted.get()
        if (total == 0 || stagedChunks.isEmpty()) {
            log("无待写入的 Java 实体记录，跳过写入。")
            return PipelineStats(0, skippedModdedCount.get(), 0)
        }

        log("正在向 Java 版存档写入 $total 个实体与乘骑树...")

        var writtenChunks = 0
        var writtenEntities = 0

        val openRegionFiles = mutableMapOf<File, AnvilRegionFile>()

        try {
            for ((chunkKey, stagedList) in stagedChunks) {
                if (stagedList.isEmpty()) continue

                val rootEntities = reconstructPassengersTree(stagedList)
                if (rootEntities.isEmpty()) continue

                val entitiesDir = getEntitiesDirectory(javaWorldDir, chunkKey.dimensionId)
                if (!entitiesDir.exists()) entitiesDir.mkdirs()

                val regionX = Math.floorDiv(chunkKey.chunkX, 32)
                val regionZ = Math.floorDiv(chunkKey.chunkZ, 32)
                val localX = Math.floorMod(chunkKey.chunkX, 32)
                val localZ = Math.floorMod(chunkKey.chunkZ, 32)

                val mcaFile = File(entitiesDir, "r.$regionX.$regionZ.mca")
                val regionFile = openRegionFiles.computeIfAbsent(mcaFile) { AnvilRegionFile(it) }

                try {

                    val existingFile = regionFile.readChunkNbt(localX, localZ)
                    val chunkRoot = existingFile?.compound ?: NbtCompound().apply {
                        this["Position"] = NbtIntArray(intArrayOf(chunkKey.chunkX, chunkKey.chunkZ))
                        this["DataVersion"] = NbtInt(3953)
                    }

                    val existingList = chunkRoot.getNullableCompoundList("Entities")?.toMutableList() ?: mutableListOf()
                    for (entity in rootEntities) {
                        existingList.add(entity)
                        writtenEntities++
                    }

                    val mergedList = NbtList<NbtCompound>()
                    for (e in existingList) {
                        mergedList.add(e)
                    }
                    chunkRoot["Entities"] = mergedList

                    regionFile.writeChunkNbt(localX, localZ, NbtFile("", chunkRoot))
                    writtenChunks++
                } catch (e: Exception) {
                    log("写入区块实体失败 [dim=${chunkKey.dimensionId}, chunk=${chunkKey.chunkX},${chunkKey.chunkZ}]: ${e.message}")
                }
            }
        } finally {
            for (rf in openRegionFiles.values) {
                try {
                    rf.close()
                } catch (_: Exception) {}
            }
        }

        log("Java 实体写入完成：共写入 $writtenEntities 个实体至 $writtenChunks 个区块。")
        return PipelineStats(writtenEntities, skippedModdedCount.get(), writtenChunks)
    }

    internal fun reconstructPassengersTree(entities: List<StagedEntity>): List<NbtCompound> {
        val entityMap = entities.associateBy { it.uniqueId }
        val isPassengerSet = mutableSetOf<Long>()

        for (staged in entities) {
            if (staged.vehicleUniqueId != 0L) {
                val vehicle = entityMap[staged.vehicleUniqueId]
                if (vehicle != null) {
                    val pList = vehicle.javaCompound.getNullableCompoundList("Passengers")?.toMutableList() ?: mutableListOf()
                    pList.add(staged.javaCompound)
                    val newList = NbtList<NbtCompound>()
                    pList.forEach { newList.add(it) }
                    vehicle.javaCompound["Passengers"] = newList
                    isPassengerSet.add(staged.uniqueId)
                }
            }
        }

        return entities.filter { it.uniqueId !in isPassengerSet }.map { it.javaCompound }
    }

    private fun getEntitiesDirectory(worldDir: File, dimensionId: Int): File {
        return when (dimensionId) {
            1 -> File(worldDir, "DIM-1/entities")
            2 -> File(worldDir, "DIM1/entities")
            else -> File(worldDir, "entities")
        }
    }

    private fun isDimensionEnabled(dim: Int, enabledDimensions: List<String>): Boolean {
        return when (dim) {
            0 -> enabledDimensions.contains("overworld")
            1 -> enabledDimensions.contains("the_nether")
            2 -> enabledDimensions.contains("the_end")
            else -> false
        }
    }

    private fun matchesPrefix(array: ByteArray, prefix: ByteArray): Boolean {
        if (array.size < prefix.size) return false
        for (i in prefix.indices) {
            if (array[i] != prefix[i]) return false
        }
        return true
    }

    private fun getInt(compound: NbtCompound, key: String, default: Int): Int {
        return when (val tag = compound[key]) {
            is NbtInt -> tag.value
            is NbtByte -> tag.signed.toInt()
            is NbtShort -> tag.value.toInt()
            else -> default
        }
    }

    private fun log(message: String) {
        onLog?.invoke(message)
        Log.i(TAG, message)
    }
}
