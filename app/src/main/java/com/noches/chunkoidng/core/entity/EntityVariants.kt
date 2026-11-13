package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.NbtByte
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtInt
import br.com.gamemods.nbtmanipulator.NbtShort

object EntityVariants {

    private const val NAMESPACE = "minecraft:"

    private val DYE_COLOURS = listOf(
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    )

    private val HORSE_COLOURS = listOf(
        "white", "creamy", "chestnut", "brown", "black", "gray", "darkbrown"
    )

    private val HORSE_MARKINGS = listOf(
        "none", "white_details", "white_fields", "white_dots", "black_dots"
    )

    private val RABBIT_COATS = listOf(
        "brown", "white", "black", "splotched", "desert", "salt"
    )

    private val PARROT_COLOURS = listOf(
        "red", "blue", "green", "cyan", "silver"
    )

    private val LLAMA_COLOURS = listOf(
        "creamy", "white", "brown", "gray"
    )

    private val AXOLOTL_COLOURS = listOf(
        "lucy", "wild", "gold", "cyan", "blue"
    )

    private val CAT_VARIANTS = mapOf(
        "white" to Pair("cat_white", 0),
        "black" to Pair("cat_tuxedo", 1),
        "tuxedo" to Pair("cat_tuxedo", 1),
        "red" to Pair("cat_red", 2),
        "siamese" to Pair("cat_siamese", 3),
        "british" to Pair("cat_british", 4),
        "british_shorthair" to Pair("cat_british", 4),
        "calico" to Pair("cat_calico", 5),
        "persian" to Pair("cat_persian", 6),
        "ragdoll" to Pair("cat_ragdoll", 7),
        "tabby" to Pair("cat_tabby", 8),
        "all_black" to Pair("cat_black", 9),
        "jellie" to Pair("cat_jellie", 10)
    )

    private val BOAT_WOODS = listOf(
        "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove",
        "bamboo", "cherry", "pale_oak", "poplar"
    )

    fun apply(
        actor: NbtCompound,
        identifier: String,
        javaEntity: NbtCompound,
        definitions: MutableList<String>
    ) {
        when (identifier) {
            "minecraft:sheep" -> sheep(actor, javaEntity, definitions)
            "minecraft:horse" -> horse(actor, javaEntity, definitions)
            "minecraft:mooshroom" -> mooshroom(actor, javaEntity, definitions)
            "minecraft:cat" -> cat(actor, javaEntity, definitions)
            "minecraft:wolf" -> wolf(actor, javaEntity, definitions)
            "minecraft:fox" -> fox(actor, javaEntity, definitions)
            "minecraft:rabbit" -> rabbit(actor, javaEntity, definitions)
            "minecraft:parrot" -> parrot(actor, javaEntity, definitions)
            "minecraft:llama", "minecraft:trader_llama" -> llama(actor, identifier, javaEntity, definitions)
            "minecraft:axolotl" -> axolotl(actor, javaEntity, definitions)
            "minecraft:frog" -> frog(actor, javaEntity, definitions)
            "minecraft:goat" -> goat(actor, javaEntity, definitions)
            "minecraft:panda" -> panda(actor, javaEntity, definitions)
            "minecraft:tropicalfish" -> tropicalFish(actor, javaEntity)
            "minecraft:boat", "minecraft:chest_boat" -> boat(actor, javaEntity, definitions)
        }
    }

    private fun sheep(actor: NbtCompound, entity: NbtCompound, definitions: MutableList<String>) {
        val color = getInt(entity, "Color", 0).coerceIn(0, 15)
        actor["Color"] = NbtByte(color.toByte())
        addGroup(definitions, "minecraft:sheep", "sheep_${DYE_COLOURS[color]}")

        val sheared = getByte(entity, "Sheared", 0)
        if (sheared != 0.toByte()) {
            actor["Sheared"] = NbtByte(1)
            addGroup(definitions, "minecraft:sheep", "sheep_sheared")
        }
    }

