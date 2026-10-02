package com.noches.chunkoidng.core.pack

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object TextureAtlasGenerator {

    fun generateTerrainTextureJson(
        blockTexturePaths: Set<String>,
        outputFile: File
    ) {
        val root = JSONObject()
        root.put("resource_pack_name", "vanilla")
        root.put("texture_name", "atlas.terrain")
        root.put("padding", 8)
        root.put("num_mip_levels", 4)

        val textureData = JSONObject()
        for (path in blockTexturePaths.sorted()) {
            val normalized = path.replace('\\', '/')
            val tileName = normalized.substringAfterLast('/').substringBeforeLast('.')
            val texturePathWithoutExt = normalized.substringBeforeLast('.')

            val tileObj = JSONObject()
            tileObj.put("textures", texturePathWithoutExt)
            textureData.put(tileName, tileObj)
        }
        root.put("texture_data", textureData)

        outputFile.parentFile?.mkdirs()
        outputFile.writeText(root.toString(2))
    }

    fun generateItemTextureJson(
        itemTexturePaths: Set<String>,
        outputFile: File
    ) {
        val root = JSONObject()
        root.put("resource_pack_name", "vanilla")
        root.put("texture_name", "atlas.items")

        val textureData = JSONObject()
        for (path in itemTexturePaths.sorted()) {
            val normalized = path.replace('\\', '/')
            val itemName = normalized.substringAfterLast('/').substringBeforeLast('.')
            val texturePathWithoutExt = normalized.substringBeforeLast('.')

            val itemObj = JSONObject()
            itemObj.put("textures", texturePathWithoutExt)
            textureData.put(itemName, itemObj)
        }
        root.put("texture_data", textureData)

        outputFile.parentFile?.mkdirs()
        outputFile.writeText(root.toString(2))
    }

    fun generateTexturesListJson(
        allTexturePaths: Set<String>,
        outputFile: File
    ) {
        val jsonArray = JSONArray()
        for (path in allTexturePaths.sorted()) {
            val normalized = path.replace('\\', '/')
            jsonArray.put(normalized.substringBeforeLast('.'))
        }
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(jsonArray.toString(2))
    }

    fun generateBlocksJson(
        blockTexturePaths: Set<String>,
        outputFile: File
    ) {
        val root = JSONObject()
        root.put("format_version", "1.1.0")

        for (path in blockTexturePaths.sorted()) {
            val normalized = path.replace('\\', '/')
            val blockName = normalized.substringAfterLast('/').substringBeforeLast('.')
            val blockObj = JSONObject()
            blockObj.put("sound", "stone")

            val texturesObj = JSONObject()
            texturesObj.put("up", blockName)
            texturesObj.put("down", blockName)
            texturesObj.put("north", blockName)
            texturesObj.put("south", blockName)
            texturesObj.put("west", blockName)
            texturesObj.put("east", blockName)
            blockObj.put("textures", texturesObj)

            root.put(blockName, blockObj)
        }

        outputFile.parentFile?.mkdirs()
        outputFile.writeText(root.toString(2))
    }
}
