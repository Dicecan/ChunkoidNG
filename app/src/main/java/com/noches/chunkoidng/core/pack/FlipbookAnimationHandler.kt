package com.noches.chunkoidng.core.pack

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FlipbookAnimationHandler {

    private const val MAX_EXPANDED_FRAMES = 100_000

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
            val frametime = if (anim.has("frametime")) anim.getInt("frametime").coerceIn(1, 1024) else 1
            val interpolate = if (anim.has("interpolate")) anim.getBoolean("interpolate") else false

            val frameSpecs = mutableListOf<Pair<Int, Int>>()
            if (anim.has("frames")) {
                val framesArr = anim.getJSONArray("frames")
                if (framesArr.length() > MAX_EXPANDED_FRAMES) {
                    throw IllegalArgumentException("Animation contains too many frames")
                }
                for (i in 0 until framesArr.length()) {
                    val item = framesArr.get(i)
                    when (item) {
                        is Int -> frameSpecs.add(item to frametime)
                        is JSONObject -> {
                            if (item.has("index")) {
                                val index = item.getInt("index")
                                val frameTime = item.optInt("time", frametime).coerceIn(1, 1024)
                                frameSpecs.add(index to frameTime)
                            }
                        }
                    }
                }
            }
            val hasPerFrameTiming = frameSpecs.any { it.second != frametime }
            val frameList = if (hasPerFrameTiming) {
                buildList {
                    frameSpecs.forEach { (index, frameTime) ->
                        repeat(frameTime) {
                            if (size < MAX_EXPANDED_FRAMES) add(index)
                        }
                    }
                }
            } else {
                frameSpecs.map { it.first }
            }

            val textureWithoutExt = bedrockTexturePath.substringBeforeLast('.')
            val atlasTile = TextureAtlasGenerator.atlasKeyForPath(bedrockTexturePath)

            FlipbookEntry(
                flipbookTexture = textureWithoutExt,
                atlasTile = atlasTile,
                // Bedrock has one duration for the whole flipbook. Expand
                // Java's per-frame durations above and use one tick per entry.
                ticksPerFrame = if (hasPerFrameTiming) 1 else frametime.coerceAtLeast(1),
                interpolate = interpolate,
                frames = frameList.takeIf { it.isNotEmpty() }
            )
        } catch (cause: Exception) {
            throw IllegalArgumentException("Invalid Java animation metadata", cause)
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
            if (array.length() > MAX_EXPANDED_FRAMES) {
                throw IllegalArgumentException("Flipbook list is too large")
            }
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val flipbookTexture = obj.getString("flipbook_texture")
                val normalizedTexture = flipbookTexture.replace('\\', '/')
                if (!normalizedTexture.startsWith("textures/") ||
                    normalizedTexture.split('/').any { it == ".." || it.isBlank() }
                ) {
                    throw IllegalArgumentException("Invalid flipbook texture path")
                }
                val ticks = if (obj.has("ticks_per_frame")) {
                    obj.getInt("ticks_per_frame").coerceIn(1, 1024)
                } else 1
                val interpolate = if (obj.has("interpolate")) obj.getBoolean("interpolate") else false

                val mcmetaRoot = JSONObject()
                val animObj = JSONObject()
                animObj.put("frametime", ticks)
                if (interpolate) {
                    animObj.put("interpolate", true)
                }

                if (obj.has("frames")) {
                    val frames = obj.getJSONArray("frames")
                    if (frames.length() > MAX_EXPANDED_FRAMES) {
                        throw IllegalArgumentException("Flipbook frame list is too large")
                    }
                    animObj.put("frames", frames)
                }

                mcmetaRoot.put("animation", animObj)
                val relativeMcmetaPath = "$normalizedTexture.png.mcmeta"
                result.add(relativeMcmetaPath to mcmetaRoot.toString(2))
            }
        } catch (cause: Exception) {
            throw IllegalArgumentException("Invalid Bedrock flipbook metadata", cause)
        }
        return result
    }
}
