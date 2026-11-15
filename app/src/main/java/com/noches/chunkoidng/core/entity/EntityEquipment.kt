package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.NbtByte
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtInt
import br.com.gamemods.nbtmanipulator.NbtList
import br.com.gamemods.nbtmanipulator.NbtShort
import br.com.gamemods.nbtmanipulator.NbtString
import br.com.gamemods.nbtmanipulator.NbtTag

object EntityEquipment {

    private val JAVA_ARMOUR_SLOTS = listOf("head", "chest", "legs", "feet", "body")
    private const val BODY_SLOT = 4
    private const val SADDLE = "minecraft:saddle"

    private val JAVA_TO_BEDROCK_ENCHANT = mapOf(
        "minecraft:protection" to 0,
        "minecraft:fire_protection" to 1,
        "minecraft:feather_falling" to 2,
        "minecraft:blast_protection" to 3,
        "minecraft:projectile_protection" to 4,
        "minecraft:thorns" to 5,
        "minecraft:respiration" to 6,
        "minecraft:depth_strider" to 7,
        "minecraft:aqua_affinity" to 8,
        "minecraft:sharpness" to 9,
        "minecraft:smite" to 10,
        "minecraft:bane_of_arthropods" to 11,
        "minecraft:knockback" to 12,
        "minecraft:fire_aspect" to 13,
        "minecraft:looting" to 14,
        "minecraft:efficiency" to 15,
        "minecraft:silk_touch" to 16,
        "minecraft:unbreaking" to 17,
        "minecraft:fortune" to 18,
        "minecraft:power" to 19,
        "minecraft:punch" to 20,
        "minecraft:flame" to 21,
        "minecraft:infinity" to 22,
        "minecraft:luck_of_the_sea" to 23,
        "minecraft:lure" to 24,
        "minecraft:frost_walker" to 25,
        "minecraft:mending" to 26,
        "minecraft:binding_curse" to 27,
        "minecraft:vanishing_curse" to 28,
        "minecraft:impaling" to 29,
        "minecraft:riptide" to 30,
        "minecraft:loyalty" to 31,
        "minecraft:channeling" to 32,
        "minecraft:multishot" to 33,
        "minecraft:piercing" to 34,
        "minecraft:quick_charge" to 35,
        "minecraft:soul_speed" to 36,
        "minecraft:swift_sneak" to 37,
        "minecraft:wind_burst" to 38,
        "minecraft:density" to 39,
        "minecraft:breach" to 40,
        "minecraft:lunge" to 41
    )

    private val BEDROCK_TO_JAVA_ENCHANT = JAVA_TO_BEDROCK_ENCHANT.entries.associate { (k, v) -> v to k }

    fun apply(actor: NbtCompound, javaEntity: NbtCompound) {
        val equipment = javaEntity.getNullableCompound("equipment")
        if (equipment != null) {
            writeModern(actor, equipment)
        } else {
            writeLegacy(actor, javaEntity)
        }
    }

    private fun writeModern(actor: NbtCompound, equipment: NbtCompound) {
        val mainhand = convertItem(equipment.getNullableCompound("mainhand"))
        if (mainhand != null) {
            actor["Mainhand"] = NbtList(listOf(mainhand))
        }

        val offhand = convertItem(equipment.getNullableCompound("offhand"))
        if (offhand != null) {
            actor["Offhand"] = NbtList(listOf(offhand))
        }

        val armorSlots = ArrayList<NbtCompound>(5)
        var hasArmor = false
        for (i in 0 until 5) {
            val slotName = JAVA_ARMOUR_SLOTS[i]
            val item = equipment.getNullableCompound(slotName)
            if (i == BODY_SLOT && isSaddle(item)) {
                armorSlots.add(emptyItem())
                continue
            }
            val converted = convertItem(item)
            if (converted != null) {
                armorSlots.add(converted)
                hasArmor = true
            } else {
                armorSlots.add(emptyItem())
            }
        }

        if (hasArmor) {
            actor["Armor"] = NbtList(armorSlots)
        }
    }

