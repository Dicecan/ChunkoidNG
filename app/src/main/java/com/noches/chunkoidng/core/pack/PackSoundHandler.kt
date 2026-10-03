package com.noches.chunkoidng.core.pack

import org.json.JSONArray
import org.json.JSONObject

object PackSoundHandler {

    fun convertJavaSoundsToBedrock(javaSoundsJson: String): String {
        return try {
            val root = JSONObject(javaSoundsJson)
            val result = JSONObject()
            result.put("format_version", "1.14.0")

            val soundDefinitions = JSONObject()
            val keys = root.keys()

            while (keys.hasNext()) {
                val soundEvent = keys.next()
                val eventObj = root.optJSONObject(soundEvent) ?: continue

                val bedrockEvent = JSONObject()
                if (eventObj.has("category")) {
                    bedrockEvent.put("category", eventObj.getString("category"))
                }
                copyFields(eventObj, bedrockEvent, listOf("subtitle", "replace"))

                if (eventObj.has("sounds")) {
                    val soundsArr = eventObj.getJSONArray("sounds")
                    val bedrockSoundsArr = JSONArray()

                    for (i in 0 until soundsArr.length()) {
                        val soundItem = soundsArr.get(i)
                        when (soundItem) {
                            is String -> {
                                val path = if (soundItem.startsWith("sounds/")) soundItem else "sounds/$soundItem"
                                bedrockSoundsArr.put(path)
                            }
                            is JSONObject -> {
                                val convertedItem = JSONObject()
                                if (soundItem.has("name")) {
                                    val name = soundItem.getString("name")
                                    val path = if (name.startsWith("sounds/")) name else "sounds/$name"
                                    convertedItem.put("name", path)
                                }
                                if (soundItem.has("volume")) convertedItem.put("volume", soundItem.getDouble("volume"))
                                if (soundItem.has("pitch")) convertedItem.put("pitch", soundItem.getDouble("pitch"))
                                if (soundItem.has("weight")) convertedItem.put("weight", soundItem.getInt("weight"))
                                copyFields(soundItem, convertedItem, listOf("stream", "preload", "attenuation_distance", "type"))
                                bedrockSoundsArr.put(convertedItem)
                            }
                        }
                    }
                    bedrockEvent.put("sounds", bedrockSoundsArr)
                }

                soundDefinitions.put(soundEvent, bedrockEvent)
            }

            result.put("sound_definitions", soundDefinitions)
            result.toString(2)
        } catch (cause: Exception) {
            throw IllegalArgumentException("Invalid Java sounds.json", cause)
        }
    }

    fun convertBedrockSoundsToJava(bedrockSoundsJson: String): String {
        return try {
            val root = JSONObject(bedrockSoundsJson)
            val defs = if (root.has("sound_definitions")) root.getJSONObject("sound_definitions") else root
            val result = JSONObject()
            val keys = defs.keys()

            while (keys.hasNext()) {
                val soundEvent = keys.next()
                val eventObj = defs.optJSONObject(soundEvent) ?: continue

                val javaEvent = JSONObject()
                if (eventObj.has("category")) {
                    javaEvent.put("category", eventObj.getString("category"))
                }
                copyFields(eventObj, javaEvent, listOf("subtitle", "replace"))

                if (eventObj.has("sounds")) {
                    val soundsArr = eventObj.getJSONArray("sounds")
                    val javaSoundsArr = JSONArray()

                    for (i in 0 until soundsArr.length()) {
                        val soundItem = soundsArr.get(i)
                        when (soundItem) {
                            is String -> {
                                val path = soundItem.removePrefix("sounds/")
                                javaSoundsArr.put(path)
                            }
                            is JSONObject -> {
                                val convertedItem = JSONObject()
                                if (soundItem.has("name")) {
                                    val name = soundItem.getString("name").removePrefix("sounds/")
                                    convertedItem.put("name", name)
                                }
                                if (soundItem.has("volume")) convertedItem.put("volume", soundItem.getDouble("volume"))
                                if (soundItem.has("pitch")) convertedItem.put("pitch", soundItem.getDouble("pitch"))
                                if (soundItem.has("weight")) convertedItem.put("weight", soundItem.getInt("weight"))
                                copyFields(soundItem, convertedItem, listOf("stream", "preload", "attenuation_distance", "type"))
                                javaSoundsArr.put(convertedItem)
                            }
                        }
                    }
                    javaEvent.put("sounds", javaSoundsArr)
                }

                result.put(soundEvent, javaEvent)
            }

            result.toString(2)
        } catch (cause: Exception) {
            throw IllegalArgumentException("Invalid Bedrock sound definitions", cause)
        }
    }

    private fun copyFields(source: JSONObject, target: JSONObject, names: List<String>) {
        for (name in names) {
            if (source.has(name)) target.put(name, source.get(name))
        }
    }
}
