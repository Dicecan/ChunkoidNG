package com.noches.chunkoidng.core.pack

import org.json.JSONArray
import org.json.JSONObject

/** Converts the subset of Java block models that has a direct blocks.json equivalent. */
object JavaModelHandler {

    data class Model(
        val id: String,
        val parent: String?,
        val textures: Map<String, String>
    )

    data class BlockDefinition(
        val name: String,
        val textures: Map<String, String>
    )

    private val modelPathRegex = Regex(
        "^assets/([^/]+)/models/block/(.+)\\.json$",
        RegexOption.IGNORE_CASE
    )
    private val blockStatePathRegex = Regex(
        "^assets/([^/]+)/blockstates/([^/]+)\\.json$",
        RegexOption.IGNORE_CASE
    )

    fun parseModel(relativePath: String, content: String): Model? {
        val match = modelPathRegex.matchEntire(relativePath.replace('\\', '/')) ?: return null
        val root = JSONObject(content)
        val namespace = match.groupValues[1].lowercase()
        val modelPath = match.groupValues[2].lowercase()
        val id = "$namespace:block/$modelPath"
        val parentValue = if (root.has("parent")) root.optString("parent") else null
        val parent = parentValue?.takeIf { it.isNotBlank() }?.let {
            qualifyId(namespace, it)
        }
        val textureObject = root.optJSONObject("textures")
        val textures = linkedMapOf<String, String>()
        if (textureObject != null) {
            val keys = textureObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = textureObject.optString(key).trim()
                if (value.isNotEmpty()) textures[key] = value
            }
        }
        return Model(id, parent, textures)
    }

    fun parseBlockState(relativePath: String, content: String): Pair<String, String>? {
        val match = blockStatePathRegex.matchEntire(relativePath.replace('\\', '/')) ?: return null
        val root = JSONObject(content)
        val variants = root.optJSONObject("variants")
        if (variants != null) {
            val variantKeys = variants.keys().asSequence().toList().sorted()
            for (variantKey in variantKeys) {
                val value = variants.opt(variantKey)
                val model = when (value) {
                    is JSONObject -> value.optString("model")
                    is JSONArray -> if (value.length() > 0) value.optJSONObject(0)?.optString("model") else ""
                    else -> ""
                }
                if (!model.isNullOrBlank()) {
                    return "${match.groupValues[1].lowercase()}:${match.groupValues[2].lowercase()}" to
                        qualifyId(match.groupValues[1], model)
                }
            }
        }
        return null
    }

    fun resolveDefinitions(
        models: Map<String, Model>,
        blockStateModels: Map<String, String>,
        availableTexturePaths: Set<String>
    ): List<BlockDefinition> {
        val result = linkedMapOf<String, BlockDefinition>()
        val blockEntries = linkedSetOf<Pair<String, String>>()
        blockEntries.addAll(blockStateModels.map { it.key to it.value })
        val referencedModels = blockStateModels.values.toSet()
        blockEntries.addAll(
            models.keys
                .filter { it.substringAfter(':').startsWith("block/") && it !in referencedModels }
                .map { it to it }
        )

        for ((blockId, modelId) in blockEntries.sortedBy { it.first }) {
            val model = resolveModel(modelId, models, mutableSetOf())
            if (model.isEmpty()) continue
            val blockName = safeBlockName(blockId)
            val faceTextures = linkedMapOf<String, String>()
            for ((face, rawTextureRef) in model) {
                val textureRef = resolveTextureReference(rawTextureRef, model)
                val texturePath = javaTextureToBedrockPath(textureRef, blockId.substringBefore(':'))
                if (texturePath != null && availableTexturePaths.contains(texturePath)) {
                    faceTextures[face] = TextureAtlasGenerator.atlasKeyForPath(texturePath)
                }
            }
            if (faceTextures.isNotEmpty()) {
                result[blockName] = BlockDefinition(blockName, faceTextures)
            }
        }
        return result.values.toList()
    }

    private fun resolveModel(
        modelId: String,
        models: Map<String, Model>,
        visiting: MutableSet<String>
    ): Map<String, String> {
        if (!visiting.add(modelId)) return emptyMap()
        val model = models[modelId] ?: return emptyMap()
        val resolved = linkedMapOf<String, String>()
        if (model.parent != null) resolved.putAll(resolveModel(model.parent, models, visiting))
        resolved.putAll(model.textures)
        visiting.remove(modelId)
        return resolved
    }

    private fun resolveTextureReference(reference: String, textures: Map<String, String>): String {
        var current = reference
        val visited = mutableSetOf<String>()
        while (current.startsWith("#")) {
            if (!visited.add(current)) return ""
            current = textures[current.removePrefix("#")] ?: return ""
        }
        return current
    }

    private fun qualifyId(namespace: String, value: String): String {
        val normalized = value.removePrefix("#").lowercase()
        return if (normalized.contains(':')) normalized else "$namespace:$normalized"
    }

    private fun safeBlockName(id: String): String {
        val parts = id.lowercase().split(':', limit = 2)
        val path = parts.last().removePrefix("block/")
        val raw = if (parts.size == 2 && parts[0] != "minecraft") "${parts[0]}_$path" else path
        return raw.replace(Regex("[^a-z0-9_.-]"), "_")
    }

    private fun javaTextureToBedrockPath(reference: String, defaultNamespace: String): String? {
        var value = reference.removePrefix("#").lowercase()
        if (value.isBlank() || value.startsWith("#")) return null
        val namespace = if (value.contains(':')) value.substringBefore(':') else defaultNamespace
        value = value.substringAfter(':', value)
        value = value.removePrefix("textures/")
        val extension = if (value.endsWith(".png")) "" else ".png"
        val path = when {
            value.startsWith("block/") || value.startsWith("blocks/") ->
                "assets/$namespace/textures/${value}$extension"
            value.startsWith("item/") || value.startsWith("items/") ->
                "assets/$namespace/textures/${value}$extension"
            else -> "assets/$namespace/textures/$value$extension"
        }
        return PackAssetMapper.mapJavaToBedrock(path)
    }
}