    private fun writeLegacy(actor: NbtCompound, entity: NbtCompound) {
        val hands = entity.getNullableCompoundList("HandItems")
        if (hands != null && hands.isNotEmpty()) {
            val main = if (hands.size > 0) convertItem(hands[0]) else null
            if (main != null) actor["Mainhand"] = NbtList(listOf(main))
            val off = if (hands.size > 1) convertItem(hands[1]) else null
            if (off != null) actor["Offhand"] = NbtList(listOf(off))
        }

        val armorItems = entity.getNullableCompoundList("ArmorItems")
        val armorSlots = ArrayList<NbtCompound>(5)
        for (i in 0 until 5) armorSlots.add(emptyItem())

        var hasArmor = false
        if (armorItems != null) {
            for (legacyIdx in 0 until 4) {
                val item = if (armorItems.size > legacyIdx) convertItem(armorItems[legacyIdx]) else null
                if (item != null) {
                    val bedrockIdx = 3 - legacyIdx
                    armorSlots[bedrockIdx] = item
                    hasArmor = true
                }
            }
        }

        val bodyItem = entity.getNullableCompound("ArmorItem")
        if (bodyItem != null && !isSaddle(bodyItem)) {
            val converted = convertItem(bodyItem)
            if (converted != null) {
                armorSlots[BODY_SLOT] = converted
                hasArmor = true
            }
        }

        if (hasArmor) {
            actor["Armor"] = NbtList(armorSlots)
        }
    }

    fun convertItem(javaItem: NbtCompound?): NbtCompound? {
        if (javaItem == null) return null
        val rawId = javaItem.getNullableString("id") ?: return null
        val id = if (rawId.contains(":")) rawId else "minecraft:$rawId"
        if (id == "minecraft:air" || rawId.isEmpty()) return null

        val countInt = javaItem.getNullableInt("Count")
            ?: javaItem.getNullableInt("count")
            ?: 1
        if (countInt <= 0) return null
        val count = countInt.coerceIn(1, 127).toByte()

        val components = javaItem.getNullableCompound("components")
        val legacyTag = javaItem.getNullableCompound("tag")

        val damage = (components?.getNullableInt("minecraft:damage")
            ?: legacyTag?.getNullableInt("Damage")
            ?: javaItem.getNullableInt("Damage")
            ?: 0).coerceAtLeast(0).toShort()

        val customName = extractPlainText(components?.get("minecraft:custom_name"))
            ?: extractPlainText(legacyTag?.getNullableCompound("display")?.get("Name"))

        val enchants = extractJavaEnchantments(legacyTag, components)

        val item = NbtCompound()
        item["Name"] = NbtString(id)
        item["Count"] = NbtByte(count)
        item["Damage"] = NbtShort(damage)

        if (!customName.isNullOrEmpty() || enchants.isNotEmpty() || damage > 0) {
            val beTag = NbtCompound()
            if (damage > 0) {
                beTag["Damage"] = NbtInt(damage.toInt())
            }
            if (!customName.isNullOrEmpty()) {
                val display = NbtCompound()
                display["Name"] = NbtString(customName)
                beTag["display"] = display
            }
            if (enchants.isNotEmpty()) {
                val enchList = enchants.map { (beId, lvl) ->
                    NbtCompound().apply {
                        this["id"] = NbtShort(beId.toShort())
                        this["lvl"] = NbtShort(lvl.toShort())
                    }
                }
                beTag["ench"] = NbtList(enchList)
            }
            if (beTag.isNotEmpty()) {
                item["tag"] = beTag
            }
        }
        return item
    }

