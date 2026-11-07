package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtIO
import br.com.gamemods.nbtmanipulator.NbtString
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BedrockToBedrockPipelineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val ACTOR_PREFIX = "actorprefix".toByteArray(Charsets.US_ASCII)
    private val DIGP_PREFIX = "digp".toByteArray(Charsets.US_ASCII)

    @Test
    fun testBedrockToBedrockActorsAndDigpPreserved() {
        val inputDir = tempFolder.newFolder("be_input")
        val inputDb = File(inputDir, "db").apply { mkdirs() }

        val cowId = 1001L
        val zombieId = 1002L

        val cowActor = NbtCompound().apply {
            this["identifier"] = NbtString("minecraft:cow")
        }
        val zombieActor = NbtCompound().apply {
            this["identifier"] = NbtString("minecraft:zombie")
        }

        val cowBytes = serializeNbt(cowActor)
        val zombieBytes = serializeNbt(zombieActor)

        val options = Options().apply { createIfMissing(true) }
        Iq80DBFactory.factory.open(inputDb, options).use { db ->
            db.put(ACTOR_PREFIX + EntityLinks.storageKey(cowId), cowBytes)
            db.put(ACTOR_PREFIX + EntityLinks.storageKey(zombieId), zombieBytes)

            val digpKey = buildDigpKey(0, 5, 10)
            val digpValue = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).apply {
                putLong(cowId)
                putLong(zombieId)
            }.array()
            db.put(digpKey, digpValue)
        }

        val pipeline = BedrockToBedrockEntityPipeline()
        pipeline.scan(inputDir, listOf("overworld"))

        val outputDir = tempFolder.newFolder("be_output")
        val outputDb = File(outputDir, "db").apply { mkdirs() }
        Iq80DBFactory.factory.open(outputDb, options).close()

        val stats = pipeline.flushToLevelDb(outputDir)
        assertEquals(2, stats.totalActors)
        assertEquals(0, stats.skippedModded)
        assertEquals(1, stats.chunksWritten)

        Iq80DBFactory.factory.open(outputDb, options).use { db ->
            val outCow = db.get(ACTOR_PREFIX + EntityLinks.storageKey(cowId))
            assertNotNull(outCow)
            val parsedCow = NbtIO.readNbtFile(ByteArrayInputStream(outCow), compressed = false, littleEndian = true)
            assertEquals("minecraft:cow", parsedCow.compound.getNullableString("identifier"))

            val outZombie = db.get(ACTOR_PREFIX + EntityLinks.storageKey(zombieId))
            assertNotNull(outZombie)

            val outDigp = db.get(buildDigpKey(0, 5, 10))
            assertNotNull(outDigp)
            assertEquals(16, outDigp.size)
        }
    }

    @Test
    fun testBedrockToBedrockModdedActorSkipped() {
        val inputDir = tempFolder.newFolder("be_input_modded")
        val inputDb = File(inputDir, "db").apply { mkdirs() }

        val horseId = 2001L
        val moddedId = 2002L

        val horseActor = NbtCompound().apply {
            this["identifier"] = NbtString("minecraft:horse")
        }
        val moddedActor = NbtCompound().apply {
            this["identifier"] = NbtString("custom_addon:laser_robot")
        }

        val options = Options().apply { createIfMissing(true) }
        Iq80DBFactory.factory.open(inputDb, options).use { db ->
            db.put(ACTOR_PREFIX + EntityLinks.storageKey(horseId), serializeNbt(horseActor))
            db.put(ACTOR_PREFIX + EntityLinks.storageKey(moddedId), serializeNbt(moddedActor))

            val digpKey = buildDigpKey(0, 1, 1)
            val digpValue = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).apply {
                putLong(horseId)
                putLong(moddedId)
            }.array()
            db.put(digpKey, digpValue)
        }

        val pipeline = BedrockToBedrockEntityPipeline()
        pipeline.scan(inputDir, listOf("overworld"))

        val outputDir = tempFolder.newFolder("be_output_modded")
        val outputDb = File(outputDir, "db").apply { mkdirs() }
        Iq80DBFactory.factory.open(outputDb, options).close()

        val stats = pipeline.flushToLevelDb(outputDir)
        assertEquals(1, stats.totalActors)
        assertEquals(1, stats.skippedModded)
        assertEquals(1, stats.chunksWritten)

        Iq80DBFactory.factory.open(outputDb, options).use { db ->
            assertNotNull(db.get(ACTOR_PREFIX + EntityLinks.storageKey(horseId)))
            assertNull(db.get(ACTOR_PREFIX + EntityLinks.storageKey(moddedId)))

            val digp = db.get(buildDigpKey(0, 1, 1))
            assertNotNull(digp)
            assertEquals(8, digp.size)
            val remainingId = ByteBuffer.wrap(digp).order(ByteOrder.LITTLE_ENDIAN).long
            assertEquals(horseId, remainingId)
        }
    }

    @Test
    fun testBedrockToBedrockDimensionFiltering() {
        val inputDir = tempFolder.newFolder("be_input_dim")
        val inputDb = File(inputDir, "db").apply { mkdirs() }

        val overworldActorId = 3001L
        val netherActorId = 3002L

        val options = Options().apply { createIfMissing(true) }
        Iq80DBFactory.factory.open(inputDb, options).use { db ->
            db.put(ACTOR_PREFIX + EntityLinks.storageKey(overworldActorId), serializeNbt(NbtCompound().apply { this["identifier"] = NbtString("minecraft:pig") }))
            db.put(ACTOR_PREFIX + EntityLinks.storageKey(netherActorId), serializeNbt(NbtCompound().apply { this["identifier"] = NbtString("minecraft:blaze") }))

            db.put(buildDigpKey(0, 0, 0), ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(overworldActorId).array())
            db.put(buildDigpKey(1, 0, 0), ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(netherActorId).array())
        }

        val pipeline = BedrockToBedrockEntityPipeline()
        pipeline.scan(inputDir, listOf("overworld"))

        val outputDir = tempFolder.newFolder("be_output_dim")
        val outputDb = File(outputDir, "db").apply { mkdirs() }
        Iq80DBFactory.factory.open(outputDb, options).close()

        val stats = pipeline.flushToLevelDb(outputDir)
        assertEquals(1, stats.totalActors)
        assertEquals(1, stats.chunksWritten)

        Iq80DBFactory.factory.open(outputDb, options).use { db ->
            assertNotNull(db.get(ACTOR_PREFIX + EntityLinks.storageKey(overworldActorId)))
            assertNull(db.get(ACTOR_PREFIX + EntityLinks.storageKey(netherActorId)))
            assertNotNull(db.get(buildDigpKey(0, 0, 0)))
            assertNull(db.get(buildDigpKey(1, 0, 0)))
        }
    }

    private fun serializeNbt(tag: NbtCompound): ByteArray {
        val baos = ByteArrayOutputStream()
        NbtIO.writeNbtFile(baos, NbtFile("", tag), compressed = false, littleEndian = true)
        return baos.toByteArray()
    }

    private fun buildDigpKey(dim: Int, cx: Int, cz: Int): ByteArray {
        val hasDim = dim != 0
        val buf = ByteBuffer.allocate(4 + 4 + 4 + if (hasDim) 4 else 0).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(DIGP_PREFIX)
        buf.putInt(cx)
        buf.putInt(cz)
        if (hasDim) buf.putInt(dim)
        return buf.array()
    }
}
