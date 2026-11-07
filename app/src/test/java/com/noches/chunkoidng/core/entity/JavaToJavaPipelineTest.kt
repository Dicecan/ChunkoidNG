package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.AnvilRegionFile
import br.com.gamemods.nbtmanipulator.NbtByte
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtDouble
import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtFloat
import br.com.gamemods.nbtmanipulator.NbtInt
import br.com.gamemods.nbtmanipulator.NbtIntArray
import br.com.gamemods.nbtmanipulator.NbtList
import br.com.gamemods.nbtmanipulator.NbtString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class JavaToJavaPipelineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testJavaToJavaModernEntitiesTransfer() {
        val inputDir = tempFolder.newFolder("java_input")
        val entitiesDir = File(inputDir, "entities").apply { mkdirs() }
        val mcaFile = File(entitiesDir, "r.0.0.mca")

        val chunkCompound = NbtCompound().apply {
            this["Position"] = NbtIntArray(intArrayOf(0, 0))
            this["DataVersion"] = NbtInt(3700)
            val list = NbtList<NbtCompound>()

            val cow = NbtCompound().apply {
                this["id"] = NbtString("minecraft:cow")
                this["Health"] = NbtFloat(10.0f)
                this["Pos"] = NbtList(listOf(NbtDouble(10.0), NbtDouble(64.0), NbtDouble(10.0)))
            }
            val sheep = NbtCompound().apply {
                this["id"] = NbtString("minecraft:sheep")
                this["Health"] = NbtFloat(8.0f)
                this["Color"] = NbtByte(0)
                this["Pos"] = NbtList(listOf(NbtDouble(12.0), NbtDouble(64.0), NbtDouble(12.0)))
            }
            val modded = NbtCompound().apply {
                this["id"] = NbtString("mod_entities:alien_creature")
                this["Health"] = NbtFloat(50.0f)
            }

            list.add(cow)
            list.add(sheep)
            list.add(modded)
            this["Entities"] = list
        }

        AnvilRegionFile(mcaFile).use { rf ->
            rf.writeChunkNbt(0, 0, NbtFile("", chunkCompound))
        }

        val pipeline = JavaToJavaEntityPipeline()
        pipeline.scan(inputDir, listOf("overworld"), "JAVA_1_21_4")

        val outputDir = tempFolder.newFolder("java_output")
        val stats = pipeline.flushToJava(outputDir, "JAVA_1_21_4", inputDir)

        assertEquals(2, stats.totalActors)
        assertEquals(1, stats.skippedModded)
        assertEquals(1, stats.chunksWritten)

        val outMca = File(outputDir, "entities/r.0.0.mca")
        assertTrue(outMca.exists())

        AnvilRegionFile(outMca).use { rf ->
            val nbt = rf.readChunkNbt(0, 0)
            assertNotNull(nbt)
            val list = nbt!!.compound.getNullableCompoundList("Entities")
            assertNotNull(list)
            assertEquals(2, list!!.size)
            assertEquals("minecraft:cow", list[0].getNullableString("id"))
            assertEquals("minecraft:sheep", list[1].getNullableString("id"))
            assertEquals(3953, nbt.compound.getNullableInt("DataVersion"))
        }
    }

    @Test
    fun testJavaEquipmentAdaptationLegacyToModern() {
        val inputDir = tempFolder.newFolder("java_input_equip")
        val entitiesDir = File(inputDir, "entities").apply { mkdirs() }
        val mcaFile = File(entitiesDir, "r.0.0.mca")

        val sword = NbtCompound().apply {
            this["id"] = NbtString("minecraft:diamond_sword")
            this["Count"] = NbtByte(1)
        }
        val shield = NbtCompound().apply {
            this["id"] = NbtString("minecraft:shield")
            this["Count"] = NbtByte(1)
        }
        val boots = NbtCompound().apply {
            this["id"] = NbtString("minecraft:iron_boots")
            this["Count"] = NbtByte(1)
        }

        val zombie = NbtCompound().apply {
            this["id"] = NbtString("minecraft:zombie")
            this["Pos"] = NbtList(listOf(NbtDouble(0.0), NbtDouble(64.0), NbtDouble(0.0)))
            this["HandItems"] = NbtList(listOf(sword, shield))
            this["ArmorItems"] = NbtList(listOf(boots, NbtCompound(), NbtCompound(), NbtCompound()))
        }

        val chunkCompound = NbtCompound().apply {
            this["Position"] = NbtIntArray(intArrayOf(0, 0))
            this["Entities"] = NbtList(listOf(zombie))
        }

        AnvilRegionFile(mcaFile).use { rf ->
            rf.writeChunkNbt(0, 0, NbtFile("", chunkCompound))
        }

        val pipeline = JavaToJavaEntityPipeline()
        pipeline.scan(inputDir, listOf("overworld"), "JAVA_1_21_4")

        val outputDir = tempFolder.newFolder("java_output_equip")
        val stats = pipeline.flushToJava(outputDir, "JAVA_1_21_4", inputDir)
        assertEquals(1, stats.totalActors)

        val outMca = File(outputDir, "entities/r.0.0.mca")
        AnvilRegionFile(outMca).use { rf ->
            val outZombie = rf.readChunkNbt(0, 0)!!.compound.getNullableCompoundList("Entities")!![0]
            val equip = outZombie.getNullableCompound("equipment")
            assertNotNull(equip)
            assertEquals("minecraft:diamond_sword", equip!!.getNullableCompound("mainhand")?.getNullableString("id"))
            assertEquals("minecraft:shield", equip.getNullableCompound("offhand")?.getNullableString("id"))
            assertEquals("minecraft:iron_boots", equip.getNullableCompound("feet")?.getNullableString("id"))
        }
    }

    @Test
    fun testJavaEquipmentAdaptationModernToLegacy() {
        val inputDir = tempFolder.newFolder("java_input_modern")
        val entitiesDir = File(inputDir, "entities").apply { mkdirs() }
        val mcaFile = File(entitiesDir, "r.0.0.mca")

        val skeleton = NbtCompound().apply {
            this["id"] = NbtString("minecraft:skeleton")
            this["Pos"] = NbtList(listOf(NbtDouble(0.0), NbtDouble(64.0), NbtDouble(0.0)))
            val equip = NbtCompound().apply {
                this["mainhand"] = NbtCompound().apply {
                    this["id"] = NbtString("minecraft:bow")
                    this["Count"] = NbtByte(1)
                }
                this["head"] = NbtCompound().apply {
                    this["id"] = NbtString("minecraft:iron_helmet")
                    this["Count"] = NbtByte(1)
                }
            }
            this["equipment"] = equip
        }

        val chunkCompound = NbtCompound().apply {
            this["Position"] = NbtIntArray(intArrayOf(0, 0))
            this["Entities"] = NbtList(listOf(skeleton))
        }

        AnvilRegionFile(mcaFile).use { rf ->
            rf.writeChunkNbt(0, 0, NbtFile("", chunkCompound))
        }

        val pipeline = JavaToJavaEntityPipeline()
        pipeline.scan(inputDir, listOf("overworld"), "JAVA_1_16_5")

        val outputDir = tempFolder.newFolder("java_output_legacy")
        val regDir = File(outputDir, "region").apply { mkdirs() }
        val destMca = File(regDir, "r.0.0.mca")
        AnvilRegionFile(destMca).use { rf ->
            val levelCompound = NbtCompound().apply {
                this["Level"] = NbtCompound().apply {
                    this["xPos"] = NbtInt(0)
                    this["zPos"] = NbtInt(0)
                    this["Entities"] = NbtList<NbtCompound>()
                }
            }
            rf.writeChunkNbt(0, 0, NbtFile("", levelCompound))
        }

        val stats = pipeline.flushToJava(outputDir, "JAVA_1_16_5", inputDir)
        assertEquals(1, stats.totalActors)

        AnvilRegionFile(destMca).use { rf ->
            val entities = rf.readChunkNbt(0, 0)!!.compound.getNullableCompound("Level")!!.getNullableCompoundList("Entities")!!
            assertEquals(1, entities.size)
            val outSkel = entities[0]
            assertNull(outSkel.getNullableCompound("equipment"))
            val hands = outSkel.getNullableCompoundList("HandItems")
            assertNotNull(hands)
            assertEquals("minecraft:bow", hands!![0].getNullableString("id"))
            val armors = outSkel.getNullableCompoundList("ArmorItems")
            assertNotNull(armors)
            assertEquals("minecraft:iron_helmet", armors!![3].getNullableString("id"))
        }
    }
}