    fun convertBedrockItemToJava(
        bedrockItem: NbtCompound?,
        modernComponents: Boolean = true,
        flatComponents: Boolean = true
    ): NbtCompound? {
        if (bedrockItem == null || bedrockItem.isEmpty()) return null
        val rawId = bedrockItem.getNullableString("Name")
            ?: bedrockItem.getNullableCompound("Block")?.getNullableString("name")
            ?: return null
        if (rawId.isEmpty() || rawId == "minecraft:air") return null
        val id = if (rawId.contains(":")) rawId else "minecraft:$rawId"

        val count = (bedrockItem.getNullableInt("Count") ?: 1).coerceIn(0, 127)
        if (count <= 0) return null

        val beTag = bedrockItem.getNullableCompound("tag")
        val damage = (bedrockItem.getNullableInt("Damage")
            ?: beTag?.getNullableInt("Damage")
            ?: 0).coerceAtLeast(0)

        val customName = beTag?.getNullableCompound("display")?.getNullableString("Name")
        val enchants = extractBedrockEnchantments(beTag)

        val javaItem = NbtCompound()
        javaItem["id"] = NbtString(id)
        writeJavaItemCountAndMetadata(
            javaItem = javaItem,
            id = id,
            count = count,
            damage = damage,
            customName = customName,
            enchants = enchants,
            modernComponents = modernComponents,
            flatComponents = flatComponents
        )
        return javaItem
    }

    fun adaptJavaItem(
        item: NbtCompound,
        modernComponents: Boolean,
        flatComponents: Boolean
    ) {
        val rawId = item.getNullableString("id") ?: return
        if (rawId.isEmpty() || rawId == "minecraft:air") return
        val id = if (rawId.contains(":")) rawId else "minecraft:$rawId"

        val legacyTag = item.getNullableCompound("tag")
        val existingComponents = item.getNullableCompound("components")

        if (modernComponents) {
            if (!item.contains("count")) {
                val oldCount = item.getNullableInt("Count") ?: 1
                item["count"] = NbtInt(oldCount.coerceAtLeast(1))
            }
            item.remove("Count")

            if (legacyTag != null && existingComponents == null) {
                val damage = (legacyTag.getNullableInt("Damage") ?: 0).coerceAtLeast(0)
                val customName = extractPlainText(legacyTag.getNullableCompound("display")?.get("Name"))
                val enchants = extractJavaEnchantmentPairs(legacyTag, null)
                val repairCost = legacyTag.getNullableInt("RepairCost") ?: 0
                val unbreakable = (legacyTag.getNullableByte("Unbreakable") ?: 0) == 1.toByte()

                val components = NbtCompound()
                applyModernComponents(components, id, damage, customName, enchants, flatComponents)
                if (repairCost > 0) components["minecraft:repair_cost"] = NbtInt(repairCost)
                if (unbreakable) components["minecraft:unbreakable"] = NbtCompound()
                if (components.isNotEmpty()) {
                    item["components"] = components
                }
                item.remove("tag")
            } else if (existingComponents != null) {
                normalizeExistingComponents(existingComponents, flatComponents)
            }
        } else {
            if (!item.contains("Count")) {
                val newCount = item.getNullableInt("count") ?: 1
                item["Count"] = NbtByte(newCount.coerceIn(1, 127).toByte())
            }
            item.remove("count")

            if (existingComponents != null && legacyTag == null) {
                val damage = (existingComponents.getNullableInt("minecraft:damage") ?: 0).coerceAtLeast(0)
                val customName = extractPlainText(existingComponents["minecraft:custom_name"])
                val enchants = extractJavaEnchantmentPairs(null, existingComponents)
                val repairCost = existingComponents.getNullableInt("minecraft:repair_cost") ?: 0
                val unbreakable = existingComponents.contains("minecraft:unbreakable")

                val tag = NbtCompound()
                applyLegacyJavaTag(tag, id, damage, customName, enchants)
                if (repairCost > 0) tag["RepairCost"] = NbtInt(repairCost)
                if (unbreakable) tag["Unbreakable"] = NbtByte(1)
                if (tag.isNotEmpty()) {
                    item["tag"] = tag
                }
                item.remove("components")
            }
        }
    }