    private fun horse(actor: NbtCompound, entity: NbtCompound, definitions: MutableList<String>) {
        val variant = getInt(entity, "Variant", 0)
        val coatIndex = (variant and 0xFF).coerceIn(0, HORSE_COLOURS.size - 1)
        val markIndex = ((variant shr 8) and 0xFF).coerceIn(0, HORSE_MARKINGS.size - 1)

        actor["Variant"] = NbtInt(coatIndex)
        actor["MarkVariant"] = NbtInt(markIndex)

        addGroup(definitions, "minecraft:horse", "base_${HORSE_COLOURS[coatIndex]}")
        if (markIndex > 0) {
            addGroup(definitions, "minecraft:horse", "markings_${HORSE_MARKINGS[markIndex]}")
        }
    }

    private fun mooshroom(actor: NbtCompound, entity: NbtCompound, definitions: MutableList<String>) {
        val type = entity.getNullableString("Type")?.lowercase()
        if (type == "brown") {
            actor["Variant"] = NbtInt(1)
            addGroup(definitions, "minecraft:mooshroom", "mooshroom_brown")
        } else {
            actor["Variant"] = NbtInt(0)
            addGroup(definitions, "minecraft:mooshroom", "mooshroom_red")
        }
    }

    private fun cat(actor: NbtCompound, entity: NbtCompound, definitions: MutableList<String>) {
        val raw = entity.getNullableString("variant") ?: entity.getNullableString("Variant")
        val variantName = raw?.removePrefix(NAMESPACE)?.lowercase() ?: "black"
        val mapped = CAT_VARIANTS[variantName] ?: Pair("cat_tabby", 0)
        actor["Variant"] = NbtInt(mapped.second)
        addGroup(definitions, "minecraft:cat", mapped.first)
    }

    private fun wolf(actor: NbtCompound, entity: NbtCompound, definitions: MutableList<String>) {
        val raw = entity.getNullableString("variant") ?: entity.getNullableString("Variant")
        val variantName = raw?.removePrefix(NAMESPACE)?.lowercase() ?: "pale"
        val group = "wolf_$variantName"
        EntityGroupTable.variant("minecraft:wolf", group)?.let { (_, v) ->
            actor["Variant"] = NbtInt(v)
        }
        addGroup(definitions, "minecraft:wolf", group)
    }

    private fun fox(actor: NbtCompound, entity: NbtCompound, definitions: MutableList<String>) {
        val type = entity.getNullableString("Type")?.lowercase()
        if (type == "snow") {
            actor["Variant"] = NbtInt(1)
            addGroup(definitions, "minecraft:fox", "fox_arctic")
        } else {
            actor["Variant"] = NbtInt(0)
            addGroup(definitions, "minecraft:fox", "fox_red")
        }
    }

    private fun rabbit(actor: NbtCompound, entity: NbtCompound, definitions: MutableList<String>) {
        val type = getInt(entity, "RabbitType", 0)
        if (type in RABBIT_COATS.indices) {
            actor["Variant"] = NbtInt(type)
            addGroup(definitions, "minecraft:rabbit", "coat_${RABBIT_COATS[type]}")
        } else if (type == 99) {
            actor["Variant"] = NbtInt(99)
        }
    }

    private fun parrot(actor: NbtCompound, entity: NbtCompound, definitions: MutableList<String>) {
        val variant = getInt(entity, "Variant", 0).coerceIn(0, PARROT_COLOURS.size - 1)
        actor["Variant"] = NbtInt(variant)
        addGroup(definitions, "minecraft:parrot", "parrot_${PARROT_COLOURS[variant]}")
    }

    private fun llama(
        actor: NbtCompound,
        identifier: String,
        entity: NbtCompound,
        definitions: MutableList<String>
    ) {
        val variant = getInt(entity, "Variant", 0).coerceIn(0, LLAMA_COLOURS.size - 1)
        actor["Variant"] = NbtInt(variant)
        addGroup(definitions, identifier, "llama_${LLAMA_COLOURS[variant]}")
    }

    private fun axolotl(actor: NbtCompound, entity: NbtCompound, definitions: MutableList<String>) {
        val variant = getInt(entity, "Variant", 0).coerceIn(0, AXOLOTL_COLOURS.size - 1)
        actor["Variant"] = NbtInt(variant)
        addGroup(definitions, "minecraft:axolotl", "axolotl_${AXOLOTL_COLOURS[variant]}")
    }

