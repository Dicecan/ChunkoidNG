package com.noches.chunkoidng

import org.iq80.leveldb.util.ZLib
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.ByteBuffer

class LevelDbZLibTest {

    @Test
    fun testZLibClassResolution() {
        val clazz = Class.forName("org.iq80.leveldb.util.ZLib")
        assertNotNull(clazz)
    }

    @Test
    fun testZLibRawCompressionAndDecompression() {
        val originalData = "Hello Minecraft Bedrock LevelDB with raw ZLib compression!".toByteArray(Charsets.UTF_8)
        val compressedBuffer = ByteArray(originalData.size * 2 + 1024)
        val compressedLength = ZLib.compress(originalData, 0, originalData.size, compressedBuffer, 0, true)

        val compressedSlice = ByteBuffer.wrap(compressedBuffer, 0, compressedLength)
        val decompressedBuffer = ZLib.uncompress(compressedSlice, true)

        val decompressedBytes = ByteArray(decompressedBuffer.remaining())
        decompressedBuffer.get(decompressedBytes)

        assertArrayEquals(originalData, decompressedBytes)
    }

    @Test
    fun testZLibStandardCompressionAndDecompression() {
        val originalData = "Hello Minecraft standard ZLib compression!".toByteArray(Charsets.UTF_8)
        val compressedBuffer = ByteArray(originalData.size * 2 + 1024)
        val compressedLength = ZLib.compress(originalData, 0, originalData.size, compressedBuffer, 0, false)

        val compressedSlice = ByteBuffer.wrap(compressedBuffer, 0, compressedLength)
        val decompressedBuffer = ZLib.uncompress(compressedSlice, false)

        val decompressedBytes = ByteArray(decompressedBuffer.remaining())
        decompressedBuffer.get(decompressedBytes)

        assertArrayEquals(originalData, decompressedBytes)
    }
}
