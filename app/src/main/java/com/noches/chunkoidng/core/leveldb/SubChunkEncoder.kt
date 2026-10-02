package com.noches.chunkoidng.core.leveldb

import br.com.gamemods.nbtmanipulator.*
import java.io.ByteArrayOutputStream

object SubChunkEncoder {

    const val SUBCHUNK_SIZE = 4096
    const val DEFAULT_STORAGE_VERSION: Byte = 8

    fun getLocalIndex(localX: Int, localY: Int, localZ: Int): Int =
        (localX shl 8) or (localZ shl 4) or localY

    fun encode(
        blocks: Array<BedrockBlockState>,
        storageVersion: Byte = DEFAULT_STORAGE_VERSION
    ): ByteArray {
        require(blocks.size == SUBCHUNK_SIZE) {
            "SubChunk blocks array must have exactly $SUBCHUNK_SIZE elements, got ${blocks.size}"
        }

        val palette = mutableListOf<BedrockBlockState>()
        val paletteMap = HashMap<BedrockBlockState, Int>()

        for (b in blocks) {
            if (!paletteMap.containsKey(b)) {
                paletteMap[b] = palette.size
                palette.add(b)
            }
        }

        val bitsPerBlock = BitArray.getOptimalBitsPerBlock(palette.size)
        val bitArray = BitArray(bitsPerBlock, SUBCHUNK_SIZE)

        for (i in 0 until SUBCHUNK_SIZE) {
            val paletteIndex = paletteMap[blocks[i]] ?: 0
            bitArray[i] = paletteIndex
        }

        val baos = ByteArrayOutputStream(SUBCHUNK_SIZE + palette.size * 64)

        baos.write(storageVersion.toInt())
        baos.write(1)

        val layerHeader = (bitsPerBlock shl 1) or 0
        baos.write(layerHeader)

        for (word in bitArray.words) {
            baos.write(word and 0xFF)
            baos.write((word ushr 8) and 0xFF)
            baos.write((word ushr 16) and 0xFF)
            baos.write((word ushr 24) and 0xFF)
        }

        val paletteSize = palette.size
        baos.write(paletteSize and 0xFF)
        baos.write((paletteSize ushr 8) and 0xFF)
        baos.write((paletteSize ushr 16) and 0xFF)
        baos.write((paletteSize ushr 24) and 0xFF)

        for (state in palette) {
            NbtIO.writeNbtFile(baos, NbtFile("", state.toNbt()), compressed = false, littleEndian = true)
        }

        return baos.toByteArray()
    }
}
