package com.noches.chunkoidng.core.conversion

import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GameRuleSyncTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun writeJe26World(dir: File, gameRulesData: NbtCompound, dataVersion: Int = 5023) {
        dir.mkdirs()
        val levelRoot = NbtCompound().apply {
            this["Data"] = NbtCompound().apply {
                this["DataVersion"] = dataVersion
                this["LevelName"] = "JE26"
            }
        }
        NbtIO.writeNbtFile(
            File(dir, "level.dat"),
            NbtFile("", levelRoot),
            compressed = true,
            littleEndian = false,
            writeHeaders = false
        )
        val grFile = File(dir, "data/minecraft/game_rules.dat")
        grFile.parentFile?.mkdirs()
        val grRoot = NbtCompound().apply {
            this["DataVersion"] = dataVersion
            this["data"] = gameRulesData
        }
        NbtIO.writeNbtFile(
            grFile,
            NbtFile("", grRoot),
            compressed = true,
            littleEndian = false,
            writeHeaders = false
        )
    }

    private fun writeJe12111World(dir: File, gameRulesData: NbtCompound, dataVersion: Int = 4671) {
        dir.mkdirs()
        val levelRoot = NbtCompound().apply {
            this["Data"] = NbtCompound().apply {
                this["DataVersion"] = dataVersion
                this["LevelName"] = "JE1_21_11"
                this["game_rules"] = gameRulesData
            }
        }
        NbtIO.writeNbtFile(
            File(dir, "level.dat"),
            NbtFile("", levelRoot),
            compressed = true,
            littleEndian = false,
            writeHeaders = false
        )
    }

    private fun writeJeLegacyWorld(dir: File, gameRulesStrings: NbtCompound, dataVersion: Int = 4189) {
        dir.mkdirs()
        val levelRoot = NbtCompound().apply {
            this["Data"] = NbtCompound().apply {
                this["DataVersion"] = dataVersion
                this["LevelName"] = "JELegacy"
                this["GameRules"] = gameRulesStrings
            }
        }
        NbtIO.writeNbtFile(
            File(dir, "level.dat"),
            NbtFile("", levelRoot),
            compressed = true,
            littleEndian = false,
            writeHeaders = false
        )
    }

    private fun writeBeWorld(dir: File, root: NbtCompound) {
        dir.mkdirs()
        File(dir, "db").mkdirs()
        val nbtFile = NbtFile("", root).apply {
            version = 10
        }
        NbtIO.writeNbtFile(
            File(dir, "level.dat"),
            nbtFile,
            compressed = false,
            littleEndian = true,
            writeHeaders = true
        )
    }

    private fun readJeData(dir: File): NbtCompound {
        val file = NbtIO.readNbtFile(
            File(dir, "level.dat"),
            compressed = true,
            littleEndian = false,
            readHeaders = false
        )
        return file.compound.getCompound("Data")
    }

    private fun readJe26GameRules(dir: File): NbtCompound {
        val file = NbtIO.readNbtFile(
            File(dir, "data/minecraft/game_rules.dat"),
            compressed = true,
            littleEndian = false,
            readHeaders = false
        )
        return file.compound.getNullableCompound("data") ?: file.compound
    }

    private fun readBeRoot(dir: File): NbtCompound {
        val file = NbtIO.readNbtFile(
            File(dir, "level.dat"),
            compressed = false,
            littleEndian = true,
            readHeaders = true
        )
        return file.compound
    }

    @Test
    fun testJe26ToBedrockSyncsMobGriefingAndRules() {
        val inDir = tempFolder.newFolder("je26_in")
        val outDir = tempFolder.newFolder("be_out")

        writeJe26World(
            inDir,
            NbtCompound().apply {
                this["minecraft:mob_griefing"] = false
                this["minecraft:spawn_mobs"] = false
                this["minecraft:spawn_phantoms"] = false
                this["minecraft:fire_spread_radius_around_player"] = 0
                this["minecraft:pvp"] = false
                this["minecraft:tnt_explodes"] = false
                this["minecraft:random_tick_speed"] = 7
                this["minecraft:max_entity_cramming"] = 24
            }
        )
        writeBeWorld(
            outDir,
            NbtCompound().apply {
                this["LevelName"] = "BEOut"
                this["mobgriefing"] = true
                this["domobspawning"] = true
                this["dofiretick"] = true
                this["pvp"] = true
                this["tntexplodes"] = true
            }
        )

        val targetFormat = ChunkerFormat.findById("BEDROCK_1_21_130")!!
        val synced = GameRuleSync.sync(inDir, outDir, targetFormat)
        assertTrue("Expected synced rules > 0", synced > 0)

        val beRoot = readBeRoot(outDir)
        assertEquals(0.toByte(), beRoot.getNullableByte("mobgriefing"))
        assertEquals(0.toByte(), beRoot.getNullableByte("domobspawning"))
        assertEquals(0.toByte(), beRoot.getNullableByte("spawnMobs"))
        assertEquals(0.toByte(), beRoot.getNullableByte("doinsomnia"))
        assertEquals(0.toByte(), beRoot.getNullableByte("dofiretick"))
        assertEquals(0.toByte(), beRoot.getNullableByte("pvp"))
        assertEquals(0.toByte(), beRoot.getNullableByte("tntexplodes"))
        assertEquals(7, beRoot.getNullableInt("randomtickspeed"))

        val outDirFire = tempFolder.newFolder("be_out_fire")
        val inDirFire = tempFolder.newFolder("je26_in_fire")
        writeJe26World(
            inDirFire,
            NbtCompound().apply {
                this["minecraft:fire_spread_radius_around_player"] = 128
            }
        )
        writeBeWorld(
            outDirFire,
            NbtCompound().apply {
                this["LevelName"] = "BEFire"
                this["dofiretick"] = false
            }
        )
        GameRuleSync.sync(inDirFire, outDirFire, targetFormat)
        assertEquals(1.toByte(), readBeRoot(outDirFire).getNullableByte("dofiretick"))
    }

    @Test
    fun testBedrockToJe26SyncsGameRulesDat() {
        val inDir = tempFolder.newFolder("be_in")
        val outDir = tempFolder.newFolder("je26_out")

        writeBeWorld(
            inDir,
            NbtCompound().apply {
                this["mobgriefing"] = false
                this["domobspawning"] = false
                this["spawnMobs"] = false
                this["doinsomnia"] = false
                this["dofiretick"] = false
                this["pvp"] = false
                this["tntexplodes"] = false
                this["randomtickspeed"] = 5
            }
        )
        writeJe26World(
            outDir,
            NbtCompound().apply {
                this["minecraft:mob_griefing"] = true
                this["minecraft:spawn_mobs"] = true
                this["minecraft:spawn_phantoms"] = true
                this["minecraft:fire_spread_radius_around_player"] = 128
                this["minecraft:pvp"] = true
                this["minecraft:tnt_explodes"] = true
                this["minecraft:random_tick_speed"] = 3
            }
        )

        val targetFormat = ChunkerFormat.findById("JAVA_26_3")!!
        val synced = GameRuleSync.sync(inDir, outDir, targetFormat)
        assertTrue(synced > 0)

        val data = readJe26GameRules(outDir)
        assertEquals(0.toByte(), data.getNullableByte("minecraft:mob_griefing"))
        assertEquals(0.toByte(), data.getNullableByte("minecraft:spawn_mobs"))
        assertEquals(0.toByte(), data.getNullableByte("minecraft:spawn_phantoms"))
        assertEquals(0, data.getNullableInt("minecraft:fire_spread_radius_around_player"))
        assertEquals(0.toByte(), data.getNullableByte("minecraft:pvp"))
        assertEquals(0.toByte(), data.getNullableByte("minecraft:tnt_explodes"))
        assertEquals(5, data.getNullableInt("minecraft:random_tick_speed"))
    }

    @Test
    fun testBedrockAndJe12111RoundTrip() {
        val inBe = tempFolder.newFolder("be_in_12111")
        val outJe = tempFolder.newFolder("je12111_out")
        writeBeWorld(
            inBe,
            NbtCompound().apply {
                this["mobgriefing"] = false
                this["domobspawning"] = false
                this["doinsomnia"] = false
                this["dofiretick"] = false
                this["pvp"] = false
                this["tntexplodes"] = false
                this["randomtickspeed"] = 4
            }
        )
        writeJe12111World(
            outJe,
            NbtCompound().apply {
                this["minecraft:mob_griefing"] = true
                this["minecraft:spawn_mobs"] = true
                this["minecraft:pvp"] = true
            }
        )

        val je12111 = ChunkerFormat.findById("JAVA_1_21_11")!!
        assertTrue(GameRuleSync.sync(inBe, outJe, je12111) > 0)

        val jeRules = readJeData(outJe).getNullableCompound("game_rules")
        assertNotNull(jeRules)
        assertEquals(0.toByte(), jeRules!!.getNullableByte("minecraft:mob_griefing"))
        assertEquals(0.toByte(), jeRules.getNullableByte("minecraft:spawn_mobs"))
        assertEquals(0.toByte(), jeRules.getNullableByte("minecraft:spawn_phantoms"))
        assertEquals(0.toByte(), jeRules.getNullableByte("minecraft:pvp"))
        assertEquals(4, jeRules.getNullableInt("minecraft:random_tick_speed"))

        val outBe = tempFolder.newFolder("be_from_12111")
        writeBeWorld(outBe, NbtCompound().apply { this["mobgriefing"] = true })
        GameRuleSync.sync(outJe, outBe, ChunkerFormat.findById("BEDROCK_1_21_130")!!)
        val beRoot = readBeRoot(outBe)
        assertEquals(0.toByte(), beRoot.getNullableByte("mobgriefing"))
        assertEquals(0.toByte(), beRoot.getNullableByte("domobspawning"))
        assertEquals(0.toByte(), beRoot.getNullableByte("doinsomnia"))
    }

    @Test
    fun testJeLegacyToJe26AndReverseInvertedBooleans() {
        val jeLegacyIn = tempFolder.newFolder("je_legacy_in")
        val je26Out = tempFolder.newFolder("je26_from_legacy")
        val beOut = tempFolder.newFolder("be_from_legacy")

        writeJeLegacyWorld(
            jeLegacyIn,
            NbtCompound().apply {
                this["mobGriefing"] = "false"
                this["doMobSpawning"] = "false"
                this["doInsomnia"] = "false"
                this["doFireTick"] = "false"
                this["disableRaids"] = "true"
                this["disableElytraMovementCheck"] = "true"
                this["disablePlayerMovementCheck"] = "true"
                this["randomTickSpeed"] = "9"
            }
        )
        writeJe26World(je26Out, NbtCompound())
        writeBeWorld(beOut, NbtCompound())

        GameRuleSync.sync(jeLegacyIn, je26Out, ChunkerFormat.findById("JAVA_26_3")!!)
        val je26Rules = readJe26GameRules(je26Out)
        assertEquals(0.toByte(), je26Rules.getNullableByte("minecraft:mob_griefing"))
        assertEquals(0.toByte(), je26Rules.getNullableByte("minecraft:spawn_mobs"))
        assertEquals(0.toByte(), je26Rules.getNullableByte("minecraft:spawn_phantoms"))
        assertEquals(0, je26Rules.getNullableInt("minecraft:fire_spread_radius_around_player"))
        assertEquals(0.toByte(), je26Rules.getNullableByte("minecraft:raids"))
        assertEquals(0.toByte(), je26Rules.getNullableByte("minecraft:elytra_movement_check"))
        assertEquals(0.toByte(), je26Rules.getNullableByte("minecraft:player_movement_check"))
        assertEquals(9, je26Rules.getNullableInt("minecraft:random_tick_speed"))

        GameRuleSync.sync(jeLegacyIn, beOut, ChunkerFormat.findById("BEDROCK_1_21_130")!!)
        val beRoot = readBeRoot(beOut)
        assertEquals(0.toByte(), beRoot.getNullableByte("mobgriefing"))
        assertEquals(0.toByte(), beRoot.getNullableByte("domobspawning"))
        assertEquals(0.toByte(), beRoot.getNullableByte("spawnMobs"))
        assertEquals(0.toByte(), beRoot.getNullableByte("doinsomnia"))
        assertEquals(0.toByte(), beRoot.getNullableByte("dofiretick"))
        assertEquals(9, beRoot.getNullableInt("randomtickspeed"))

        val jeLegacyOut = tempFolder.newFolder("je_legacy_out")
        writeJeLegacyWorld(jeLegacyOut, NbtCompound())
        GameRuleSync.sync(je26Out, jeLegacyOut, ChunkerFormat.findById("JAVA_1_21_4")!!)
        val legacyRules = readJeData(jeLegacyOut).getNullableCompound("GameRules")!!
        assertEquals("false", legacyRules.getNullableString("mobGriefing"))
        assertEquals("false", legacyRules.getNullableString("doFireTick"))
        assertEquals("true", legacyRules.getNullableString("disableRaids"))
        assertEquals("true", legacyRules.getNullableString("disableElytraMovementCheck"))
        assertEquals("true", legacyRules.getNullableString("disablePlayerMovementCheck"))
        assertEquals("9", legacyRules.getNullableString("randomTickSpeed"))
    }

    @Test
    fun testBedrockToBedrockAndFormatInputOnJava() {
        val beIn = tempFolder.newFolder("be_be_in")
        val beOut = tempFolder.newFolder("be_be_out")
        writeBeWorld(
            beIn,
            NbtCompound().apply {
                this["mobgriefing"] = false
                this["spawnMobs"] = false
                this["domobspawning"] = false
                this["showcoordinates"] = true
                this["randomtickspeed"] = 10
            }
        )
        writeBeWorld(
            beOut,
            NbtCompound().apply {
                this["mobgriefing"] = true
                this["showcoordinates"] = false
            }
        )
        GameRuleSync.sync(beIn, beOut, ChunkerFormat.findById("BEDROCK_1_21_130")!!)
        val beResult = readBeRoot(beOut)
        assertEquals(0.toByte(), beResult.getNullableByte("mobgriefing"))
        assertEquals(0.toByte(), beResult.getNullableByte("spawnMobs"))
        assertEquals(1.toByte(), beResult.getNullableByte("showcoordinates"))
        assertEquals(10, beResult.getNullableInt("randomtickspeed"))

        val jeIn = tempFolder.newFolder("je_input_src")
        val jeOut = tempFolder.newFolder("je_input_dst")
        writeJe26World(
            jeIn,
            NbtCompound().apply {
                this["minecraft:mob_griefing"] = false
                this["minecraft:raids"] = false
            }
        )
        writeJe26World(
            jeOut,
            NbtCompound().apply {
                this["minecraft:mob_griefing"] = true
                this["minecraft:raids"] = true
            }
        )
        val synced = GameRuleSync.sync(jeIn, jeOut, ChunkerFormat.FORMAT_INPUT)
        assertTrue(synced > 0)
        val rules = readJe26GameRules(jeOut)
        assertEquals(0.toByte(), rules.getNullableByte("minecraft:mob_griefing"))
        assertEquals(0.toByte(), rules.getNullableByte("minecraft:raids"))
    }
}