    private fun frog(actor: NbtCompound, entity: NbtCompound, definitions: MutableList<String>) {
        val raw = entity.getNullableString("variant") ?: entity.getNullableString("Variant")
        val variantName = raw?.removePrefix(NAMESPACE)?.lowercase() ?: "temperate"
        val (group, variantInt) = when (variantName) {
            "cold" -> "cold_frog" to 1
            "warm" -> "warm_frog" to 2
            else -> "temperate_frog" to 0
        }
        actor["Variant"] = NbtInt(variantInt)
        addGroup(definitions, "minecraft:frog", group)
    }

    private fun goat(actor: NbtCompound, entity: NbtCompound, definitions: MutableList<String>) {
        val isScreamer = getByte(entity, "IsScreamingGoat", 0) != 0.toByte() ||
                getInt(entity, "IsScreamingGoat", 0) != 0
        if (isScreamer) {
            actor["Variant"] = NbtInt(1)
            addGroup(definitions, "minecraft:goat", "goat_screamer")
        } else {
            actor["Variant"] = NbtInt(0)
            addGroup(definitions, "minecraft:goat", "goat_default")
        }
    }

    private fun panda(actor: NbtCompound, entity: NbtCompound, definitions: MutableList<String>) {
        val gene = entity.getNullableString("MainGene")?.lowercase()
        if (gene != null && gene != "normal") {
            addGroup(definitions, "minecraft:panda", "panda_$gene")
        }
    }

    private fun tropicalFish(actor: NbtCompound, entity: NbtCompound) {
        val variant = getInt(entity, "Variant", 0)
        actor["Variant"] = NbtInt(variant)
    }

    private fun boat(actor: NbtCompound, entity: NbtCompound, definitions: MutableList<String>) {
        val variant = resolveBoatVariant(entity)
        actor["Variant"] = NbtInt(variant)
        val group = if (variant == 7) "can_ride_bamboo" else "can_ride_default"
        val id = entity.getNullableString("id")?.removePrefix(NAMESPACE) ?: ""
        val targetId = if (id.contains("chest")) "minecraft:chest_boat" else "minecraft:boat"
        addGroup(definitions, targetId, group)
    }

    private fun resolveBoatVariant(entity: NbtCompound): Int {
        val javaId = entity.getNullableString("id")?.removePrefix(NAMESPACE) ?: "boat"
        if (javaId == "bamboo_raft" || javaId == "bamboo_chest_raft") return 7
        for (i in BOAT_WOODS.indices) {
            if (javaId.startsWith(BOAT_WOODS[i])) return i
        }
        val typeStr = entity.getNullableString("Type")?.lowercase()?.removePrefix(NAMESPACE)
        if (typeStr != null) {
            val idx = BOAT_WOODS.indexOf(typeStr)
            if (idx >= 0) return idx
        }
        return 0
    }

    fun addGroup(definitions: MutableList<String>, bareGroup: String) {
        val full = "+$NAMESPACE$bareGroup"
        if (full !in definitions) {
            definitions.add(full)
        }
    }

    fun addGroup(definitions: MutableList<String>, identifier: String, bareGroup: String) {
        val full = "+$NAMESPACE$bareGroup"
        if (full in definitions) return
        val row = EntityGroupTable.of(identifier)
        if (row != null && row.hasGroup(bareGroup)) {
            definitions.add(full)
        }
    }

    private fun getInt(compound: NbtCompound, key: String, default: Int): Int {
        val tag = compound[key] ?: return default
        return when (tag) {
            is NbtInt -> tag.value
            is NbtByte -> tag.signed.toInt()
            is NbtShort -> tag.value.toInt()
            else -> default
        }
    }

    private fun getByte(compound: NbtCompound, key: String, default: Byte): Byte {
        val tag = compound[key] ?: return default
        return when (tag) {
            is NbtByte -> tag.signed
            is NbtInt -> tag.value.toByte()
            is NbtShort -> tag.value.toByte()
            else -> default
        }
    }
}
