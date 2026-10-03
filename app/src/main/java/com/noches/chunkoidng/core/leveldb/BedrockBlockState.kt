package com.noches.chunkoidng.core.leveldb

import br.com.gamemods.nbtmanipulator.*

data class BedrockBlockState(
    val name: String,
    val states: Map<String, Any> = emptyMap(),
    val version: Int = 18090752
) {
    fun toNbt(): NbtCompound {
        val root = NbtCompound()
        root["name"] = NbtString(name)

        val statesTag = NbtCompound()
        for ((key, value) in states) {
            when (value) {
                is Boolean -> statesTag[key] = NbtByte((if (value) 1 else 0).toByte())
                is Byte -> statesTag[key] = NbtByte(value)
                is Short -> statesTag[key] = NbtInt(value.toInt())
                is Int -> statesTag[key] = NbtInt(value)
                is Long -> statesTag[key] = NbtInt(value.toInt())
                is String -> statesTag[key] = NbtString(value)
                else -> statesTag[key] = NbtString(value.toString())
            }
        }
        root["states"] = statesTag
        root["version"] = NbtInt(version)
        return root
    }

    companion object {
        val AIR = BedrockBlockState("minecraft:air")
        val BEDROCK = BedrockBlockState("minecraft:bedrock")
        val DIRT = BedrockBlockState("minecraft:dirt", mapOf("dirt_type" to "normal"))
        val GRASS_BLOCK = BedrockBlockState("minecraft:grass_block")
    }
}
