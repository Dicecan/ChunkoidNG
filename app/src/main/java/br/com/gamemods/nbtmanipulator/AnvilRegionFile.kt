package br.com.gamemods.nbtmanipulator

import java.io.*
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

public class AnvilRegionFile(public val file: File) : Closeable, AutoCloseable {

    private val raf: RandomAccessFile = RandomAccessFile(file, "rw")

    public val regionX: Int?
    public val regionZ: Int?

    init {
        val coords = parseRegionCoordinates(file.name)
        regionX = coords?.first
        regionZ = coords?.second
    }

    public data class ChunkInfo(
        val localX: Int,
        val localZ: Int,
        val sectorOffset: Int,
        val sectorCount: Int,
        val timestamp: Int
    ) {
        val exists: Boolean get() = sectorOffset > 0 && sectorCount > 0
        fun getGlobalX(regionX: Int): Int = regionX * 32 + localX
        fun getGlobalZ(regionZ: Int): Int = regionZ * 32 + localZ
        fun getBlockStartX(regionX: Int): Int = getGlobalX(regionX) * 16
        fun getBlockStartZ(regionZ: Int): Int = getGlobalZ(regionZ) * 16
    }

    @Synchronized
    public fun getChunkTable(): List<ChunkInfo> {
        val list = ArrayList<ChunkInfo>(1024)
        if (raf.length() < 8192L) return list

        val header = ByteArray(8192)
        raf.seek(0)
        raf.readFully(header)

        for (z in 0 until 32) {
            for (x in 0 until 32) {
                val index = (x and 31) + (z and 31) * 32
                val locOffset = index * 4
                val timeOffset = 4096 + index * 4

                val b0 = header[locOffset].toInt() and 0xFF
                val b1 = header[locOffset + 1].toInt() and 0xFF
                val b2 = header[locOffset + 2].toInt() and 0xFF
                val sectorCount = header[locOffset + 3].toInt() and 0xFF
                val sectorOffset = (b0 shl 16) or (b1 shl 8) or b2

                val t0 = header[timeOffset].toInt() and 0xFF
                val t1 = header[timeOffset + 1].toInt() and 0xFF
                val t2 = header[timeOffset + 2].toInt() and 0xFF
                val t3 = header[timeOffset + 3].toInt() and 0xFF
                val timestamp = (t0 shl 24) or (t1 shl 16) or (t2 shl 8) or t3

                list.add(ChunkInfo(x, z, sectorOffset, sectorCount, timestamp))
            }
        }
        return list
    }

    public fun getPopulatedChunks(): List<ChunkInfo> {
        return getChunkTable().filter { it.exists }
    }

    @Synchronized
    public fun getChunkInfo(localX: Int, localZ: Int): ChunkInfo {
        val index = (localX and 31) + (localZ and 31) * 32
        raf.seek((index * 4).toLong())
        val b0 = raf.read() and 0xFF
        val b1 = raf.read() and 0xFF
        val b2 = raf.read() and 0xFF
        val sectorCount = raf.read() and 0xFF
        val sectorOffset = (b0 shl 16) or (b1 shl 8) or b2

        raf.seek((4096 + index * 4).toLong())
        val timestamp = raf.readInt()

        return ChunkInfo(localX and 31, localZ and 31, sectorOffset, sectorCount, timestamp)
    }

    @Synchronized
    @Throws(IOException::class)
    public fun readChunkNbt(localX: Int, localZ: Int): NbtFile? {
        val info = getChunkInfo(localX, localZ)
        if (!info.exists) return null

        val byteOffset = info.sectorOffset.toLong() * 4096L
        if (byteOffset + 5 > raf.length()) {
            return null
        }

        raf.seek(byteOffset)
        val length = raf.readInt()
        if (length <= 1 || length > info.sectorCount * 4096) {
            return null
        }

        val compressionType = raf.read()
        val payload = ByteArray(length - 1)
        raf.readFully(payload)

        val rawStream = ByteArrayInputStream(payload)
        val decompressedStream: InputStream = when (compressionType) {
            1 -> GZIPInputStream(rawStream)
            2 -> InflaterInputStream(rawStream)
            3 -> rawStream
            else -> throw IOException("Unsupported Anvil chunk compression scheme: $compressionType")
        }

        return decompressedStream.use { stream ->
            val dataIn = DataInputStream(stream.buffered())
            NbtIO.readNbtFileDirectly(dataIn)
        }
    }

