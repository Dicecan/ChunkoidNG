package com.noches.chunkoidng

import com.noches.chunkoidng.core.leveldb.BedrockLevelDbHelper
import com.noches.chunkoidng.core.leveldb.SafeEnv
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.DbImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files

class BedrockLevelDbHelperTest {

    @Test
    fun chunkDeletionKeepsCoordinateOnlyAndUnknownKeys() {
        val folder = Files.createTempDirectory("chunkoid-leveldb-test").toFile()
        try {
            DbImpl(Options().apply { createIfMissing(true) }, folder.absolutePath, SafeEnv()).use { database ->
                database.put(chunkKey(4, -7, 0x31), byteArrayOf(1))
                database.put(chunkKey(4, -7, 0x7E), byteArrayOf(2))
                database.put(coordinateOnlyKey(4, -7), byteArrayOf(3))
            }

            BedrockLevelDbHelper(folder).use { helper ->
                assertEquals(1, helper.deleteChunk(4, -7, 0))
                assertNull(helper.get(chunkKey(4, -7, 0x31)))
                assertNotNull(helper.get(chunkKey(4, -7, 0x7E)))
                assertNotNull(helper.get(coordinateOnlyKey(4, -7)))
            }
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun bulkScanningWithoutBlockCacheDoesNotThrow() {
        val folder = Files.createTempDirectory("chunkoid-leveldb-cache-test").toFile()
        try {
            DbImpl(Options().apply { createIfMissing(true); writeBufferSize(4 * 1024) }, folder.absolutePath, SafeEnv()).use { database ->
                for (i in 0 until 500) {
                    val key = chunkKey(i % 50, i / 50, 0x31)
                    val value = ByteArray(1024) { (it % 128).toByte() }
                    database.put(key, value)
                }
            }

            BedrockLevelDbHelper(folder).use { helper ->
                val records = helper.getAllRecords()
                assertEquals(500, records.size)

                val summary = helper.getChunkCoordinateSummary(0)
                assertEquals(500, summary.blockEntities.size)

                val record = helper.get(chunkKey(0, 0, 0x31))
                assertNotNull(record)
                assertEquals(1024, record?.size)
            }
        } finally {
            folder.deleteRecursively()
        }
    }

    private fun chunkKey(x: Int, z: Int, tag: Int): ByteArray {
        return coordinateOnlyKey(x, z) + byteArrayOf(tag.toByte())
    }

    private fun coordinateOnlyKey(x: Int, z: Int): ByteArray {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(x)
            .putInt(z)
            .array()
    }
}
