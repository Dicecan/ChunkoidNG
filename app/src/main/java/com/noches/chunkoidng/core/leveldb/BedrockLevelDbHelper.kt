package com.noches.chunkoidng.core.leveldb

import androidx.annotation.StringRes
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtIO
import com.noches.chunkoidng.R
import org.iq80.leveldb.DB
import org.iq80.leveldb.Options
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class LevelDbCategory(@StringRes val titleRes: Int) {
    ALL(R.string.leveldb_cat_all),
    PLAYER(R.string.leveldb_cat_player),
    ENTITY(R.string.leveldb_cat_entity),
    BLOCK_ENTITY(R.string.leveldb_cat_block_entity),
    WORLD(R.string.leveldb_cat_world),
    CHUNK(R.string.leveldb_cat_chunk),
    OTHER(R.string.leveldb_cat_other)
}

data class LevelDbRecord(
    val key: ByteArray,
    val keyString: String,
    val displayName: String,
    val category: LevelDbCategory,
    val chunkX: Int? = null,
    val chunkZ: Int? = null,
    val dimensionId: Int? = null,
    val valueSize: Int,
    val isNbt: Boolean,
    val hasMultipleCompounds: Boolean = false
) {
    fun keyToHex(): String {
        return key.joinToString("") { "%02x".format(it) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LevelDbRecord
        return key.contentEquals(other.key)
    }

    override fun hashCode(): Int {
        return key.contentHashCode()
    }
}

class BedrockLevelDbHelper(val dbFolder: File) : Closeable, AutoCloseable {

    data class ChunkCoordinateSummary(
        val populated: Set<Pair<Int, Int>>,
        val blockEntities: Set<Pair<Int, Int>>
    )

    private var db: DB? = null
    private val dbLock = Any()

    fun open(readOnly: Boolean = false): DB {
        synchronized(dbLock) {
            if (db == null) {
                val options = Options().apply {
                    createIfMissing(false)
                }
                db = org.iq80.leveldb.impl.DbImpl(options, dbFolder.absolutePath, SafeEnv())
            }
            return db!!
        }
    }

    fun getAllRecords(onProgress: ((scannedCount: Int) -> Unit)? = null): List<LevelDbRecord> {
        val list = mutableListOf<LevelDbRecord>()
        synchronized(dbLock) {
            val iterator = open().iterator()
            var count = 0
            try {
                iterator.seekToFirst()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    // LevelDB iterators are allowed to reuse their backing buffers.
                    // Records outlive the iterator, so retain an owned key copy.
                    val key = entry.key.copyOf()
                    val value = entry.value
                    // categorizeKey only inspects the first few bytes. Reusing the
                    // iterator-owned value avoids a copy for every LevelDB record.
                    list.add(categorizeKey(key, value.size, value))
                    count++
                    if (count % 200 == 0) {
                        onProgress?.invoke(count)
                    }
                }
                onProgress?.invoke(count)
            } finally {
                iterator.close()
            }
        }
        return list
    }

    fun get(key: ByteArray): ByteArray? {
        return synchronized(dbLock) { open().get(key) }
    }

    fun put(key: ByteArray, value: ByteArray) {
        synchronized(dbLock) { open().put(key, value) }
    }

    fun delete(key: ByteArray) {
        synchronized(dbLock) { open().delete(key) }
    }

    fun deleteChunk(chunkX: Int, chunkZ: Int, dimensionId: Int? = null): Int {
        return deleteChunkRecords { info ->
            isKnownChunkTag(info.tagByte) && info.chunkX == chunkX && info.chunkZ == chunkZ &&
                (dimensionId == null || info.dimensionId == dimensionId)
        }
    }

    fun deleteChunks(chunkCoords: Collection<Pair<Int, Int>>, dimensionId: Int? = null): Int {
        val coordSet = chunkCoords.toSet()
        return deleteChunkRecords { info ->
            isKnownChunkTag(info.tagByte) && coordSet.contains(Pair(info.chunkX, info.chunkZ)) &&
                (dimensionId == null || info.dimensionId == dimensionId)
        }
    }

    fun deleteChunksOutsideRange(minChunkX: Int, maxChunkX: Int, minChunkZ: Int, maxChunkZ: Int, dimensionId: Int? = null): Int {
        return deleteChunkRecords { info ->
            isKnownChunkTag(info.tagByte) && (dimensionId == null || info.dimensionId == dimensionId) &&
                (info.chunkX < minChunkX || info.chunkX > maxChunkX ||
                    info.chunkZ < minChunkZ || info.chunkZ > maxChunkZ)
        }
    }

    private fun isKnownChunkTag(tagByte: Int): Boolean {
        return when (tagByte) {
            0x31, 0x32, 0x2F, 0x2C, 0x2D, 0x33, 0x34, 0x35, 0x36, 0x37 -> true
            else -> false
        }
    }

    private data class ChunkKeyInfo(
        val chunkX: Int,
        val chunkZ: Int,
        val dimensionId: Int,
        val tagByte: Int
    )

    private fun deleteChunkRecords(predicate: (ChunkKeyInfo) -> Boolean): Int {
        synchronized(dbLock) {
            val database = open()
            val keys = mutableListOf<ByteArray>()
            val iterator = database.iterator()
            try {
                iterator.seekToFirst()
                while (iterator.hasNext()) {
                    val key = iterator.next().key
                    val info = parseChunkKey(key)
                    if (info != null && predicate(info)) {
                        keys += key.copyOf()
                    }
                }
            } finally {
                iterator.close()
            }

            if (keys.isEmpty()) return 0
            val batch = database.createWriteBatch()
            try {
                keys.forEach(batch::delete)
                database.write(batch)
            } finally {
                batch.close()
            }
            return keys.size
        }
    }

    fun getPopulatedChunkCoordinates(dimensionId: Int = 0): Set<Pair<Int, Int>> {
        return scanChunkCoordinates(dimensionId).populated
    }

    fun getBlockEntityChunkCoordinates(dimensionId: Int = 0): Set<Pair<Int, Int>> {
        return scanChunkCoordinates(dimensionId).blockEntities
    }

    fun getChunkCoordinateSummary(dimensionId: Int = 0): ChunkCoordinateSummary {
        return scanChunkCoordinates(dimensionId)
    }

    private fun scanChunkCoordinates(dimensionId: Int): ChunkCoordinateSummary {
        val populated = mutableSetOf<Pair<Int, Int>>()
        val blockEntities = mutableSetOf<Pair<Int, Int>>()
        synchronized(dbLock) {
            val iterator = open().iterator()
            try {
                iterator.seekToFirst()
                while (iterator.hasNext()) {
                    val info = parseChunkKey(iterator.next().key) ?: continue
                    if (info.dimensionId != dimensionId) continue
                    val coordinate = Pair(info.chunkX, info.chunkZ)
                    populated += coordinate
                    if (info.tagByte == 0x31) blockEntities += coordinate
                }
            } finally {
                iterator.close()
            }
        }
        return ChunkCoordinateSummary(populated, blockEntities)
    }

    override fun close() {
        synchronized(dbLock) {
            try {
                db?.close()
            } catch (_: Exception) {
            } finally {
                db = null
            }
        }
    }

    companion object {

        fun categorizeKey(key: ByteArray, valueSize: Int, valueSample: ByteArray?): LevelDbRecord {
            val keyStr = try {
                String(key, Charsets.UTF_8)
            } catch (_: Exception) {
                ""
            }

            val isNbt = valueSample != null && valueSample.isNotEmpty() && (
                valueSample[0] == 0x0A.toByte() ||
                valueSample[0] == 0x09.toByte() ||
                valueSample[0] == 0x08.toByte()
            )

            if (keyStr == "~local_player") {
                return LevelDbRecord(key, keyStr, "~local_player", LevelDbCategory.PLAYER, valueSize = valueSize, isNbt = true)
            }
            if (keyStr.startsWith("player_server_")) {
                val uuid = keyStr.removePrefix("player_server_")
                return LevelDbRecord(key, keyStr, "Server Player ($uuid)", LevelDbCategory.PLAYER, valueSize = valueSize, isNbt = true)
            }
            if (keyStr.startsWith("player_")) {
                val uuid = keyStr.removePrefix("player_")
                return LevelDbRecord(key, keyStr, "Player ($uuid)", LevelDbCategory.PLAYER, valueSize = valueSize, isNbt = true)
            }

            if (key.size >= 11 && keyStr.startsWith("actorprefix")) {
                val actorId = try {
                    if (key.size >= 19) ByteBuffer.wrap(key, 11, 8).order(ByteOrder.LITTLE_ENDIAN).long else null
                } catch (_: Exception) { null }
                val title = if (actorId != null) "Entity ($actorId)" else "Entity ($keyStr)"
                return LevelDbRecord(key, keyStr, title, LevelDbCategory.ENTITY, valueSize = valueSize, isNbt = true)
            }
            if (keyStr.startsWith("digp")) {
                return LevelDbRecord(key, keyStr, "Digp Entity ($keyStr)", LevelDbCategory.ENTITY, valueSize = valueSize, isNbt = true)
            }

            if (keyStr == "portals") {
                return LevelDbRecord(key, keyStr, "Portals (portals)", LevelDbCategory.WORLD, valueSize = valueSize, isNbt = true)
            }
            if (keyStr == "scoreboard") {
                return LevelDbRecord(key, keyStr, "Scoreboard (scoreboard)", LevelDbCategory.WORLD, valueSize = valueSize, isNbt = true)
            }
            if (keyStr == "autonomousentities") {
                return LevelDbRecord(key, keyStr, "Autonomous Entities", LevelDbCategory.WORLD, valueSize = valueSize, isNbt = true)
            }
            if (keyStr == "Overworld" || keyStr == "Nether" || keyStr == "TheEnd") {
                return LevelDbRecord(key, keyStr, "Dimension ($keyStr)", LevelDbCategory.WORLD, valueSize = valueSize, isNbt = true)
            }
            if (keyStr.startsWith("map_")) {
                return LevelDbRecord(key, keyStr, "Map (${keyStr.removePrefix("map_")})", LevelDbCategory.WORLD, valueSize = valueSize, isNbt = isNbt)
            }
            if (keyStr.startsWith("village_") || keyStr.startsWith("VILLAGE_")) {
                return LevelDbRecord(key, keyStr, "Village ($keyStr)", LevelDbCategory.WORLD, valueSize = valueSize, isNbt = true)
            }
            if (keyStr.startsWith("tickingarea_")) {
                return LevelDbRecord(key, keyStr, "Ticking Area ($keyStr)", LevelDbCategory.WORLD, valueSize = valueSize, isNbt = true)
            }
            if (keyStr == "schedulerWT") {
                return LevelDbRecord(key, keyStr, "Scheduler (schedulerWT)", LevelDbCategory.WORLD, valueSize = valueSize, isNbt = isNbt)
            }

            parseChunkKey(key)?.let { chunkKey ->
                val chunkX = chunkKey.chunkX
                val chunkZ = chunkKey.chunkZ
                val dim = chunkKey.dimensionId
                val tagByte = chunkKey.tagByte
                val hasDim = key.size >= 13
                val dimName = when (dim) {
                    1 -> " [Nether]"
                    2 -> " [End]"
                    else -> ""
                }

                when (tagByte) {
                    0x31 -> return LevelDbRecord(key, keyStr, "Chunk [$chunkX, $chunkZ]$dimName Block Entities", LevelDbCategory.BLOCK_ENTITY, chunkX, chunkZ, dim, valueSize, isNbt = true, hasMultipleCompounds = true)
                    0x32 -> return LevelDbRecord(key, keyStr, "Chunk [$chunkX, $chunkZ]$dimName Entity Data", LevelDbCategory.ENTITY, chunkX, chunkZ, dim, valueSize, isNbt = true, hasMultipleCompounds = true)
                    0x2F -> {
                        val subIndex = if (hasDim && key.size >= 14) key[13].toInt() else if (key.size >= 10) key[9].toInt() else 0
                        return LevelDbRecord(key, keyStr, "SubChunk [$chunkX, $chunkZ]$dimName (Y: $subIndex)", LevelDbCategory.CHUNK, chunkX, chunkZ, dim, valueSize, isNbt = false)
                    }
                    0x2C -> return LevelDbRecord(key, keyStr, "Chunk [$chunkX, $chunkZ]$dimName Version", LevelDbCategory.CHUNK, chunkX, chunkZ, dim, valueSize, isNbt = false)
                    0x2D -> return LevelDbRecord(key, keyStr, "Chunk [$chunkX, $chunkZ]$dimName 2D Data", LevelDbCategory.CHUNK, chunkX, chunkZ, dim, valueSize, isNbt = false)
                    0x33 -> return LevelDbRecord(key, keyStr, "Chunk [$chunkX, $chunkZ]$dimName Pending Ticks", LevelDbCategory.CHUNK, chunkX, chunkZ, dim, valueSize, isNbt = true)
                    0x34 -> return LevelDbRecord(key, keyStr, "Chunk [$chunkX, $chunkZ]$dimName Extra Block Data", LevelDbCategory.CHUNK, chunkX, chunkZ, dim, valueSize, isNbt = isNbt)
                    0x35 -> return LevelDbRecord(key, keyStr, "Chunk [$chunkX, $chunkZ]$dimName Biome State", LevelDbCategory.CHUNK, chunkX, chunkZ, dim, valueSize, isNbt = false)
                    0x36 -> return LevelDbRecord(key, keyStr, "Chunk [$chunkX, $chunkZ]$dimName Generation State", LevelDbCategory.CHUNK, chunkX, chunkZ, dim, valueSize, isNbt = false)
                    0x37 -> return LevelDbRecord(key, keyStr, "Chunk [$chunkX, $chunkZ]$dimName Spawn Data", LevelDbCategory.CHUNK, chunkX, chunkZ, dim, valueSize, isNbt = isNbt)
                }
            }

            val fallbackName = if (keyStr.isNotEmpty() && keyStr.all { it.isLetterOrDigit() || it in "_-~.: " }) {
                keyStr
            } else {
                "0x" + key.take(8).joinToString("") { "%02x".format(it) } + if (key.size > 8) "..." else ""
            }
            return LevelDbRecord(key, keyStr, fallbackName, LevelDbCategory.OTHER, valueSize = valueSize, isNbt = isNbt)
        }

        private fun parseChunkKey(key: ByteArray): ChunkKeyInfo? {
            if (key.size !in 8..14) return null
            val chunkX = readLittleEndianInt(key, 0)
            val chunkZ = readLittleEndianInt(key, 4)
            if (key.size == 8) return ChunkKeyInfo(chunkX, chunkZ, 0, -1)

            val hasDim = key.size >= 13
            val dimensionId = if (hasDim) readLittleEndianInt(key, 8) else 0
            val tagOffset = if (hasDim) 12 else 8
            return ChunkKeyInfo(
                chunkX = chunkX,
                chunkZ = chunkZ,
                dimensionId = dimensionId,
                tagByte = key[tagOffset].toInt() and 0xFF
            )
        }

        private fun readLittleEndianInt(bytes: ByteArray, offset: Int): Int {
            return (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                (bytes[offset + 3].toInt() shl 24)
        }

        fun readBedrockNbt(bytes: ByteArray, isMultiple: Boolean = false): NbtFile {
            if (!isMultiple) {
                try {
                    return readBedrockRoot(ByteArrayInputStream(bytes))
                } catch (e: Exception) {
                    return readBedrockRoots(bytes, e)
                }
            }

            return readBedrockRoots(bytes, null)
        }

        private fun readBedrockRoot(stream: ByteArrayInputStream): NbtFile {
            return NbtIO.readNbtFile(stream, compressed = false, littleEndian = true, readHeaders = false)
        }

        private fun readBedrockRoots(bytes: ByteArray, firstFailure: Exception?): NbtFile {
            val stream = ByteArrayInputStream(bytes)
            val rootCompound = NbtCompound()
            var index = 0
            var failure: Exception? = firstFailure

            while (stream.available() > 0) {
                if (isZeroPadding(stream)) break
                val availableBefore = stream.available()
                try {
                    val nbt = readBedrockRoot(stream)
                    val name = if (nbt.name.isNotEmpty()) nbt.name else "entry_$index"
                    rootCompound[name] = nbt.tag
                    index++
                } catch (e: Exception) {
                    failure = e
                    throw IOException("Invalid Bedrock NBT payload", e)
                }
                if (stream.available() >= availableBefore) {
                    throw IOException("Bedrock NBT parser made no progress")
                }
            }

            if (index == 0) {
                throw failure ?: EOFException("Empty Bedrock NBT payload")
            }
            return NbtFile("Root", rootCompound)
        }

        private fun isZeroPadding(stream: ByteArrayInputStream): Boolean {
            stream.mark(stream.available())
            if (stream.read() != 0) {
                stream.reset()
                return false
            }
            var value = stream.read()
            while (value == 0) value = stream.read()
            val allZero = value < 0
            stream.reset()
            return allZero
        }

        fun writeBedrockNbt(nbtFile: NbtFile, wasMultiple: Boolean = false): ByteArray {
            val baos = ByteArrayOutputStream()
            if (!wasMultiple) {
                NbtIO.writeNbtFile(baos, nbtFile, compressed = false, littleEndian = true)
            } else {
                val tag = nbtFile.tag
                if (tag is NbtCompound) {
                    tag.forEach { (_, childTag) ->
                        val singleFile = NbtFile("", childTag)
                        NbtIO.writeNbtFile(baos, singleFile, compressed = false, littleEndian = true)
                    }
                } else {
                    NbtIO.writeNbtFile(baos, nbtFile, compressed = false, littleEndian = true)
                }
            }
            return baos.toByteArray()
        }
    }
}
