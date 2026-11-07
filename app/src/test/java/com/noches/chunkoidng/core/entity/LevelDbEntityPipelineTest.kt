package com.noches.chunkoidng.core.entity

import org.iq80.leveldb.CompressionType
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LevelDbEntityPipelineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDigpKeyLayout() {
        val pipeline = EntityConversionPipeline()

        val overworldKey = pipeline.buildDigpKey(dimensionId = 0, chunkX = 10, chunkZ = -20)
        assertEquals(12, overworldKey.size)

        val prefix = String(overworldKey, 0, 4)
        assertEquals("digp", prefix)

        val buf = ByteBuffer.wrap(overworldKey, 4, 8).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(10, buf.int)
        assertEquals(-20, buf.int)

        val netherKey = pipeline.buildDigpKey(dimensionId = 1, chunkX = 5, chunkZ = 8)
        assertEquals(16, netherKey.size)

        val netherBuf = ByteBuffer.wrap(netherKey, 4, 12).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(5, netherBuf.int)
        assertEquals(8, netherBuf.int)
        assertEquals(1, netherBuf.int)
    }

    @Test
    fun testLevelDbBatchWriteAndRead() {
        val dbDir = tempFolder.newFolder("db")
        val options = Options()
        options.compressionType(CompressionType.ZLIB_RAW)
        options.createIfMissing(true)

        val db = Iq80DBFactory.factory.open(dbDir, options)
        try {
            val batch = db.createWriteBatch()
            val sampleStorageKey = EntityLinks.storageKey(88888L)
            val actorKey = "actorprefix".toByteArray() + sampleStorageKey
            val dummyNbt = byteArrayOf(0x0A, 0x00, 0x00, 0x00)
            batch.put(actorKey, dummyNbt)

            val digpKey = EntityConversionPipeline().buildDigpKey(0, 1, 2)
            batch.put(digpKey, sampleStorageKey)
            db.write(batch)
            batch.close()

            val readActor = db.get(actorKey)
            assertNotNull(readActor)
            assertEquals(dummyNbt.size, readActor.size)

            val readDigp = db.get(digpKey)
            assertNotNull(readDigp)
            assertEquals(8, readDigp.size)
        } finally {
            db.close()
        }
    }
}
