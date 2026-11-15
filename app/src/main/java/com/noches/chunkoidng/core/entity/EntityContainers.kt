package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.NbtByte
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtList

object EntityContainers {

    private val CONTAINER_VEHICLES = setOf(
        "minecraft:chest_minecart",
        "minecraft:hopper_minecart",
        "minecraft:chest_boat"
    )

    private val CHESTED_ANIMALS = setOf(
        "minecraft:donkey",
        "minecraft:mule",
        "minecraft:llama",
        "minecraft:trader_llama"
    )

    fun apply(actor: NbtCompound, javaEntity: NbtCompound, bedrockId: String) {
        when (bedrockId) {
            in CONTAINER_VEHICLES -> {
                val items = convertJavaItems(javaEntity.getNullableCompoundList("Items"))
                if (items.isNotEmpty()) {
                    actor["ChestItems"] = NbtList(items)
                }
            }
            in CHESTED_ANIMALS -> {
                val rawItems = javaEntity.getNullableCompoundList("Items")
                val chestedFlag = (javaEntity.getNullableByte("ChestedHorse")
                    ?: javaEntity.getNullableByte("Chested")
                    ?: 0) == 1.toByte()
                val items = convertJavaItems(rawItems)
                if (chestedFlag || items.isNotEmpty()) {
                    actor["Chested"] = NbtByte(1)
                }
                if (items.isNotEmpty()) {
                    actor["Items"] = NbtList(items)
                }
            }
        }
    }

    fun applyToJava(
        javaEntity: NbtCompound,
        actor: NbtCompound,
        bedrockId: String,
        modernComponents: Boolean = true,
        flatComponents: Boolean = true
    ) {
        when (bedrockId) {
            in CONTAINER_VEHICLES -> {
                val rawItems = actor.getNullableCompoundList("ChestItems")
                    ?: actor.getNullableCompoundList("Items")
                val items = convertBedrockItems(rawItems, modernComponents, flatComponents)
                if (items.isNotEmpty()) {
                    javaEntity["Items"] = NbtList(items)
                }
            }
            in CHESTED_ANIMALS -> {
                val chestedFlag = (actor.getNullableByte("Chested") ?: 0) == 1.toByte()
                val rawItems = actor.getNullableCompoundList("Items")
                    ?: actor.getNullableCompoundList("ChestItems")
                val items = convertBedrockItems(rawItems, modernComponents, flatComponents)
                if (chestedFlag || items.isNotEmpty()) {
                    javaEntity["ChestedHorse"] = NbtByte(1)
                }
                if (items.isNotEmpty()) {
                    javaEntity["Items"] = NbtList(items)
                }
            }
        }
    }

    private fun convertJavaItems(rawItems: NbtList<NbtCompound>?): List<NbtCompound> {
        if (rawItems.isNullOrEmpty()) return emptyList()
        val out = ArrayList<NbtCompound>(rawItems.size)
        for (raw in rawItems) {
            val slot = raw.getNullableByte("Slot") ?: raw.getNullableInt("Slot")?.toByte() ?: continue
            if (slot < 0) continue
            val converted = EntityEquipment.convertItem(raw) ?: continue
            converted["Slot"] = NbtByte(slot)
            out.add(converted)
        }
        return out
    }

    private fun convertBedrockItems(
        rawItems: NbtList<NbtCompound>?,
        modernComponents: Boolean,
        flatComponents: Boolean
    ): List<NbtCompound> {
        if (rawItems.isNullOrEmpty()) return emptyList()
        val out = ArrayList<NbtCompound>(rawItems.size)
        for (raw in rawItems) {
            val slot = raw.getNullableByte("Slot") ?: raw.getNullableInt("Slot")?.toByte() ?: continue
            if (slot < 0) continue
            val converted = EntityEquipment.convertBedrockItemToJava(raw, modernComponents, flatComponents)
                ?: continue
            converted["Slot"] = NbtByte(slot)
            out.add(converted)
        }
        return out
    }
}
