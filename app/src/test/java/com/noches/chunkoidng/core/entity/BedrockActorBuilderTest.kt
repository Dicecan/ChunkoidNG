package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.NbtByte
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtDouble
import br.com.gamemods.nbtmanipulator.NbtFloat
import br.com.gamemods.nbtmanipulator.NbtInt
import br.com.gamemods.nbtmanipulator.NbtList
import br.com.gamemods.nbtmanipulator.NbtShort
import br.com.gamemods.nbtmanipulator.NbtString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BedrockActorBuilderTest {

    @Test
    fun testSheepConversion() {
        val javaSheep = NbtCompound()
        javaSheep["id"] = NbtString("minecraft:sheep")
        javaSheep["Color"] = NbtByte(5)
        javaSheep["Sheared"] = NbtByte(1)
        javaSheep["Health"] = NbtFloat(8f)

        val posList = NbtList<NbtDouble>()
        posList.add(NbtDouble(12.5))
        posList.add(NbtDouble(64.0))
        posList.add(NbtDouble(-88.25))
        javaSheep["Pos"] = posList

        val actor = BedrockActorBuilder.build(javaSheep, uniqueId = 1001L)
        assertNotNull(actor)
        assertEquals("minecraft:sheep", actor!!.getNullableString("identifier"))
        assertEquals(1001L, actor.getNullableLong("UniqueID"))
        assertEquals(5.toByte(), actor.getNullableByte("Color"))
        assertEquals(1.toByte(), actor.getNullableByte("Sheared"))

        val definitions = actor.getNullableStringList("definitions")
        assertNotNull(definitions)
        assertTrue(definitions!!.any { it.value == "+minecraft:sheep" })
        assertTrue(definitions.any { it.value == "+minecraft:sheep_lime" })
        assertTrue(definitions.any { it.value == "+minecraft:sheep_sheared" })

        val bedrockPos = actor.getNullableFloatList("Pos")
        assertNotNull(bedrockPos)
        assertEquals(12.5f, bedrockPos!![0].value, 0.001f)
        assertEquals(64.0f, bedrockPos[1].value, 0.001f)
        assertEquals(-88.25f, bedrockPos[2].value, 0.001f)
    }

    @Test
    fun testHorseWithSaddleAndMarkings() {
        val javaHorse = NbtCompound()
        javaHorse["id"] = NbtString("minecraft:horse")

        javaHorse["Variant"] = NbtInt(770)

        val equipment = NbtCompound()
        val saddleItem = NbtCompound()
        saddleItem["id"] = NbtString("minecraft:saddle")
        equipment["body"] = saddleItem
        javaHorse["equipment"] = equipment

        val actor = BedrockActorBuilder.build(javaHorse, uniqueId = 2002L)
        assertNotNull(actor)
        assertEquals("minecraft:horse", actor!!.getNullableString("identifier"))
        assertEquals(2, actor.getNullableInt("Variant"))
        assertEquals(3, actor.getNullableInt("MarkVariant"))
        assertEquals(1.toByte(), actor.getNullableByte("Saddled"))

        val defs = actor.getNullableStringList("definitions")
        assertNotNull(defs)
        assertTrue(defs!!.any { it.value == "+minecraft:horse_saddled" })
        assertTrue(defs.any { it.value == "+minecraft:horse_chestnut" })
        assertTrue(defs.any { it.value == "+minecraft:horse_white_dots" })
    }

    @Test
    fun testVillagerProfessionAndLevel() {
        val javaVillager = NbtCompound()
        javaVillager["id"] = NbtString("minecraft:villager")

        val villagerData = NbtCompound()
        villagerData["profession"] = NbtString("minecraft:farmer")
        villagerData["type"] = NbtString("minecraft:desert")
        villagerData["level"] = NbtInt(3)
        javaVillager["VillagerData"] = villagerData
        javaVillager["Xp"] = NbtInt(120)

        val actor = BedrockActorBuilder.build(javaVillager, uniqueId = 3003L)
        assertNotNull(actor)
        assertEquals("minecraft:villager_v2", actor!!.getNullableString("identifier"))
        assertEquals(2, actor.getNullableInt("TradeTier"))
        assertEquals(120, actor.getNullableInt("TradeExperience"))

        val defs = actor.getNullableStringList("definitions")
        assertNotNull(defs)
        assertTrue(defs!!.any { it.value == "+minecraft:farmer" })
        assertTrue(defs.any { it.value == "+minecraft:desert_villager" })
    }

    @Test
    fun testDroppedItemConversion() {
        val javaItemEntity = NbtCompound()
        javaItemEntity["id"] = NbtString("minecraft:item")
        val itemTag = NbtCompound()
        itemTag["id"] = NbtString("minecraft:diamond_sword")
        itemTag["Count"] = NbtByte(1)
        itemTag["Damage"] = NbtShort(15)
        javaItemEntity["Item"] = itemTag
        javaItemEntity["Age"] = NbtShort(120)

        val actor = BedrockActorBuilder.build(javaItemEntity, uniqueId = 4004L)
        assertNotNull(actor)
        assertEquals("minecraft:item", actor!!.getNullableString("identifier"))
        assertEquals(5.toShort(), actor.getNullableShort("Health"))
        assertEquals(120.toShort(), actor.getNullableShort("Age"))

        val itemPayload = actor.getNullableCompound("Item")
        assertNotNull(itemPayload)
        assertEquals("minecraft:diamond_sword", itemPayload!!.getNullableString("Name"))
        assertEquals(1.toByte(), itemPayload.getNullableByte("Count"))
        assertEquals(15.toShort(), itemPayload.getNullableShort("Damage"))
    }

    @Test
    fun testModdedEntityFiltering() {
        val modEntity = NbtCompound()
        modEntity["id"] = NbtString("twilightforest:naga")
        modEntity["Health"] = NbtFloat(120f)

        val actor = BedrockActorBuilder.build(modEntity, uniqueId = 5005L)
        assertNull(actor)
    }
}
