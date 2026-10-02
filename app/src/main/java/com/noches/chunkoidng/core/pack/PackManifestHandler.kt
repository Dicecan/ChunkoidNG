package com.noches.chunkoidng.core.pack

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object PackManifestHandler {

    fun detectPlatform(paths: Collection<String>): PackPlatform? {
        val normalized = paths.map { it.replace('\\', '/') }
        if (normalized.any { it == "pack.mcmeta" || it.startsWith("assets/minecraft/") }) {
            return PackPlatform.JAVA
        }
        if (normalized.any { it == "manifest.json" || (it.startsWith("textures/") && !it.startsWith("assets/")) }) {
            return PackPlatform.BEDROCK
        }
        return null
    }

    fun parseJavaPackMcmeta(mcmetaContent: String): Pair<Int?, String?> {
        return try {
            val root = JSONObject(mcmetaContent)
            val pack = root.optJSONObject("pack")
            val format = pack?.optInt("pack_format")
            val desc = pack?.optString("description")
            Pair(format, desc)
        } catch (_: Exception) {
            Pair(null, null)
        }
    }

    fun parseBedrockManifest(manifestContent: String): Triple<String?, String?, BedrockEngineVersion?> {
        return try {
            val root = JSONObject(manifestContent)
            val header = root.optJSONObject("header")
            val name = header?.optString("name")
            val desc = header?.optString("description")
            val engineVerArr = header?.optJSONArray("min_engine_version")
            val engineVer = if (engineVerArr != null && engineVerArr.length() >= 3) {
                BedrockEngineVersion(
                    engineVerArr.getInt(0),
                    engineVerArr.getInt(1),
                    engineVerArr.getInt(2)
                )
            } else {
                null
            }
            Triple(name, desc, engineVer)
        } catch (_: Exception) {
            Triple(null, null, null)
        }
    }

    fun generateBedrockManifest(
        name: String,
        description: String,
        engineVersion: BedrockEngineVersion = BedrockEngineVersion.V_1_21_0,
        headerUuid: String = UUID.randomUUID().toString(),
        moduleUuid: String = UUID.randomUUID().toString()
    ): String {
        val root = JSONObject()
        root.put("format_version", 2)

        val header = JSONObject()
        header.put("name", name)
        header.put("description", description)
        header.put("uuid", headerUuid)

        val verArray = JSONArray()
        verArray.put(1)
        verArray.put(0)
        verArray.put(0)
        header.put("version", verArray)

        val engineArray = JSONArray()
        engineArray.put(engineVersion.major)
        engineArray.put(engineVersion.minor)
        engineArray.put(engineVersion.patch)
        header.put("min_engine_version", engineArray)
        root.put("header", header)

        val modules = JSONArray()
        val module = JSONObject()
        module.put("type", "resources")
        module.put("uuid", moduleUuid)
        module.put("version", verArray)
        modules.put(module)
        root.put("modules", modules)

        return root.toString(2)
    }

    fun generateJavaPackMcmeta(
        format: Int,
        description: String
    ): String {
        val root = JSONObject()
        val pack = JSONObject()
        pack.put("pack_format", format)
        pack.put("description", description)
        root.put("pack", pack)
        return root.toString(2)
    }
}
