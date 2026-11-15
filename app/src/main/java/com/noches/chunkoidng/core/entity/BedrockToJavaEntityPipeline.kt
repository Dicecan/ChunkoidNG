package com.noches.chunkoidng.core.entity

import android.util.Log
import br.com.gamemods.nbtmanipulator.*
import com.noches.chunkoidng.core.leveldb.SafeEnv
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.DbImpl
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

    fun scan(
        bedrockWorldDir: File,
        enabledDimensions: List<String> = listOf("overworld", "the_nether", "the_end"),
        targetFormatId: String = ""
    ) {
        stagedChunks.clear()
        totalExtracted.set(0)
        skippedModdedCount.set(0)

        val dbDir = File(bedrockWorldDir, "db")
        if (!dbDir.isDirectory) {
            log("未找到基岩版数据库目录 (db/)，跳过实体扫描。")
            return
        }

        val targetOptions = JavaEntityBuilder.JavaTargetOptions.fromProfile(
            JavaTargetFormatHelper.resolveProfile(targetFormatId)
        )

        log("开始扫描基岩版 LevelDB 实体数据...")

        val options = Options().createIfMissing(false)

        try {
            DbImpl(options, dbDir.absolutePath, SafeEnv()).use { db ->
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

                        processBedrockActor(actor, enabledDimensions, targetOptions)
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

    internal fun processBedrockActor(
        actor: NbtCompound,
        enabledDimensions: List<String>,
        targetOptions: JavaEntityBuilder.JavaTargetOptions = JavaEntityBuilder.JavaTargetOptions()
    ) {
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

        val javaCompound = JavaEntityBuilder.build(actor, targetOptions)
        if (javaCompound == null) {
            skippedModdedCount.incrementAndGet()
            return
        }

        val vehicleId = extractVehicleId(actor, uniqueId)

        val chunkKey = ChunkKey(dim, chunkX, chunkZ)
        val entity = StagedEntity(uniqueId, vehicleId, javaCompound)
        stagedChunks.computeIfAbsent(chunkKey) { mutableListOf() }.add(entity)
        totalExtracted.incrementAndGet()
    }

    internal fun extractVehicleId(actor: NbtCompound, uniqueId: Long): Long {
        val links = actor.getNullableCompoundList("LinksTag") ?: return 0L
        for (link in links) {
            val rideId = link.getNullableLong("RideID") ?: 0L
            val vId = link.getNullableLong("VehicleID") ?: 0L
            if (rideId != 0L && rideId != uniqueId && vId == 0L) {
                return rideId
            }
            if (rideId == uniqueId && vId != 0L && vId != uniqueId) {
                return vId
            }
        }
        return 0L
    }

    fun flushToJava(
        javaWorldDir: File,
        targetFormatId: String = "",
        inputDir: File? = null
    ): PipelineStats {
        val total = totalExtracted.get()
        if (total == 0 || stagedChunks.isEmpty()) {
            log("无待写入的 Java 实体记录，跳过写入。")
            return PipelineStats(0, skippedModdedCount.get(), 0)
        }

        val profile = JavaTargetFormatHelper.resolveProfile(targetFormatId, javaWorldDir, inputDir)
        log("正在向 Java 版存档写入 $total 个实体与乘骑树 (DataVersion=${profile.dataVersion})...")

        var writtenChunks = 0
        var writtenEntities = 0

        val openRegionFiles = mutableMapOf<File, AnvilRegionFile>()

        try {
            for ((chunkKey, stagedList) in stagedChunks) {
                if (stagedList.isEmpty()) continue

                val rootEntities = reconstructPassengersTree(stagedList)
                if (rootEntities.isEmpty()) continue

                val targetDir = if (profile.usesEntitiesDir) {
                    JavaTargetFormatHelper.getEntitiesDirectory(
                        javaWorldDir,
                        chunkKey.dimensionId,
                        profile.usesDimensionNamespaces
                    )
                } else {
                    JavaTargetFormatHelper.getRegionDirectory(
                        javaWorldDir,
                        chunkKey.dimensionId,
                        profile.usesDimensionNamespaces
                    )
                }
                if (!targetDir.exists()) targetDir.mkdirs()

                val regionX = Math.floorDiv(chunkKey.chunkX, 32)
                val regionZ = Math.floorDiv(chunkKey.chunkZ, 32)
                val localX = Math.floorMod(chunkKey.chunkX, 32)
                val localZ = Math.floorMod(chunkKey.chunkZ, 32)

                val mcaFile = File(targetDir, "r.$regionX.$regionZ.mca")
                val regionFile = openRegionFiles.computeIfAbsent(mcaFile) { AnvilRegionFile(it) }

                try {
                    if (profile.usesEntitiesDir) {
                        val existingFile = regionFile.readChunkNbt(localX, localZ)
                        val chunkRoot = existingFile?.compound ?: NbtCompound().apply {
                            this["Position"] = NbtIntArray(intArrayOf(chunkKey.chunkX, chunkKey.chunkZ))
                            this["DataVersion"] = NbtInt(profile.dataVersion)
                        }
                        chunkRoot["DataVersion"] = NbtInt(profile.dataVersion)

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
                    } else {
                        val existingFile = regionFile.readChunkNbt(localX, localZ) ?: continue
                        val root = existingFile.compound
                        val level = root.getNullableCompound("Level") ?: continue

                        val existingList = level.getNullableCompoundList("Entities")?.toMutableList() ?: mutableListOf()
                        for (entity in rootEntities) {
                            existingList.add(entity)
                            writtenEntities++
                        }

                        val mergedList = NbtList<NbtCompound>()
                        for (e in existingList) {
                            mergedList.add(e)
                        }
                        level["Entities"] = mergedList

                        regionFile.writeChunkNbt(localX, localZ, NbtFile("", root))
                        writtenChunks++
                    }
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
            if (staged.vehicleUniqueId != 0L && staged.vehicleUniqueId != staged.uniqueId) {
                if (createsCycle(staged.uniqueId, staged.vehicleUniqueId, entityMap)) continue
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

    private fun createsCycle(startId: Long, targetVehicleId: Long, entityMap: Map<Long, StagedEntity>): Boolean {
        val visited = mutableSetOf(startId)
        var cur = targetVehicleId
        while (cur != 0L) {
            if (!visited.add(cur)) return true
            cur = entityMap[cur]?.vehicleUniqueId ?: 0L
        }
        return false
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
