package com.noches.chunkoidng.core.entity

import android.util.Log
import br.com.gamemods.nbtmanipulator.NbtIO
import org.iq80.leveldb.CompressionType
import org.iq80.leveldb.DB
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class BedrockToBedrockEntityPipeline(
    private val onLog: ((String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "BedrockToBedrockPipeline"
        private val ACTOR_PREFIX = "actorprefix".toByteArray(Charsets.US_ASCII)
        private val DIGP_PREFIX = "digp".toByteArray(Charsets.US_ASCII)
    }

    private data class DigpEntry(
        val key: ByteArray,
        val value: ByteArray,
        val dimensionId: Int,
        val chunkX: Int,
        val chunkZ: Int
    )

    private val stagedActors = ConcurrentHashMap<Long, ByteArray>()
    private val stagedDigpList = mutableListOf<DigpEntry>()
    private val actorDimensions = ConcurrentHashMap<Long, Int>()
    private val totalExtracted = AtomicInteger(0)
    private val skippedModdedCount = AtomicInteger(0)

    fun scan(inputDir: File, dimensions: List<String>) {
        stagedActors.clear()
        stagedDigpList.clear()
        actorDimensions.clear()
        totalExtracted.set(0)
        skippedModdedCount.set(0)

        val dbDir = File(inputDir, "db")
        if (!dbDir.isDirectory) return

        val options = Options()
        options.createIfMissing(false)
        var db: DB? = null

        try {
            db = Iq80DBFactory.factory.open(dbDir, options)
            val iterator = db.iterator()
            try {
                iterator.seekToFirst()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val key = entry.key
                    val value = entry.value

                    if (startsWith(key, ACTOR_PREFIX) && key.size == 19) {
                        val uniqueId = ByteBuffer.wrap(key, 11, 8).order(ByteOrder.BIG_ENDIAN).long
                        stagedActors[uniqueId] = value
                        totalExtracted.incrementAndGet()
                    } else if (startsWith(key, DIGP_PREFIX) && key.size >= 12) {
                        val buf = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN)
                        val chunkX = buf.getInt(4)
                        val chunkZ = buf.getInt(8)
                        val dim = if (key.size >= 16) buf.getInt(12) else 0

                        val isDimEnabled = when (dim) {
                            0 -> "overworld" in dimensions
                            1 -> "the_nether" in dimensions
                            2 -> "the_end" in dimensions
                            else -> false
                        }

                        if (isDimEnabled) {
                            stagedDigpList.add(DigpEntry(key, value, dim, chunkX, chunkZ))
                            if (value.size >= 8) {
                                val vBuf = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
                                val count = value.size / 8
                                for (i in 0 until count) {
                                    val actorId = vBuf.getLong(i * 8)
                                    actorDimensions[actorId] = dim
                                }
                            }
                        }
                    }
                }
            } finally {
                iterator.close()
            }
        } catch (e: Exception) {
            log("扫描输入 Bedrock LevelDB 失败: ${e.message}")
        } finally {
            try {
                db?.close()
            } catch (_: Exception) {}
        }
    }

    fun flushToLevelDb(destWorldDir: File): PipelineStats {
        val total = totalExtracted.get()
        if (total == 0 || stagedActors.isEmpty()) {
            return PipelineStats(0, skippedModdedCount.get(), 0)
        }

        val destDbDir = File(destWorldDir, "db")
        if (!destDbDir.isDirectory) {
            log("目标目录未发现有效的 LevelDB (db/)，无法写入实体数据。")
            return PipelineStats(0, skippedModdedCount.get(), 0)
        }

        val options = Options()
        options.compressionType(CompressionType.ZLIB_RAW)
        options.createIfMissing(false)

        var writtenActors = 0
        var writtenChunks = 0
        var db: DB? = null

        val validActorIds = mutableSetOf<Long>()
        val filteredActors = mutableMapOf<Long, ByteArray>()

        for ((id, nbtBytes) in stagedActors) {
            val dim = actorDimensions[id]
            if (dim == null && stagedDigpList.isNotEmpty()) {
                continue
            }

            try {
                val bais = ByteArrayInputStream(nbtBytes)
                val nbt = NbtIO.readNbtFile(bais, compressed = false, littleEndian = true)
                val identifier = nbt.compound.getNullableString("identifier")
                if (identifier != null && EntityIdMapper.toJavaId(identifier) == null) {
                    skippedModdedCount.incrementAndGet()
                    continue
                }
                validActorIds.add(id)
                filteredActors[id] = nbtBytes
            } catch (_: Exception) {
                validActorIds.add(id)
                filteredActors[id] = nbtBytes
            }
        }

        try {
            db = Iq80DBFactory.factory.open(destDbDir, options)
            val batch = db.createWriteBatch()
            try {
                for (digp in stagedDigpList) {
                    val valBytes = digp.value
                    if (valBytes.isEmpty()) continue

                    val vBuf = ByteBuffer.wrap(valBytes).order(ByteOrder.LITTLE_ENDIAN)
                    val count = valBytes.size / 8
                    val survivors = mutableListOf<Long>()

                    for (i in 0 until count) {
                        val actorId = vBuf.getLong(i * 8)
                        if (actorId in validActorIds) {
                            survivors.add(actorId)
                        }
                    }

                    if (survivors.isNotEmpty()) {
                        val outBuf = ByteBuffer.allocate(survivors.size * 8).order(ByteOrder.LITTLE_ENDIAN)
                        for (actorId in survivors) {
                            outBuf.putLong(actorId)
                            val actorBytes = filteredActors[actorId]
                            if (actorBytes != null) {
                                val storageKey = EntityLinks.storageKey(actorId)
                                batch.put(ACTOR_PREFIX + storageKey, actorBytes)
                                writtenActors++
                            }
                        }
                        batch.put(digp.key, outBuf.array())
                        writtenChunks++
                    }
                }

                db.write(batch)
            } finally {
                batch.close()
            }
        } catch (e: Exception) {
            log("写入目标 Bedrock LevelDB 异常: ${e.message}")
        } finally {
            try {
                db?.close()
            } catch (_: Exception) {}
            stagedActors.clear()
            stagedDigpList.clear()
            actorDimensions.clear()
        }

        return PipelineStats(writtenActors, skippedModdedCount.get(), writtenChunks)
    }

    private fun startsWith(array: ByteArray, prefix: ByteArray): Boolean {
        if (array.size < prefix.size) return false
        for (i in prefix.indices) {
            if (array[i] != prefix[i]) return false
        }
        return true
    }

    private fun log(message: String) {
        onLog?.invoke(message)
        Log.i(TAG, message)
    }
}
