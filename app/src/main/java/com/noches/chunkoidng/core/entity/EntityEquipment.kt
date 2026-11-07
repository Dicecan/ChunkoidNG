package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.NbtByte
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtList
import br.com.gamemods.nbtmanipulator.NbtShort
import br.com.gamemods.nbtmanipulator.NbtString

object EntityEquipment {

    private val JAVA_ARMOUR_SLOTS = listOf("head", "chest", "legs", "feet", "body")
    private const val BODY_SLOT = 4
    private const val SADDLE = "minecraft:saddle"

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
        val id = javaItem.getNullableString("id") ?: return null
        if (id == "minecraft:air" || id.isEmpty()) return null

        val count = javaItem.getNullableByte("Count")
            ?: javaItem.getNullableInt("count")?.toByte()
            ?: 1

        val damage = javaItem.getNullableShort("Damage")
            ?: javaItem.getNullableInt("Damage")?.toShort()
            ?: 0

        val item = NbtCompound()
        item["Name"] = NbtString(id)
        item["Count"] = NbtByte(count)
        item["Damage"] = NbtShort(damage)
        return item
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