    private fun writeJavaItemCountAndMetadata(
        javaItem: NbtCompound,
        id: String,
        count: Int,
        damage: Int,
        customName: String?,
        enchants: List<Pair<String, Int>>,
        modernComponents: Boolean,
        flatComponents: Boolean
    ) {
        if (modernComponents) {
            javaItem["count"] = NbtInt(count)
            val components = NbtCompound()
            applyModernComponents(components, id, damage, customName, enchants, flatComponents)
            if (components.isNotEmpty()) {
                javaItem["components"] = components
            }
        } else {
            javaItem["Count"] = NbtByte(count.coerceIn(1, 127).toByte())
            val tag = NbtCompound()
            applyLegacyJavaTag(tag, id, damage, customName, enchants)
            if (tag.isNotEmpty()) {
                javaItem["tag"] = tag
            }
        }
    }

    private fun applyModernComponents(
        components: NbtCompound,
        id: String,
        damage: Int,
        customName: String?,
        enchants: List<Pair<String, Int>>,
        flatComponents: Boolean
    ) {
        if (damage > 0) {
            components["minecraft:damage"] = NbtInt(damage)
        }
        if (!customName.isNullOrEmpty()) {
            components["minecraft:custom_name"] = if (flatComponents) {
                NbtCompound().apply { this["text"] = NbtString(customName) }
            } else {
                NbtString("{\"text\":\"${escapeJson(customName)}\"}")
            }
        }
        if (enchants.isNotEmpty()) {
            val key = if (id == "minecraft:enchanted_book") {
                "minecraft:stored_enchantments"
            } else {
                "minecraft:enchantments"
            }
            val levelMap = NbtCompound()
            for ((jeEnchId, lvl) in enchants) {
                levelMap[jeEnchId] = NbtInt(lvl)
            }
            components[key] = if (flatComponents) {
                levelMap
            } else {
                NbtCompound().apply { this["levels"] = levelMap }
            }
        }
    }

    private fun applyLegacyJavaTag(
        tag: NbtCompound,
        id: String,
        damage: Int,
        customName: String?,
        enchants: List<Pair<String, Int>>
    ) {
        if (damage > 0) {
            tag["Damage"] = NbtInt(damage)
        }
        if (!customName.isNullOrEmpty()) {
            tag["display"] = NbtCompound().apply {
                this["Name"] = NbtString("{\"text\":\"${escapeJson(customName)}\"}")
            }
        }
        if (enchants.isNotEmpty()) {
            val listKey = if (id == "minecraft:enchanted_book") {
                "StoredEnchantments"
            } else {
                "Enchantments"
            }
            tag[listKey] = NbtList(
                enchants.map { (jeEnchId, lvl) ->
                    NbtCompound().apply {
                        this["id"] = NbtString(jeEnchId)
                        this["lvl"] = NbtShort(lvl.toShort())
                    }
                }
            )
        }
    }

    private fun normalizeExistingComponents(components: NbtCompound, flatComponents: Boolean) {
        val nameTag = components["minecraft:custom_name"]
        val plainName = extractPlainText(nameTag)
        if (!plainName.isNullOrEmpty()) {
            if (flatComponents && nameTag !is NbtCompound) {
                components["minecraft:custom_name"] = NbtCompound().apply { this["text"] = NbtString(plainName) }
            } else if (!flatComponents && nameTag is NbtCompound) {
                components["minecraft:custom_name"] = NbtString("{\"text\":\"${escapeJson(plainName)}\"}")
            }
        }
        for (key in listOf("minecraft:enchantments", "minecraft:stored_enchantments")) {
            val enchComp = components.getNullableCompound(key) ?: continue
            val levels = enchComp.getNullableCompound("levels")
            if (flatComponents && levels != null) {
                components[key] = levels
            } else if (!flatComponents && levels == null && enchComp.isNotEmpty()) {
                components[key] = NbtCompound().apply { this["levels"] = enchComp }
            }
        }
    }

