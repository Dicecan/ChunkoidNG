package com.noches.chunkoidng.core.pack

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FlipbookAnimationHandler {

    data class FlipbookEntry(
        val flipbookTexture: String,
        val atlasTile: String,
        val ticksPerFrame: Int = 1,
        val interpolate: Boolean = false,
        val frames: List<Int>? = null
    )

    fun parseJavaAnimationMcmeta(mcmetaContent: String, bedrockTexturePath: String): FlipbookEntry? {
        return try {
            val root = JSONObject(mcmetaContent)
            if (!root.has("animation")) return null

            val anim = root.getJSONObject("animation")
            val frametime = if (anim.has("frametime")) anim.getInt("frametime") else 1
            val interpolate = if (anim.has("interpolate")) anim.getBoolean("interpolate") else false

            val frameList = mutableListOf<Int>()
            if (anim.has("frames")) {
                val framesArr = anim.getJSONArray("frames")
                for (i in 0 until framesArr.length()) {
                    val item = framesArr.get(i)
                    when (item) {
                        is Int -> frameList.add(item)
                        is JSONObject -> {
                            if (item.has("index")) {
                                frameList.add(item.getInt("index"))
                            }
                        }
                    }
                }
            }

            val textureWithoutExt = bedrockTexturePath.substringBeforeLast('.')
            val atlasTile = textureWithoutExt.substringAfterLast('/')

            FlipbookEntry(
                flipbookTexture = textureWithoutExt,
                atlasTile = atlasTile,
                ticksPerFrame = if (frametime > 0) frametime else 1,
                interpolate = interpolate,
                frames = if (frameList.isNotEmpty()) frameList else null
            )
        } catch (_: Exception) {
            null
        }
    }

    fun writeFlipbookTexturesJson(entries: List<FlipbookEntry>, outputFile: File) {
        val root = JSONArray()
        for (entry in entries) {
            val obj = JSONObject()
            obj.put("flipbook_texture", entry.flipbookTexture)
            obj.put("atlas_tile", entry.atlasTile)
            obj.put("ticks_per_frame", entry.ticksPerFrame)
            if (entry.interpolate) {
                obj.put("interpolate", true)
            }
            if (entry.frames != null && entry.frames.isNotEmpty()) {
                val framesArr = JSONArray()
                for (f in entry.frames) {
                    framesArr.put(f)
                }
                obj.put("frames", framesArr)
            }
            root.put(obj)
        }
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(root.toString(2))
    }

    fun parseBedrockFlipbookJson(flipbookContent: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        try {
            val array = JSONArray(flipbookContent)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val flipbookTexture = obj.getString("flipbook_texture")
                val ticks = if (obj.has("ticks_per_frame")) obj.getInt("ticks_per_frame") else 1
                val interpolate = if (obj.has("interpolate")) obj.getBoolean("interpolate") else false

                val mcmetaRoot = JSONObject()
                val animObj = JSONObject()
                animObj.put("frametime", ticks)
                if (interpolate) {
                    animObj.put("interpolate", true)
                }

                if (obj.has("frames")) {
                    val frames = obj.getJSONArray("frames")
                    animObj.put("frames", frames)
                }

                mcmetaRoot.put("animation", animObj)
                val relativeMcmetaPath = "$flipbookTexture.png.mcmeta"
                result.add(relativeMcmetaPath to mcmetaRoot.toString(2))
            }
        } catch (_: Exception) {}
        return result
    }
}
