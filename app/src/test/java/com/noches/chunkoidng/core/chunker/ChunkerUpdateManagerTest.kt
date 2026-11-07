package com.noches.chunkoidng.core.chunker

import com.noches.chunkoidng.core.conversion.ChunkerFormat
import com.noches.chunkoidng.core.world.Platform
import org.junit.Assert.*
import org.junit.Test

class ChunkerUpdateManagerTest {

    @Test
    fun testVersionComparison() {
        assertTrue(ChunkerUpdateManager.isNewerVersion("1.21.0", "1.19.1"))
        assertTrue(ChunkerUpdateManager.isNewerVersion("1.20.0", "1.19.1"))
        assertTrue(ChunkerUpdateManager.isNewerVersion("1.19.2", "1.19.1"))
        assertTrue(ChunkerUpdateManager.isNewerVersion("2.0.0", "1.21.0"))

        assertFalse(ChunkerUpdateManager.isNewerVersion("1.19.1", "1.19.1"))
        assertFalse(ChunkerUpdateManager.isNewerVersion("1.19.0", "1.19.1"))
        assertFalse(ChunkerUpdateManager.isNewerVersion("1.18.2", "1.19.1"))
        assertFalse(ChunkerUpdateManager.isNewerVersion("1.19.1", "1.21.0"))
    }

    @Test
    fun testParseSupportedFormatsFromBody() {
        val sampleBody = """
            ## Chunker 1.22.0
            Added support for Bedrock 1.22.0 and Java 1.22.0.
            Also updated Bedrock 1.22.10.
        """.trimIndent()

        val parsed = ChunkerUpdateManager.parseSupportedFormatsFromBody(sampleBody, "1.22.0")
        assertTrue(parsed.any { it.id == "BEDROCK_1_22_0" && it.platform == Platform.BEDROCK })
        assertTrue(parsed.any { it.id == "BEDROCK_1_22_10" && it.platform == Platform.BEDROCK })
        assertTrue(parsed.any { it.id == "JAVA_1_22_0" && it.platform == Platform.JAVA })
    }

    @Test
    fun testDynamicFormatRegistrationInChunkerFormat() {
        val customFormat = ChunkerFormat(
            id = "BEDROCK_1_99_0",
            platform = Platform.BEDROCK,
            group = "1.99.x",
            displayName = "Bedrock 1.99.0",
            isPopular = true
        )

        ChunkerFormat.registerDynamicFormats(listOf(customFormat))

        val found = ChunkerFormat.findById("BEDROCK_1_99_0")
        assertNotNull(found)
        assertEquals("Bedrock 1.99.0", found?.displayName)
        assertTrue(ChunkerFormat.BEDROCK_FORMATS.any { it.id == "BEDROCK_1_99_0" })
    }
}