    private fun extractJavaEnchantmentPairs(
        legacyTag: NbtCompound?,
        components: NbtCompound?
    ): List<Pair<String, Int>> {
        val out = mutableListOf<Pair<String, Int>>()
        val compEnch = components?.getNullableCompound("minecraft:enchantments")
            ?: components?.getNullableCompound("minecraft:stored_enchantments")
        if (compEnch != null) {
            val levels = compEnch.getNullableCompound("levels") ?: compEnch
            for ((rawKey, _) in levels) {
                if (rawKey == "show_in_tooltip") continue
                val lvl = levels.getNullableInt(rawKey) ?: continue
                val normKey = if (rawKey.contains(":")) rawKey else "minecraft:$rawKey"
                out.add(normKey to lvl)
            }
            if (out.isNotEmpty()) return out
        }

        val legacyList = legacyTag?.getNullableCompoundList("Enchantments")
            ?: legacyTag?.getNullableCompoundList("StoredEnchantments")
        if (legacyList != null) {
            for (entry in legacyList) {
                val rawKey = entry.getNullableString("id") ?: continue
                val lvl = entry.getNullableInt("lvl") ?: 1
                val normKey = if (rawKey.contains(":")) rawKey else "minecraft:$rawKey"
                out.add(normKey to lvl)
            }
        }
        return out
    }

    private fun extractJavaEnchantments(
        legacyTag: NbtCompound?,
        components: NbtCompound?
    ): List<Pair<Int, Int>> {
        return extractJavaEnchantmentPairs(legacyTag, components).mapNotNull { (normKey, lvl) ->
            val beId = JAVA_TO_BEDROCK_ENCHANT[normKey] ?: return@mapNotNull null
            beId to lvl
        }
    }

    private fun extractBedrockEnchantments(beTag: NbtCompound?): List<Pair<String, Int>> {
        val enchList = beTag?.getNullableCompoundList("ench") ?: return emptyList()
        val out = mutableListOf<Pair<String, Int>>()
        for (entry in enchList) {
            val id = entry.getNullableInt("id") ?: continue
            val lvl = entry.getNullableInt("lvl") ?: 1
            val jeId = BEDROCK_TO_JAVA_ENCHANT[id] ?: continue
            out.add(jeId to lvl)
        }
        return out
    }

    private fun extractPlainText(tag: NbtTag?): String? {
        return when (tag) {
            is NbtString -> parseJsonTextString(tag.value)
            is NbtCompound -> {
                val base = tag.getNullableString("text") ?: ""
                val extra = tag.getNullableList("extra")?.joinToString("") {
                    extractPlainText(it) ?: ""
                } ?: ""
                (base + extra).ifEmpty { null }
            }
            else -> null
        }
    }

    private fun parseJsonTextString(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val m = Regex(""""text"\s*:\s*"((?:\\.|[^"\\])*)"""").find(trimmed)
            if (m != null) {
                return unescapeJson(m.groupValues[1]).ifEmpty { null }
            }
        }
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
            return unescapeJson(trimmed.substring(1, trimmed.length - 1)).ifEmpty { null }
        }
        return trimmed
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun unescapeJson(s: String): String {
        return s.replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }

    private fun isSaddle(item: NbtCompound?): Boolean {
        if (item == null) return false
        val id = item.getNullableString("id") ?: return false
        return id == SADDLE || id.endsWith("saddle")
    }

    fun emptyItem(): NbtCompound {
        val item = NbtCompound()
        item["Name"] = NbtString("")
        item["Count"] = NbtByte(0)
        item["Damage"] = NbtShort(0)
        return item
    }
}
