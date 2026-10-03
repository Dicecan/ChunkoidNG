package com.noches.chunkoidng.core.pack

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object PackLangHandler {

    fun convertJavaJsonToBedrockLang(jsonContent: String): String {
        return try {
            val json = JSONObject(jsonContent)
            val builder = StringBuilder()
            val keys = json.keys()
            val sortedKeys = mutableListOf<String>()
            while (keys.hasNext()) {
                sortedKeys.add(keys.next())
            }
            sortedKeys.sort()

            for (key in sortedKeys) {
                val value = json.optString(key, "")
                val bedrockKey = mapJavaKeyToBedrock(key)
                val sanitizedValue = value.replace("\r", "").replace("\n", "\\n")
                builder.append("$bedrockKey=$sanitizedValue\n")
            }
            builder.toString()
        } catch (cause: Exception) {
            throw IllegalArgumentException("Invalid Java language JSON", cause)
        }
    }

    fun convertBedrockLangToJavaJson(langContent: String): String {
        return try {
            val json = JSONObject()
            val lines = langContent.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//") || !trimmed.contains('=')) {
                    continue
                }
                val equalIndex = trimmed.indexOf('=')
                val key = trimmed.substring(0, equalIndex).trim()
                val value = trimmed.substring(equalIndex + 1).replace("\\n", "\n")
                val javaKey = mapBedrockKeyToJava(key)
                json.put(javaKey, value)
            }
            json.toString(2)
        } catch (cause: Exception) {
            throw IllegalArgumentException("Invalid Bedrock language file", cause)
        }
    }

    fun generateLanguagesJson(locales: List<String>, outputFile: File) {
        val array = JSONArray()
        for (loc in locales) {
            array.put(loc)
        }
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(array.toString(2))
    }

    private fun mapJavaKeyToBedrock(key: String): String {
        if (key.startsWith("block.minecraft.")) {
            val blockName = key.removePrefix("block.minecraft.")
            return "tile.$blockName.name"
        }
        if (key.startsWith("item.minecraft.")) {
            val itemName = key.removePrefix("item.minecraft.")
            return "item.$itemName.name"
        }
        if (key.startsWith("entity.minecraft.")) {
            val entityName = key.removePrefix("entity.minecraft.")
            return "entity.$entityName.name"
        }
        return key
    }

    private fun mapBedrockKeyToJava(key: String): String {
        if (key.startsWith("tile.") && key.endsWith(".name")) {
            val blockName = key.removePrefix("tile.").removeSuffix(".name")
            return "block.minecraft.$blockName"
        }
        if (key.startsWith("item.") && key.endsWith(".name")) {
            val itemName = key.removePrefix("item.").removeSuffix(".name")
            return "item.minecraft.$itemName"
        }
        if (key.startsWith("entity.") && key.endsWith(".name")) {
            val entityName = key.removePrefix("entity.").removeSuffix(".name")
            return "entity.minecraft.$entityName"
        }
        return key
    }
}
