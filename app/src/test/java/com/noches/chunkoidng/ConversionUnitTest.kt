package com.noches.chunkoidng

import com.noches.chunkoidng.core.conversion.ChunkerFormat
import com.noches.chunkoidng.core.conversion.ConversionConfig
import com.noches.chunkoidng.core.world.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ConversionUnitTest {

    @Test
    fun testChunkerFormats() {
        val bedrockDefault = ChunkerFormat.getDefaultFormatForOpposite(Platform.JAVA)
        assertEquals(Platform.BEDROCK, bedrockDefault.platform)
        assertEquals("BEDROCK_1_21_50", bedrockDefault.id)

        val javaDefault = ChunkerFormat.getDefaultFormatForOpposite(Platform.BEDROCK)
        assertEquals(Platform.JAVA, javaDefault.platform)
        assertEquals("JAVA_1_21_4", javaDefault.id)

        val inputFormat = ChunkerFormat.findById("INPUT")
        assertNotNull(inputFormat)
        assertEquals("INPUT", inputFormat?.id)

        val specificFormat = ChunkerFormat.findById("BEDROCK_1_21_130")
        assertNotNull(specificFormat)
        assertEquals("1.21.x", specificFormat?.group)
    }

    @Test
    fun testConversionConfigPruningJson() {
        val dummyIn = File("in")
        val dummyOut = File("out")
        val targetFormat = ChunkerFormat.BEDROCK_FORMATS.first()

        // When all dimensions are included, pruning JSON should be null
        val configAll = ConversionConfig(
            inputDir = dummyIn,
            outputDir = dummyOut,
            targetFormat = targetFormat,
            includeOverworld = true,
            includeNether = true,
            includeTheEnd = true
        )
        assertNull(configAll.buildPruningJson())

        // When Nether and End are excluded
        val configExcludes = ConversionConfig(
            inputDir = dummyIn,
            outputDir = dummyOut,
            targetFormat = targetFormat,
            includeOverworld = true,
            includeNether = false,
            includeTheEnd = false
        )
        val pruningJson = configExcludes.buildPruningJson()
        assertNotNull(pruningJson)
        assertTrue(pruningJson!!.contains("\"NETHER\": {\"include\": false}"))
        assertTrue(pruningJson.contains("\"THE_END\": {\"include\": false}"))
    }

    @Test
    fun testConversionConfigWorldSettingsJson() {
        val dummyIn = File("in")
        val dummyOut = File("out")
        val targetFormat = ChunkerFormat.JAVA_FORMATS.first()

        val config = ConversionConfig(
            inputDir = dummyIn,
            outputDir = dummyOut,
            targetFormat = targetFormat,
            overrideWorldName = "My Epic World",
            overrideGameMode = "SURVIVAL",
            overrideDifficulty = "HARD"
        )

        val json = config.buildWorldSettingsJson()
        assertNotNull(json)
        assertTrue(json!!.contains("\"world_name\": \"My Epic World\""))
        assertTrue(json.contains("\"game_mode\": \"SURVIVAL\""))
        assertTrue(json.contains("\"difficulty\": \"HARD\""))
    }

    @Test
    fun testPlatformEnum() {
        assertEquals(Platform.JAVA, Platform.fromString("java"))
        assertEquals(Platform.JAVA, Platform.fromString("JE"))
        assertEquals(Platform.BEDROCK, Platform.fromString("bedrock"))
        assertEquals(Platform.BEDROCK, Platform.fromString("be"))
    }
}
