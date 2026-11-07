package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class EntityFuzzingAndRobustnessTest {

    @Test
    fun testModdedAndUnknownEntitiesAreSilentlyFiltered() {
        val modEntities = listOf(
            "twilightforest:hydra",
            "alexsmobs:komodo_dragon",
            "mekanism:robotic_drone",
            "botania:doppleganger",
            "create:carriage_contraption",
            "pixelmon:pikachu",
            "custom_namespace:custom_mob",
            "",
            "invalid_format_without_colon"
        )

        for (modId in modEntities) {
            val javaEntity = NbtCompound()
            javaEntity["id"] = NbtString(modId)
            val result = BedrockActorBuilder.build(javaEntity, uniqueId = 9999L)
            assertNull("Modded entity $modId must be filtered out and return null", result)
        }
    }

    @Test
    fun testCorruptedAndExtremeNbtDoesNotCrash() {
        val corruptedEntities = listOf(

            NbtCompound(),

            NbtCompound().apply {
                this["id"] = NbtString("minecraft:zombie")
            },

            NbtCompound().apply {
                this["id"] = NbtString("minecraft:skeleton")
                val posList = NbtList<NbtDouble>()
                posList.add(NbtDouble(10.5))
                this["Pos"] = posList
            },

            NbtCompound().apply {
                this["id"] = NbtString("minecraft:cow")
                this["Health"] = NbtFloat(-500f)
            },

            NbtCompound().apply {
                this["id"] = NbtString("minecraft:pig")
                this["Age"] = NbtInt(-24000)
                this["IsBaby"] = NbtByte(1)
            },

            NbtCompound().apply {
                this["id"] = NbtString("minecraft:creeper")
                this["CustomName"] = NbtString("{\"bad_json: unclosed")
            }
        )

        for ((idx, entity) in corruptedEntities.withIndex()) {
            val actor = BedrockActorBuilder.build(entity, uniqueId = 1000L + idx)
            val id = (entity["id"] as? NbtString)?.value
            if (id == null) {
                assertNull(actor)
            } else {
                assertNotNull("Should safely construct bedrock actor for $id even with extreme NBT", actor)

                assertEquals(1000L + idx, actor!!.getNullableLong("UniqueID"))
                assertNotNull("Pos must always exist", actor.getNullableFloatList("Pos"))
                assertEquals(3, actor.getNullableFloatList("Pos")!!.size)
                assertNotNull("Rotation must always exist", actor.getNullableFloatList("Rotation"))
                assertEquals(2, actor.getNullableFloatList("Rotation")!!.size)
                assertNotNull("Attributes must contain health", actor.getNullableCompoundList("Attributes"))
            }
        }
    }

    @Test
    fun testVillagerAndZombieVillagerData() {
        val villager = NbtCompound().apply {
            this["id"] = NbtString("minecraft:villager")
            val vData = NbtCompound()
            vData["profession"] = NbtString("minecraft:armorer")
            vData["type"] = NbtString("minecraft:taiga")
            vData["level"] = NbtInt(3)
            this["VillagerData"] = vData
            this["Xp"] = NbtInt(150)
        }

        val actor = BedrockActorBuilder.build(villager, 2024L)
        assertNotNull(actor)
        assertEquals("minecraft:villager_v2", actor!!.getNullableString("identifier"))
        assertEquals(2, actor.getNullableInt("TradeTier"))
        assertEquals(150, actor.getNullableInt("TradeExperience"))

        val defs = actor.getNullableStringList("definitions")?.map { it.value } ?: emptyList()
        assertTrue(defs.contains("+minecraft:armorer"))
        assertTrue(defs.contains("+minecraft:taiga_villager"))
    }

    @Test
    fun testTamedWolfWithCollarAndOwner() {
        val wolf = NbtCompound().apply {
            this["id"] = NbtString("minecraft:wolf")
            this["Owner"] = NbtIntArray(intArrayOf(1, 2, 3, 4))
            this["CollarColor"] = NbtByte(14)
            this["Sitting"] = NbtByte(1)
            this["Variant"] = NbtString("minecraft:ashen")
        }

        val actor = BedrockActorBuilder.build(wolf, 3030L)
        assertNotNull(actor)
        assertEquals("minecraft:wolf", actor!!.getNullableString("identifier"))
        assertEquals(14.toByte(), actor.getNullableByte("CollarColor"))
        assertEquals(1.toByte(), actor.getNullableByte("Sitting"))

        val defs = actor.getNullableStringList("definitions")?.map { it.value } ?: emptyList()
        assertTrue(defs.contains("+minecraft:wolf_ashen"))
    }

    @Test
    fun testDeepPassengerTreeFlattening() {

        val spider = NbtCompound().apply {
            this["id"] = NbtString("minecraft:spider")
            this["UUID"] = NbtIntArray(intArrayOf(10, 20, 30, 40))
        }
        val skeleton = NbtCompound().apply {
            this["id"] = NbtString("minecraft:skeleton")
            this["UUID"] = NbtIntArray(intArrayOf(11, 21, 31, 41))
            val pList = NbtList<NbtCompound>()
            pList.add(spider)
            this["Passengers"] = pList
        }
        val zombie = NbtCompound().apply {
            this["id"] = NbtString("minecraft:zombie")
            this["UUID"] = NbtIntArray(intArrayOf(12, 22, 32, 42))
            this["IsBaby"] = NbtByte(1)
            val pList = NbtList<NbtCompound>()
            pList.add(skeleton)
            this["Passengers"] = pList
        }
        val chicken = NbtCompound().apply {
            this["id"] = NbtString("minecraft:chicken")
            this["UUID"] = NbtIntArray(intArrayOf(13, 23, 33, 43))
            val pList = NbtList<NbtCompound>()
            pList.add(zombie)
            this["Passengers"] = pList
        }

        val pipeline = EntityConversionPipeline()
        val chunkKey = EntityConversionPipeline.ChunkKey(dimensionId = 0, chunkX = 5, chunkZ = 6)
        pipeline.processEntity(chunkKey, chicken, vehicleId = 0L)

        val records = pipeline.stagedChunks[chunkKey]
        assertNotNull(records)

        assertEquals(4, records!!.size)

        val parsedActors = records.map { rec ->
            val bis = ByteArrayInputStream(rec.nbtBytes)
            val nbtFile = NbtIO.readNbtFile(bis, compressed = false, littleEndian = true)
            rec to nbtFile.compound
        }

        val chickenActor = parsedActors.find { it.second.getNullableString("identifier") == "minecraft:chicken" }
        val zombieActor = parsedActors.find { it.second.getNullableString("identifier") == "minecraft:zombie" }
        val skeletonActor = parsedActors.find { it.second.getNullableString("identifier") == "minecraft:skeleton" }
        val spiderActor = parsedActors.find { it.second.getNullableString("identifier") == "minecraft:spider" }

        assertNotNull(chickenActor)
        assertNotNull(zombieActor)
        assertNotNull(skeletonActor)
        assertNotNull(spiderActor)

        val zombieLinks = zombieActor!!.second.getNullableCompoundList("LinksTag")
        assertNotNull("Zombie must have LinksTag", zombieLinks)
        assertTrue(zombieLinks!!.any { it.getNullableLong("VehicleID") == chickenActor!!.first.uniqueId })
    }

    @Test
    fun testLevelDbBatchWriterAndReaderRoundTrip() {
        val tempWorldDir = File.createTempFile("test_bedrock_fuzz_", "_dir")
        tempWorldDir.delete()
        tempWorldDir.mkdirs()
        val dbDir = File(tempWorldDir, "db")
        dbDir.mkdirs()

        val createOpts = org.iq80.leveldb.Options().createIfMissing(true)
        org.iq80.leveldb.impl.Iq80DBFactory.factory.open(dbDir, createOpts).close()

        try {
            val pipeline = EntityConversionPipeline()

            val testIds = listOf(
                "minecraft:creeper", "minecraft:skeleton", "minecraft:spider",
                "minecraft:zombie", "minecraft:pig", "minecraft:sheep",
                "minecraft:cow", "minecraft:chicken", "minecraft:horse",
                "minecraft:villager", "minecraft:item", "minecraft:tnt"
            )

            for (i in 0 until 50) {
                val entity = NbtCompound()
                entity["id"] = NbtString(testIds[i % testIds.size])
                entity["UUID"] = NbtIntArray(intArrayOf(i, i + 10, i + 20, i + 30))
                val pos = NbtList<NbtDouble>()
                pos.add(NbtDouble(i * 1.5))
                pos.add(NbtDouble(64.0))
                pos.add(NbtDouble(i * 2.0))
                entity["Pos"] = pos
                if (testIds[i % testIds.size] == "minecraft:item") {
                    val it = NbtCompound()
                    it["id"] = NbtString("minecraft:apple")
                    it["Count"] = NbtByte(5)
                    entity["Item"] = it
                }
                val chunkKey = EntityConversionPipeline.ChunkKey(
                    dimensionId = i % 3,
                    chunkX = i / 10,
                    chunkZ = i / 10
                )
                pipeline.processEntity(chunkKey, entity, vehicleId = 0L)
            }

            val stats = pipeline.flushToLevelDb(tempWorldDir)
            assertEquals(50, stats.totalActors)
            assertTrue(stats.chunksIndexed > 0)

            val factory = org.iq80.leveldb.impl.Iq80DBFactory.factory
            val options = org.iq80.leveldb.Options().createIfMissing(false)
            factory.open(dbDir, options).use { db ->
                var actorKeyCount = 0
                var digpKeyCount = 0

                val iterator = db.iterator()
                iterator.seekToFirst()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val key = entry.key
                    val value = entry.value

                    if (key.size == 19 && String(key, 0, 11, Charsets.US_ASCII) == "actorprefix") {
                        actorKeyCount++

                        val bis = ByteArrayInputStream(value)
                        val readNbtFile = NbtIO.readNbtFile(bis, compressed = false, littleEndian = true)
                        val readActor = readNbtFile.compound
                        assertNotNull(readActor)
                        assertNotNull(readActor.getNullableString("identifier"))
                        assertNotNull(readActor.getNullableLong("UniqueID"))
                        assertNotNull(readActor.getNullableFloatList("Pos"))
                        assertEquals(3, readActor.getNullableFloatList("Pos")!!.size)
                    } else if (key.size >= 12 && String(key, 0, 4, Charsets.US_ASCII) == "digp") {
                        digpKeyCount++

                        assertEquals(0, value.size % 8)
                    }
                }
                iterator.close()

                assertEquals(50, actorKeyCount)
                assertTrue("Must contain digp chunk keys", digpKeyCount > 0)
            }
        } finally {
            tempWorldDir.deleteRecursively()
        }
    }
}
