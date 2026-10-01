package com.noches.chunkoidng

import com.noches.chunkoidng.core.leveldb.BedrockLevelDbHelper
import com.noches.chunkoidng.core.leveldb.LevelDbCategory
import com.noches.chunkoidng.core.leveldb.LevelDbRecord
import com.noches.chunkoidng.core.nbt.MinecraftSemanticDescriptor
import org.junit.Assert.*
import org.junit.Test

class MinecraftSemanticDescriptorTest {

    @Test
    fun testNbtKeyResolution() {
        val gamemodeTag = MinecraftSemanticDescriptor.describeNbt("GameType")
        assertNotNull(gamemodeTag)
        assertEquals(R.string.nbt_tag_gamemode, gamemodeTag!!.titleRes)
        assertEquals(R.string.nbt_guide_gamemode, gamemodeTag.guideRes)

        val healthTag = MinecraftSemanticDescriptor.describeNbt("Health")
        assertNotNull(healthTag)
        assertEquals(R.string.nbt_tag_health, healthTag!!.titleRes)
        assertEquals(R.string.nbt_guide_health, healthTag.guideRes)

        val creativeTag = MinecraftSemanticDescriptor.describeNbt("hasBeenLoadedInCreative")
        assertNotNull(creativeTag)
        assertEquals(R.string.nbt_tag_creative_loaded, creativeTag!!.titleRes)
        assertEquals(R.string.nbt_guide_creative_loaded, creativeTag.guideRes)

        val cheatsTag = MinecraftSemanticDescriptor.describeNbt("allowCommands")
        assertNotNull(cheatsTag)
        assertEquals(R.string.nbt_tag_cheats, cheatsTag!!.titleRes)
        assertEquals(R.string.nbt_guide_cheats, cheatsTag.guideRes)

        val posTag = MinecraftSemanticDescriptor.describeNbt("Pos")
        assertNotNull(posTag)
        assertEquals(R.string.nbt_tag_position, posTag!!.titleRes)

        val inhabitedTag = MinecraftSemanticDescriptor.describeNbt("InhabitedTime")
        assertNotNull(inhabitedTag)
        assertEquals(R.string.nbt_tag_inhabited_time, inhabitedTag!!.titleRes)
        assertEquals(R.string.nbt_guide_inhabited_time, inhabitedTag.guideRes)

        val unbreakableTag = MinecraftSemanticDescriptor.describeNbt("Unbreakable")
        assertNotNull(unbreakableTag)
        assertEquals(R.string.nbt_tag_unbreakable, unbreakableTag!!.titleRes)
        assertEquals(R.string.nbt_guide_unbreakable, unbreakableTag.guideRes)
    }

    @Test
    fun testLevelDbRecordResolution() {
        val localPlayerRecord = LevelDbRecord(
            key = "~local_player".toByteArray(Charsets.UTF_8),
            keyString = "~local_player",
            displayName = "Local Player",
            valueSize = 100,
            isNbt = true,
            category = LevelDbCategory.PLAYER
        )
        val localPlayerTag = MinecraftSemanticDescriptor.describeLevelDbRecord(localPlayerRecord)
        assertNotNull(localPlayerTag)
        assertEquals(R.string.leveldb_record_local_player, localPlayerTag!!.titleRes)

        val portalRecord = LevelDbRecord(
            key = "portals".toByteArray(Charsets.UTF_8),
            keyString = "portals",
            displayName = "portals",
            valueSize = 50,
            isNbt = true,
            category = LevelDbCategory.WORLD
        )
        val portalTag = MinecraftSemanticDescriptor.describeLevelDbRecord(portalRecord)
        assertNotNull(portalTag)
        assertEquals(R.string.leveldb_record_portals, portalTag!!.titleRes)

        val scoreboardRecord = LevelDbRecord(
            key = "scoreboard".toByteArray(Charsets.UTF_8),
            keyString = "scoreboard",
            displayName = "scoreboard",
            valueSize = 200,
            isNbt = true,
            category = LevelDbCategory.WORLD
        )
        val scoreboardTag = MinecraftSemanticDescriptor.describeLevelDbRecord(scoreboardRecord)
        assertNotNull(scoreboardTag)
        assertEquals(R.string.leveldb_record_scoreboard, scoreboardTag!!.titleRes)
    }

    @Test
    fun testPathFallbackResolution() {
        val pathTag = MinecraftSemanticDescriptor.describeNbt("foo", "Data/Player/Pos")
        assertNotNull(pathTag)
        assertEquals(R.string.nbt_tag_position, pathTag!!.titleRes)
    }
}
