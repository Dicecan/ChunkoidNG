package com.noches.chunkoidng.core.leveldb

import org.iq80.leveldb.CompressionType
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.DbImpl
import org.iq80.leveldb.util.Slices
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SafeEnvTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun safeEnvAvoidsMmapFilesOnReadAndWrite() {
        val env = SafeEnv()
        val ioFile = tempFolder.newFile("sample.ldb")
        val envFile = env.toFile(ioFile.absolutePath)
        val writable = env.newWritableFile(envFile)
        writable.append(Slices.wrappedBuffer("hello-leveldb".toByteArray()))
        writable.close()

        val randomInput = env.newRandomAccessFile(envFile)
        try {
            assertFalse(
                "SafeEnv must not allocate MMWritableFile on Android",
                writable::class.java.simpleName.startsWith("MM")
            )
            assertFalse(
                "SafeEnv must not allocate MMRandomInputFile (avoids MappedByteBuffer.duplicate NoSuchMethodError)",
                randomInput::class.java.simpleName.startsWith("MM")
            )
        } finally {
            randomInput.close()
        }
    }

    @Test
    fun safeEnvDbImplWritesReopensAndIteratesSstables() {
        val dbDir = tempFolder.newFolder("db")
        val options = Options().apply {
            createIfMissing(true)
            compressionType(CompressionType.ZLIB_RAW)
            writeBufferSize(64 * 1024)
        }

        val payload = ByteArray(1024) { (it % 251).toByte() }
        val entryCount = 200

        DbImpl(options, dbDir.absolutePath, SafeEnv()).use { db ->
            val batch = db.createWriteBatch()
            for (i in 0 until entryCount) {
                val key = "actorprefix_${i.toString().padStart(4, '0')}".toByteArray()
                batch.put(key, payload)
            }
            db.write(batch)
            batch.close()
        }

        DbImpl(options, dbDir.absolutePath, SafeEnv()).use { reopened ->
            val firstValue = reopened.get("actorprefix_0000".toByteArray())
            assertArrayEquals(payload, firstValue)

            var iterated = 0
            reopened.iterator().use { it ->
                it.seekToFirst()
                while (it.hasNext()) {
                    it.next()
                    iterated++
                }
            }
            assertEquals(entryCount, iterated)
        }
    }
}
