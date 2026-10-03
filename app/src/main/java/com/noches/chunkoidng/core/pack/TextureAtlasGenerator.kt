package com.noches.chunkoidng.core.pack

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object TextureAtlasGenerator {

    /**
     * Bedrock atlas keys must be stable across runs.  Using only the file name
     * makes textures/blocks/foo.png and textures/blocks/custom/foo.png collide.
     */
    fun atlasKeyForPath(path: String): String {
        val normalized = path.replace('\\', '/')
        val withoutExtension = normalized.substringBeforeLast('.')
        val relative = when {
            withoutExtension.startsWith("textures/blocks/") -> {
                withoutExtension.removePrefix("textures/blocks/")
            }
            withoutExtension.startsWith("textures/items/") -> {
                withoutExtension.removePrefix("textures/items/")
            }
            else -> withoutExtension.substringAfterLast('/')
        }
        return relative
            .replace('/', '_')
            .replace(Regex("[^A-Za-z0-9_.-]"), "_")
    }

    private fun texturePathWithoutExtension(path: String): String {
        return path.replace('\\', '/').substringBeforeLast('.')
    }

    private fun uniqueAtlasEntries(paths: Set<String>): List<Pair<String, String>> {
        val used = mutableSetOf<String>()
        return paths.map { it.replace('\\', '/') }.sorted().map { path ->
            val base = atlasKeyForPath(path).ifBlank { "texture" }
            var key = base
            var suffix = 2
            while (!used.add(key)) {
                key = "${base}_$suffix"
                suffix++
            }
            path to key
        }
    }

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
        for ((path, tileName) in uniqueAtlasEntries(blockTexturePaths)) {
            val texturePathWithoutExt = texturePathWithoutExtension(path)

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
        for ((path, itemName) in uniqueAtlasEntries(itemTexturePaths)) {
            val texturePathWithoutExt = texturePathWithoutExtension(path)

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
            jsonArray.put(texturePathWithoutExtension(normalized))
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

        for ((path, blockName) in uniqueAtlasEntries(blockTexturePaths)) {
            // A directional texture is only one face of a block. Defining it as
            // a six-sided block changes the vanilla model, so leave those to
            // the game's existing block definition.
            if (blockName.endsWith("_top") ||
                blockName.endsWith("_bottom") ||
                blockName.endsWith("_side") ||
                blockName.endsWith("_front") ||
                blockName.endsWith("_back") ||
                blockName.endsWith("_left") ||
                blockName.endsWith("_right")
            ) {
                continue
            }
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
