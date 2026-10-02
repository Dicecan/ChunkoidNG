package com.noches.chunkoidng

import com.noches.chunkoidng.core.pack.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

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
}