    @Synchronized
    @Throws(IOException::class)
    public fun writeChunkNbt(localX: Int, localZ: Int, nbtFile: NbtFile) {

        val baos = ByteArrayOutputStream()
        DeflaterOutputStream(baos).use { deflater ->
            val dataOut = DataOutputStream(deflater)
            NbtIO.writeNbtFileDirectly(dataOut, nbtFile)
            dataOut.flush()
        }
        val compressedBytes = baos.toByteArray()
        val payloadSize = compressedBytes.size + 1
        val totalChunkBytes = 4 + payloadSize
        val neededSectors = (totalChunkBytes + 4095) / 4096

        val index = (localX and 31) + (localZ and 31) * 32
        val currentInfo = getChunkInfo(localX, localZ)

        val targetSectorOffset: Int
        if (currentInfo.exists && neededSectors <= currentInfo.sectorCount) {

            targetSectorOffset = currentInfo.sectorOffset
        } else {

            val fileLen = raf.length()
            targetSectorOffset = ((fileLen + 4095) / 4096).toInt().coerceAtLeast(2)
        }

        val targetByteOffset = targetSectorOffset.toLong() * 4096L
        raf.seek(targetByteOffset)
        raf.writeInt(payloadSize)
        raf.write(2)
        raf.write(compressedBytes)

        val writtenBytes = 4 + 1 + compressedBytes.size
        val paddingNeeded = (neededSectors * 4096) - writtenBytes
        if (paddingNeeded > 0) {
            raf.write(ByteArray(paddingNeeded))
        }

        raf.seek((index * 4).toLong())
        raf.write((targetSectorOffset shr 16) and 0xFF)
        raf.write((targetSectorOffset shr 8) and 0xFF)
        raf.write(targetSectorOffset and 0xFF)
        raf.write(neededSectors and 0xFF)

        val currentUnixTime = (System.currentTimeMillis() / 1000L).toInt()
        raf.seek((4096 + index * 4).toLong())
        raf.writeInt(currentUnixTime)
    }

    @Synchronized
    @Throws(IOException::class)
    public fun deleteChunk(localX: Int, localZ: Int) {
        val index = (localX and 31) + (localZ and 31) * 32
        raf.seek((index * 4).toLong())
        raf.writeInt(0)

        raf.seek((4096 + index * 4).toLong())
        raf.writeInt(0)
    }

    @Synchronized
    public fun readChunkInhabitedTime(localX: Int, localZ: Int): Long? {
        val nbt = try {
            readChunkNbt(localX, localZ)
        } catch (e: Exception) {
            null
        } ?: return null

        val tag = nbt.tag
        if (tag is NbtCompound) {
            val direct = (tag["InhabitedTime"] as? NbtLong)?.value
                ?: (tag["InhabitedTime"] as? NbtInt)?.value?.toLong()
            if (direct != null) return direct

            val level = tag["Level"] as? NbtCompound
            if (level != null) {
                return (level["InhabitedTime"] as? NbtLong)?.value
                    ?: (level["InhabitedTime"] as? NbtInt)?.value?.toLong()
            }
        }
        return null
    }

    @Synchronized
    public fun scanUselessChunks(
        maxInhabitedTimeTicks: Long = 0L,
        onProgress: ((scanned: Int, total: Int) -> Unit)? = null
    ): List<Pair<ChunkInfo, Long>> {
        val populated = getPopulatedChunks()
        val useless = mutableListOf<Pair<ChunkInfo, Long>>()
        populated.forEachIndexed { index, chunk ->
            val inhabitedTime = readChunkInhabitedTime(chunk.localX, chunk.localZ) ?: 0L
            if (inhabitedTime <= maxInhabitedTimeTicks) {
                useless.add(Pair(chunk, inhabitedTime))
            }
            onProgress?.invoke(index + 1, populated.size)
        }
        return useless
    }

    @Synchronized
    public fun deleteChunks(chunkCoords: Collection<Pair<Int, Int>>) {
        for ((lx, lz) in chunkCoords) {
            deleteChunk(lx, lz)
        }
    }

    override fun close() {
        raf.close()
    }

    public companion object {

        @JvmStatic
        public fun parseRegionCoordinates(fileName: String): Pair<Int, Int>? {
            val regex = Regex("""[rc]\.(-?\d+)\.(-?\d+)\.(mca|mcr)""", RegexOption.IGNORE_CASE)
            val match = regex.find(fileName) ?: return null
            val x = match.groupValues[1].toIntOrNull() ?: return null
            val z = match.groupValues[2].toIntOrNull() ?: return null
            return Pair(x, z)
        }

        @JvmStatic
        public fun isRegionFileName(name: String): Boolean {
            return name.endsWith(".mca", ignoreCase = true) || name.endsWith(".mcr", ignoreCase = true)
        }
    }
}
