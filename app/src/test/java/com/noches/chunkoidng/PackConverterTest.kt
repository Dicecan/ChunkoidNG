package com.noches.chunkoidng

import com.noches.chunkoidng.core.pack.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PackConverterTest {

    @Test
    fun testPackAssetMapperMappings() {
        val bedrockBlock = PackAssetMapper.mapJavaToBedrock("assets/minecraft/textures/block/stone_bricks.png")
        assertEquals("textures/blocks/stonebrick.png", bedrockBlock)

        val javaBlock = PackAssetMapper.mapBedrockToJava("textures/blocks/stonebrick.png")
        assertEquals("assets/minecraft/textures/block/stone_bricks.png", javaBlock)

        val bedrockItem = PackAssetMapper.mapJavaToBedrock("assets/minecraft/textures/item/wooden_sword.png")
        assertEquals("textures/items/wood_sword.png", bedrockItem)

        val javaItem = PackAssetMapper.mapBedrockToJava("textures/items/wood_sword.png")
        assertEquals("assets/minecraft/textures/item/wooden_sword.png", javaItem)

        val bedrockTexture = PackAssetMapper.mapJavaToBedrock("assets/minecraft/textures/block/grass_block_top.png")
        assertEquals("textures/blocks/grass_top.png", bedrockTexture)

        val javaTexture = PackAssetMapper.mapBedrockToJava("textures/blocks/grass_top.png")
        assertEquals("assets/minecraft/textures/block/grass_block_top.png", javaTexture)
    }

    @Test
    fun testPackManifestDetectionAndConversion() {
        val detectedJava = PackManifestHandler.detectPlatform(listOf("pack.mcmeta", "assets/minecraft/textures/block/stone.png"))
        assertEquals(PackPlatform.JAVA, detectedJava)

        val detectedBedrock = PackManifestHandler.detectPlatform(listOf("manifest.json", "textures/blocks/stone.png"))
        assertEquals(PackPlatform.BEDROCK, detectedBedrock)

        val generatedManifest = PackManifestHandler.generateBedrockManifest(
            name = "Test Pack",
            description = "Test Description"
        )
        val json = JSONObject(generatedManifest)
        assertEquals(2, json.getInt("format_version"))
        val header = json.getJSONObject("header")
        assertEquals("Test Description", header.getString("description"))

        assertEquals(
            PackPlatform.JAVA,
            PackManifestHandler.detectPlatform(listOf("MyPack/pack.mcmeta", "MyPack/assets/minecraft/textures/block/stone.png"))
        )
    }

    @Test
    fun testTextureAtlasGenerator() {
        val testDir = File.createTempFile("atlas_test", "").apply {
            delete()
            mkdirs()
        }
        try {
            val terrainTexture = File(testDir, "textures/terrain_texture.json")
            val itemTexture = File(testDir, "textures/item_texture.json")
            val texturesList = File(testDir, "textures/textures_list.json")

            TextureAtlasGenerator.generateTerrainTextureJson(
                setOf("textures/blocks/stone.png", "textures/blocks/dirt.png"),
                terrainTexture
            )
            TextureAtlasGenerator.generateItemTextureJson(
                setOf("textures/items/iron_sword.png"),
                itemTexture
            )
            TextureAtlasGenerator.generateTexturesListJson(
                setOf("textures/blocks/stone.png", "textures/items/iron_sword.png"),
                texturesList
            )

            assertTrue(terrainTexture.exists())
            assertTrue(itemTexture.exists())
            assertTrue(texturesList.exists())

            val terrainJson = JSONObject(terrainTexture.readText())
            val textureData = terrainJson.getJSONObject("texture_data")
            assertTrue(textureData.has("stone"))
            assertTrue(textureData.has("dirt"))

            val itemJson = JSONObject(itemTexture.readText())
            val itemData = itemJson.getJSONObject("texture_data")
            assertTrue(itemData.has("iron_sword"))

            val collisionFile = File(testDir, "textures/collision.json")
            TextureAtlasGenerator.generateTerrainTextureJson(
                setOf("textures/blocks/foo.png", "textures/blocks/custom/foo.png"),
                collisionFile
            )
            val collisionData = JSONObject(collisionFile.readText()).getJSONObject("texture_data")
            assertTrue(collisionData.has("foo"))
            assertTrue(collisionData.has("custom_foo"))
        } finally {
            testDir.deleteRecursively()
        }
    }

    @Test
    fun testFlipbookAnimationConversion() {
        val mcmeta = """
            {
              "animation": {
                "frametime": 2,
                "interpolate": true
              }
            }
        """.trimIndent()

        val entry = FlipbookAnimationHandler.parseJavaAnimationMcmeta(mcmeta, "textures/blocks/water_still.png")
        assertNotNull(entry)
        assertEquals(2, entry?.ticksPerFrame)
        assertEquals(true, entry?.interpolate)
        assertEquals("water_still", entry?.atlasTile)

        val outputFile = File.createTempFile("flipbook", ".json").apply { deleteOnExit() }
        FlipbookAnimationHandler.writeFlipbookTexturesJson(listOf(entry!!), outputFile)
        assertTrue(outputFile.exists())
        assertTrue(outputFile.readText().contains("water_still"))

        val variableTiming = """
            {"animation":{"frametime":2,"frames":[{"index":0,"time":1},{"index":1,"time":3}]}}
        """.trimIndent()
        val variableEntry = FlipbookAnimationHandler.parseJavaAnimationMcmeta(
            variableTiming,
            "textures/blocks/custom/water.png"
        )
        assertNotNull(variableEntry)
        assertEquals(1, variableEntry?.ticksPerFrame)
        assertEquals(listOf(0, 1, 1, 1), variableEntry?.frames)
        assertEquals("custom_water", variableEntry?.atlasTile)
    }

    @Test
    fun testPackLangConversion() {
        val javaLang = """
            {
              "block.minecraft.stone": "Stone",
              "item.minecraft.apple": "Apple"
            }
        """.trimIndent()

        val bedrockLang = PackLangHandler.convertJavaJsonToBedrockLang(javaLang)
        assertTrue(bedrockLang.contains("tile.stone.name=Stone"))
        assertTrue(bedrockLang.contains("item.apple.name=Apple"))

        val roundTripJava = PackLangHandler.convertBedrockLangToJavaJson(bedrockLang)
        val json = JSONObject(roundTripJava)
        assertEquals("Stone", json.getString("block.minecraft.stone"))
        assertEquals("Apple", json.getString("item.minecraft.apple"))
    }

    @Test
    fun testArchiveRejectsPathTraversal() {
        val archive = File.createTempFile("pack_slip", ".zip")
        val destination = File.createTempFile("pack_slip_dest", "").apply {
            delete()
            mkdirs()
        }
        val escapedName = "pack_slip_escaped_${System.nanoTime()}.txt"
        val escapedFile = File(destination.parentFile, escapedName)
        try {
            ZipOutputStream(archive.outputStream()).use { zos ->
                zos.putNextEntry(ZipEntry("../$escapedName"))
                zos.write("blocked".toByteArray())
                zos.closeEntry()
            }

            var failed = false
            try {
                runBlocking { ResourcePackConverterEngine.extractArchive(archive, destination) }
            } catch (_: Exception) {
                failed = true
            }
            assertTrue(failed)
            assertFalse(escapedFile.exists())
        } finally {
            archive.delete()
            destination.deleteRecursively()
            escapedFile.delete()
        }
    }

    @Test
    fun testConversionFlattensPackWrapperDirectory() {
        val source = File.createTempFile("pack_wrapper", "").apply {
            delete()
            mkdirs()
        }
        val wrapper = File(source, "MyPack")
        val texture = File(wrapper, "assets/minecraft/textures/block/stone.png")
        val target = File.createTempFile("pack_wrapper_target", "").apply {
            delete()
            mkdirs()
        }
        try {
            texture.parentFile?.mkdirs()
            texture.writeBytes(byteArrayOf(1, 2, 3))
            File(wrapper, "pack.mcmeta").apply {
                parentFile?.mkdirs()
                writeText("{\"pack\":{\"pack_format\":34,\"description\":\"demo\"}}")
            }
            runBlocking {
                ResourcePackConverterEngine.convertPack(
                    source,
                    target,
                    PackConversionConfig(PackPlatform.BEDROCK)
                ) { }
            }
            assertTrue(File(target, "manifest.json").exists())
            assertTrue(File(target, "textures/blocks/stone.png").exists())
            assertFalse(File(target, "MyPack").exists())
        } finally {
            source.deleteRecursively()
            target.deleteRecursively()
        }
    }
}
