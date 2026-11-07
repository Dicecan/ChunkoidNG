package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.*
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

class BedrockToJavaPipelineTest {

    @Test
    fun testPassengerTreeReconstruction() {
        val chicken = NbtCompound().apply {
            this["identifier"] = NbtString("minecraft:chicken")
            this["UniqueID"] = NbtLong(100L)
        }
        val zombie = NbtCompound().apply {
            this["identifier"] = NbtString("minecraft:zombie")
            this["UniqueID"] = NbtLong(200L)
            val links = NbtList<NbtCompound>().apply {
                val link = NbtCompound()
                link["RideID"] = NbtLong(200L)
                link["VehicleID"] = NbtLong(100L)
                add(link)
            }
            this["LinksTag"] = links
        }
        val spider = NbtCompound().apply {
            this["identifier"] = NbtString("minecraft:spider")
            this["UniqueID"] = NbtLong(300L)
            val links = NbtList<NbtCompound>().apply {
                val link = NbtCompound()
                link["RideID"] = NbtLong(300L)
                link["VehicleID"] = NbtLong(200L)
                add(link)
            }
            this["LinksTag"] = links
        }

        val pipeline = BedrockToJavaEntityPipeline()
        val cKey = BedrockToJavaEntityPipeline.ChunkKey(0, 5, 5)

        val jChicken = JavaEntityBuilder.build(chicken)!!
        val jZombie = JavaEntityBuilder.build(zombie)!!
        val jSpider = JavaEntityBuilder.build(spider)!!

        val staged = listOf(
            BedrockToJavaEntityPipeline.StagedEntity(100L, 0L, jChicken),
            BedrockToJavaEntityPipeline.StagedEntity(200L, 100L, jZombie),
            BedrockToJavaEntityPipeline.StagedEntity(300L, 200L, jSpider)
        )

        val roots = pipeline.reconstructPassengersTree(staged)

        assertEquals(1, roots.size)
        val root = roots[0]
        assertEquals("minecraft:chicken", root.getNullableString("id"))

        val chickenPassengers = root.getNullableCompoundList("Passengers")
        assertNotNull(chickenPassengers)
        assertEquals(1, chickenPassengers!!.size)
        val pZombie = chickenPassengers[0]
        assertEquals("minecraft:zombie", pZombie.getNullableString("id"))

        val zombiePassengers = pZombie.getNullableCompoundList("Passengers")
        assertNotNull(zombiePassengers)
        assertEquals(1, zombiePassengers!!.size)
        val pSpider = zombiePassengers[0]
        assertEquals("minecraft:spider", pSpider.getNullableString("id"))
    }

    @Test
    fun testLevelDbScanAndFlushToJavaAnvil() {
        val tempDir = File.createTempFile("test_be2je_", "_dir")
        tempDir.delete()
        tempDir.mkdirs()

        val bedrockDir = File(tempDir, "bedrock")
        val dbDir = File(bedrockDir, "db")
        dbDir.mkdirs()

        val javaTargetDir = File(tempDir, "java")
        javaTargetDir.mkdirs()

        try {

            val factory = Iq80DBFactory.factory
            val opts = Options().createIfMissing(true)
            factory.open(dbDir, opts).use { db ->
                val batch = db.createWriteBatch()

                val testActors = listOf("minecraft:cow", "minecraft:pig", "minecraft:creeper")
                for ((idx, id) in testActors.withIndex()) {
                    val uid = 1000L + idx
                    val actor = NbtCompound().apply {
                        this["identifier"] = NbtString(id)
                        this["UniqueID"] = NbtLong(uid)
                        val pos = NbtList<NbtFloat>().apply {
                            add(NbtFloat((idx * 32).toFloat()))
                            add(NbtFloat(70.0f))
                            add(NbtFloat((idx * 32).toFloat()))
                        }
                        this["Pos"] = pos
                    }

                    val baos = ByteArrayOutputStream()
                    NbtIO.writeNbtFile(baos, NbtFile("", actor), compressed = false, littleEndian = true)

                    val key = "actorprefix".toByteArray(Charsets.UTF_8) + EntityLinks.storageKey(uid)
                    batch.put(key, baos.toByteArray())
                }

                db.write(batch)
                batch.close()
            }

            val pipeline = BedrockToJavaEntityPipeline()
            pipeline.scan(bedrockDir)

            val stats = pipeline.flushToJava(javaTargetDir)
            assertEquals(3, stats.totalActors)
            assertTrue(stats.chunksWritten > 0)

            val entitiesDir = File(javaTargetDir, "entities")
            assertTrue("entities directory must be created", entitiesDir.isDirectory)

            val mcaFiles = entitiesDir.listFiles { _, name -> name.endsWith(".mca") }
            assertNotNull(mcaFiles)
            assertTrue("Must contain at least 1 region file", mcaFiles!!.isNotEmpty())

            val firstMca = mcaFiles[0]
            AnvilRegionFile(firstMca).use { rf ->
                val populated = rf.getPopulatedChunks()
                assertTrue(populated.isNotEmpty())

                val firstChunk = populated[0]
                val chunkNbt = rf.readChunkNbt(firstChunk.localX, firstChunk.localZ)
                assertNotNull(chunkNbt)
                val chunkRoot = chunkNbt!!.compound
                assertNotNull("Must contain Position", chunkRoot.getNullableIntArray("Position"))
                val entitiesList = chunkRoot.getNullableCompoundList("Entities")
                assertNotNull("Must contain Entities list", entitiesList)
                assertTrue("Chunk must contain entities", entitiesList!!.isNotEmpty())
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
