package com.noches.chunkoidng.core.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityIdMapperTest {

    @Test
    fun testVanillaWhitelistValidation() {

        assertTrue(EntityIdMapper.isVanilla("minecraft:sheep"))
        assertTrue(EntityIdMapper.isVanilla("minecraft:zombie"))
        assertTrue(EntityIdMapper.isVanilla("minecraft:creeper"))
        assertTrue(EntityIdMapper.isVanilla("minecraft:pig"))
        assertTrue(EntityIdMapper.isVanilla("minecraft:horse"))
        assertTrue(EntityIdMapper.isVanilla("minecraft:villager"))
        assertTrue(EntityIdMapper.isVanilla("minecraft:item"))
        assertTrue(EntityIdMapper.isVanilla("minecraft:experience_orb"))

        assertTrue(EntityIdMapper.isVanilla("minecraft:oak_boat"))
        assertTrue(EntityIdMapper.isVanilla("minecraft:mangrove_chest_boat"))
        assertTrue(EntityIdMapper.isVanilla("minecraft:cherry_boat"))

        assertFalse(EntityIdMapper.isVanilla("twilightforest:naga"))
        assertFalse(EntityIdMapper.isVanilla("alexsmobs:capuchin_monkey"))
        assertFalse(EntityIdMapper.isVanilla("custom_mod:unknown_boss"))
        assertFalse(EntityIdMapper.isVanilla(null))
        assertFalse(EntityIdMapper.isVanilla(""))
    }

    @Test
    fun testBedrockRenames() {
        assertEquals("minecraft:zombie_pigman", EntityIdMapper.toBedrock("minecraft:zombified_piglin"))
        assertEquals("minecraft:evocation_illager", EntityIdMapper.toBedrock("minecraft:evoker"))
        assertEquals("minecraft:xp_orb", EntityIdMapper.toBedrock("minecraft:experience_orb"))
        assertEquals("minecraft:xp_bottle", EntityIdMapper.toBedrock("minecraft:experience_bottle"))
        assertEquals("minecraft:villager_v2", EntityIdMapper.toBedrock("minecraft:villager"))
        assertEquals("minecraft:zombie_villager_v2", EntityIdMapper.toBedrock("minecraft:zombie_villager"))
        assertEquals("minecraft:tropicalfish", EntityIdMapper.toBedrock("minecraft:tropical_fish"))
        assertEquals("minecraft:ender_crystal", EntityIdMapper.toBedrock("minecraft:end_crystal"))

        assertEquals("minecraft:boat", EntityIdMapper.toBedrock("minecraft:spruce_boat"))
        assertEquals("minecraft:chest_boat", EntityIdMapper.toBedrock("minecraft:dark_oak_chest_boat"))

        assertEquals("minecraft:sheep", EntityIdMapper.toBedrock("minecraft:sheep"))
        assertEquals("minecraft:cow", EntityIdMapper.toBedrock("minecraft:cow"))

        assertNull(EntityIdMapper.toBedrock("botania:doppleganger"))
        assertNull(EntityIdMapper.toBedrock("pixelmon:pikachu"))
    }
}
