package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.*
import org.junit.Assert.*
import org.junit.Test

class JavaEntityBuilderTest {

    @Test
    fun testSheepConversion() {
        val bedrockSheep = NbtCompound().apply {
            this["identifier"] = NbtString("minecraft:sheep")
            this["UniqueID"] = NbtLong(123456789L)
            this["Color"] = NbtByte(14)
            this["Sheared"] = NbtByte(1)
            val pos = NbtList<NbtFloat>().apply {
                add(NbtFloat(10.5f))
                add(NbtFloat(64.0f))
                add(NbtFloat(-20.5f))
            }
            this["Pos"] = pos
            this["Health"] = NbtShort(8)
        }

        val javaSheep = JavaEntityBuilder.build(bedrockSheep)
        assertNotNull(javaSheep)
        assertEquals("minecraft:sheep", javaSheep!!.getNullableString("id"))
        assertEquals(14.toByte(), javaSheep.getNullableByte("Color"))
        assertEquals(1.toByte(), javaSheep.getNullableByte("Sheared"))
        assertEquals(8f, javaSheep.getNullableFloat("Health") ?: 0f, 0.001f)

        val pos = javaSheep.getNullableDoubleList("Pos")
        assertNotNull(pos)
        assertEquals(3, pos!!.size)
        assertEquals(10.5, pos[0].value, 0.001)
        assertEquals(64.0, pos[1].value, 0.001)
        assertEquals(-20.5, pos[2].value, 0.001)

        val uuid = javaSheep.getNullableIntArray("UUID")
        assertNotNull(uuid)
        assertEquals(4, uuid!!.size)
    }

    @Test
    fun testHorseVariantAndSaddle() {
        val bedrockHorse = NbtCompound().apply {
            this["identifier"] = NbtString("minecraft:horse")
            this["UniqueID"] = NbtLong(999999L)
            this["Variant"] = NbtInt(3)
            this["MarkVariant"] = NbtInt(2)
            this["Saddled"] = NbtByte(1)
            this["Chested"] = NbtByte(1)
        }

        val javaHorse = JavaEntityBuilder.build(bedrockHorse)
        assertNotNull(javaHorse)
        assertEquals("minecraft:horse", javaHorse!!.getNullableString("id"))

        val variant = javaHorse.getNullableInt("Variant")
        assertEquals((2 shl 8) or 3, variant)

        val saddle = javaHorse.getNullableCompound("SaddleItem")
        assertNotNull("Must have SaddleItem", saddle)
        assertEquals("minecraft:saddle", saddle!!.getNullableString("id"))

        val chested = javaHorse.getNullableByte("ChestedHorse")
        assertEquals(1.toByte(), chested)
    }

    @Test
    fun testVillagerV2ToVillager() {
        val bedrockVillager = NbtCompound().apply {
            this["identifier"] = NbtString("minecraft:villager_v2")
            this["UniqueID"] = NbtLong(7777L)
            this["TradeTier"] = NbtInt(2)
            this["TradeExperience"] = NbtInt(250)
            val defs = NbtList<NbtString>().apply {
                add(NbtString("+minecraft:farmer"))
                add(NbtString("+minecraft:desert_villager"))
            }
            this["definitions"] = defs
        }

        val javaVillager = JavaEntityBuilder.build(bedrockVillager)
        assertNotNull(javaVillager)
        assertEquals("minecraft:villager", javaVillager!!.getNullableString("id"))

        val vData = javaVillager.getNullableCompound("VillagerData")
        assertNotNull(vData)
        assertEquals("minecraft:farmer", vData!!.getNullableString("profession"))
        assertEquals("minecraft:desert", vData.getNullableString("type"))
        assertEquals(3, vData.getNullableInt("level"))

        assertEquals(250, javaVillager.getNullableInt("Xp"))
    }

    @Test
    fun testFlagsBitmaskUnpacking() {
        val bedrockActor = NbtCompound().apply {
            this["identifier"] = NbtString("minecraft:wolf")
            this["UniqueID"] = NbtLong(8888L)

            val flags = (1L shl 0) or (1L shl 16) or (1L shl 28)
            this["Flags"] = NbtLong(flags)
            this["CollarColor"] = NbtByte(11)
            val defs = NbtList<NbtString>().apply {
                add(NbtString("+minecraft:wolf_ashen"))
            }
            this["definitions"] = defs
        }

        val javaWolf = JavaEntityBuilder.build(bedrockActor)
        assertNotNull(javaWolf)
        assertEquals("minecraft:wolf", javaWolf!!.getNullableString("id"))
        assertEquals(1.toByte(), javaWolf.getNullableByte("Sitting"))
        assertEquals(1.toByte(), javaWolf.getNullableByte("Tamed"))
        assertEquals(11.toByte(), javaWolf.getNullableByte("CollarColor"))
        assertTrue((javaWolf.getNullableShort("Fire") ?: 0) > 0)
        assertEquals("minecraft:ashen", javaWolf.getNullableString("variant"))
    }

    @Test
    fun testBoatWoodVariant() {
        val bedrockBoat = NbtCompound().apply {
            this["identifier"] = NbtString("minecraft:boat")
            this["UniqueID"] = NbtLong(5555L)
            this["Variant"] = NbtInt(2)
        }

        val javaBoat = JavaEntityBuilder.build(bedrockBoat)
        assertNotNull(javaBoat)
        assertEquals("minecraft:birch_boat", javaBoat!!.getNullableString("id"))
    }

    @Test
    fun testModdedAndUnknownEntityFiltered() {
        val unknown = NbtCompound().apply {
            this["identifier"] = NbtString("custom_mod:magic_golem")
            this["UniqueID"] = NbtLong(111L)
        }
        val result = JavaEntityBuilder.build(unknown)
        assertNull("Unknown entity must return null", result)
    }
}
