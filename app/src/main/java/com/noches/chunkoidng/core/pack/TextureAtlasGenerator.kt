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
        outputFile: File,
        modelDefinitions: List<JavaModelHandler.BlockDefinition> = emptyList()
    ) {
        val root = JSONObject()
        root.put("format_version", "1.1.0")

        val definedNames = mutableSetOf<String>()
        for (definition in modelDefinitions) {
            val blockObj = JSONObject()
            blockObj.put("sound", "stone")
            val texturesObj = JSONObject()
            val textures = definition.textures
            if (textures.containsKey("all")) {
                blockObj.put("textures", textures["all"])
            } else {
                for (face in listOf("up", "down", "north", "south", "west", "east")) {
                    val key = textures[face] ?: textures["side"] ?: textures["all"] ?: continue
                    texturesObj.put(face, key)
                }
                if (texturesObj.length() > 0) blockObj.put("textures", texturesObj)
            }
            if (blockObj.has("textures")) {
                root.put(definition.name, blockObj)
                definedNames.add(definition.name)
            }
        }

        val grouped = linkedMapOf<String, MutableMap<String, String>>()
        for ((_, textureName) in uniqueAtlasEntries(blockTexturePaths)) {
            val (base, face) = directionalTextureName(textureName)
            grouped.getOrPut(base) { linkedMapOf() }[face] = textureName
        }
        for ((blockName, faceTextures) in grouped) {
            if (definedNames.contains(blockName)) continue
            val fallback = faceTextures["all"] ?: faceTextures["side"] ?: faceTextures.values.firstOrNull() ?: continue
            val up = faceTextures["up"] ?: faceTextures["top"] ?: fallback
            val down = faceTextures["down"] ?: faceTextures["bottom"] ?: fallback
            val side = faceTextures["side"] ?: faceTextures["front"] ?: fallback

            val blockObj = JSONObject()
            blockObj.put("sound", defaultSoundForBlock(blockName))
            val texturesObj = JSONObject()
            texturesObj.put("up", up)
            texturesObj.put("down", down)
            texturesObj.put("north", faceTextures["north"] ?: side)
            texturesObj.put("south", faceTextures["south"] ?: side)
            texturesObj.put("west", faceTextures["west"] ?: side)
            texturesObj.put("east", faceTextures["east"] ?: side)
            blockObj.put("textures", texturesObj)
            root.put(blockName, blockObj)
        }

        outputFile.parentFile?.mkdirs()
        outputFile.writeText(root.toString(2))
    }

    private fun directionalTextureName(name: String): Pair<String, String> {
        val suffixes = listOf(
            "_top" to "top",
            "_bottom" to "bottom",
            "_side" to "side",
            "_front" to "front",
            "_back" to "back",
            "_left" to "left",
            "_right" to "right",
            "_up" to "up",
            "_down" to "down"
        )
        val suffix = suffixes.firstOrNull { name.endsWith(it.first) }
        return if (suffix == null) name to "all" else name.removeSuffix(suffix.first) to suffix.second
    }

    private fun defaultSoundForBlock(name: String): String {
        return when {
            name.contains("glass") || name.contains("ice") -> "glass"
            name.contains("wool") || name.contains("cloth") -> "cloth"
            name.contains("sand") -> "sand"
            name.contains("gravel") -> "gravel"
            name.contains("snow") -> "snow"
            name.contains("metal") || name.contains("iron") || name.contains("gold") -> "metal"
            name.contains("plank") || name.contains("wood") || name.contains("log") -> "wood"
            name.contains("grass") || name.contains("leaves") -> "grass"
            else -> "stone"
        }
    }
}
