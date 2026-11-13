package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.NbtIO
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class Issue1AnalysisTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testIssue1EntitiesExtractionAndFlush() {
        var lctDir = File("../issue_1_test/lct")
        if (!lctDir.exists()) lctDir = File("issue_1_test/lct")
        assertTrue("lct directory should exist", lctDir.exists())

        val logs = mutableListOf<String>()
        val pipeline = EntityConversionPipeline { log ->
            logs.add(log)
            println("[Pipeline Log] $log")
        }

        pipeline.preExtract(lctDir, listOf("overworld"))

        // Verify Chunk A (-7, 6) has the sheep
        val chunkA = EntityConversionPipeline.ChunkKey(0, -7, 6)
        val chunkARecords = pipeline.stagedChunks[chunkA]
        assertNotNull("Chunk A (-7, 6) should have staged records", chunkARecords)
        assertTrue("Chunk A should have at least 1 entity", chunkARecords!!.isNotEmpty())

        val sheepNbt = NbtIO.readNbtFile(ByteArrayInputStream(chunkARecords[0].nbtBytes), compressed = false, littleEndian = true).compound
        assertEquals("minecraft:sheep", sheepNbt.getNullableString("identifier"))
        assertEquals(1.toByte(), sheepNbt.getNullableByte("Persistent"))

        // Verify Chunk B (-6, 6) has the goats
        val chunkB = EntityConversionPipeline.ChunkKey(0, -6, 6)
        val chunkBRecords = pipeline.stagedChunks[chunkB]
        assertNotNull("Chunk B (-6, 6) should have staged records", chunkBRecords)
        assertTrue("Chunk B should have at least 1 entity", chunkBRecords!!.isNotEmpty())

        // Verify leash knot registration at fence [-90, -60, 100]
        val expectedKnotId = EntityLinks.knotId(-90L, -60L, 100L)
        val sheepLeasherId = sheepNbt.getNullableLong("LeasherID")
        assertEquals("Sheep should be leashed to knot", expectedKnotId, sheepLeasherId)

        // Verify pending knots in chunk B (-6, 6)
        val knotsInChunkB = pipeline.pendingKnots[chunkB]
        assertNotNull("Chunk B should have pending knot", knotsInChunkB)
        assertTrue("Pending knot should match expected knotId", knotsInChunkB!!.any { it.knotId == expectedKnotId })

        // Test flushing to LevelDB
        val dummyWorldDir = tempFolder.newFolder("world")
        val dbDir = File(dummyWorldDir, "db")
        dbDir.mkdirs()

        // Initialize LevelDB
        val initOptions = Options().createIfMissing(true)
        Iq80DBFactory.factory.open(dbDir, initOptions).use { it.put("init".toByteArray(), "1".toByteArray()) }

        val stats = pipeline.flushToLevelDb(dummyWorldDir)
        println("Flushed stats: totalActors=${stats.totalActors}, chunks=${stats.chunksWritten}")
        assertTrue("Should write at least 300 actors", stats.totalActors >= 300)

        // Verify LevelDB content
        val verifyOptions = Options().createIfMissing(false)
        Iq80DBFactory.factory.open(dbDir, verifyOptions).use { db ->
            // Verify knot actor exists
            val knotStorageKey = EntityLinks.storageKey(expectedKnotId)
            val knotActorBytes = db.get("actorprefix".toByteArray() + knotStorageKey)
            assertNotNull("Knot actor should exist in LevelDB", knotActorBytes)

            val knotActor = NbtIO.readNbtFile(ByteArrayInputStream(knotActorBytes), compressed = false, littleEndian = true).compound
            assertEquals("minecraft:leash_knot", knotActor.getNullableString("identifier"))
            val knotPos = knotActor.getNullableFloatList("Pos")
            assertNotNull(knotPos)
            assertEquals(-89.5f, knotPos!![0].value, 0.001f) // -90 + 0.5
            assertEquals(-59.75f, knotPos[1].value, 0.001f) // -60 + 0.25
            assertEquals(100.5f, knotPos[2].value, 0.001f) // 100 + 0.5

            // Verify digp for Chunk B contains knot's storage key
            val digpBKey = pipeline.buildDigpKey(0, -6, 6)
            val digpBBytes = db.get(digpBKey)
            assertNotNull("digp for Chunk B should exist", digpBBytes)
            assertTrue("digp for Chunk B should contain knot storageKey",
                digpBBytes.toList().windowed(8, 8).any { it.toByteArray().contentEquals(knotStorageKey) }
            )
        }
    }
}
