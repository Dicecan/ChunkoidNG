package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.NbtByte
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtDouble
import br.com.gamemods.nbtmanipulator.NbtFloat
import br.com.gamemods.nbtmanipulator.NbtInt
import br.com.gamemods.nbtmanipulator.NbtList
import br.com.gamemods.nbtmanipulator.NbtLong
import br.com.gamemods.nbtmanipulator.NbtShort
import br.com.gamemods.nbtmanipulator.NbtString

object BedrockActorBuilder {

    private const val ITEM = "minecraft:item"
    private const val XP_ORB = "minecraft:xp_orb"
    private const val TNT = "minecraft:tnt"
    private const val END_CRYSTAL = "minecraft:ender_crystal"

    fun build(javaEntity: NbtCompound, uniqueId: Long): NbtCompound? {
        val javaId = javaEntity.getNullableString("id") ?: return null
        val identifier = EntityIdMapper.toBedrock(javaId) ?: return null

        val actor = NbtCompound()
        actor["identifier"] = NbtString(identifier)
        actor["UniqueID"] = NbtLong(uniqueId)

        val definitions = mutableListOf<String>()
        val row = EntityGroupTable.of(identifier)
        val baby = isBaby(javaEntity)

        if (row != null) {
            definitions.add("+$identifier")
            for (group in row.groups(baby)) {
                definitions.add("+minecraft:$group")
            }
        }

        actor["Pos"] = convertDoubleVectorToFloat(javaEntity.getNullableDoubleList("Pos"))
        actor["Motion"] = convertDoubleVectorToFloat(javaEntity.getNullableDoubleList("Motion"))

        val rotation = javaEntity.getNullableFloatList("Rotation")
        val rotList = NbtList<NbtFloat>()
        val r0 = if (rotation != null && rotation.size > 0) rotation[0].value else 0f
        val r1 = if (rotation != null && rotation.size > 1) rotation[1].value else 0f
        rotList.add(NbtFloat(r0))
        rotList.add(NbtFloat(r1))
        actor["Rotation"] = rotList

        val defaultHp = EntityGroupTable.defaultHealth(identifier)
        val health = getFloat(javaEntity, "Health", defaultHp)
        actor["Health"] = NbtShort(health.toInt().toShort())

        val attrList = NbtList<NbtCompound>()
        val healthAttr = NbtCompound()
        healthAttr["Name"] = NbtString("minecraft:health")
        healthAttr["Base"] = NbtFloat(defaultHp)
        healthAttr["Current"] = NbtFloat(health)
        healthAttr["Max"] = NbtFloat(defaultHp)
        healthAttr["Min"] = NbtFloat(0f)
        attrList.add(healthAttr)
        actor["Attributes"] = attrList

        val customName = (javaEntity["CustomName"] as? NbtString)?.value
        if (!customName.isNullOrEmpty()) {
            val cleanName = cleanJsonText(customName)
            actor["CustomName"] = NbtString(cleanName)
            val nameVisible = getByte(javaEntity, "CustomNameVisible", 0)
            actor["CustomNameVisible"] = NbtByte(nameVisible)
        }

        val invulnerable = getByte(javaEntity, "Invulnerable", 0)
        if (invulnerable != 0.toByte()) {
            actor["Invulnerable"] = NbtByte(1)
        }

        val fire = getShort(javaEntity, "Fire", 0)
        if (fire > 0) {
            actor["Fire"] = NbtShort(fire)
        }

        val air = getShort(javaEntity, "Air", 300)
        actor["Air"] = NbtShort(air)

        val onGround = getByte(javaEntity, "OnGround", 1)
        actor["OnGround"] = NbtByte(onGround)

        val fallDistance = getFloat(javaEntity, "FallDistance", 0f)
        actor["FallDistance"] = NbtFloat(fallDistance)

        when (identifier) {
            ITEM -> {
                val itemCompound = javaEntity.getNullableCompound("Item")
                    ?: javaEntity.getNullableCompound("item")
                val item = EntityEquipment.convertItem(itemCompound)
                if (item != null) {
                    actor["Item"] = item
                }
                actor["Health"] = NbtShort(5)
                val age = getShort(javaEntity, "Age", 0)
                actor["Age"] = NbtShort(age)
            }
            XP_ORB -> {
                val value = getShort(javaEntity, "Value", 1)
                actor["Value"] = NbtShort(value)
                val age = getShort(javaEntity, "Age", 0)
                actor["Age"] = NbtShort(age)
            }
            TNT -> {
                val fuse = getByte(javaEntity, "Fuse", 80)
                actor["Fuse"] = NbtByte(fuse)
            }
            END_CRYSTAL -> {
                val showBottom = getByte(javaEntity, "ShowBottom", 0)
                actor["ShowBottom"] = NbtByte(showBottom)
            }
            else -> {

                EntityVariants.apply(actor, identifier, javaEntity, definitions)
                EntityStates.apply(actor, identifier, javaEntity, definitions)
                EntityEquipment.apply(actor, javaEntity)

                if (identifier == "minecraft:creeper") {
                    val powered = getByte(javaEntity, "powered", 0)
                    if (powered != 0.toByte()) {
                        actor["powered"] = NbtByte(1)
                        definitions.add("+minecraft:charged_creeper")
                    }
                } else if (identifier == "minecraft:slime" || identifier == "minecraft:magma_cube") {
                    val size = getInt(javaEntity, "Size", 1)
                    actor["Size"] = NbtInt(size)
                    val prefix = identifier.removePrefix("minecraft:")
                    when (size) {
                        1 -> definitions.add("+minecraft:${prefix}_small")
                        2 -> definitions.add("+minecraft:${prefix}_medium")
                        else -> definitions.add("+minecraft:${prefix}_large")
                    }
                } else if (identifier == "minecraft:shulker") {
                    val color = getByte(javaEntity, "Color", 16)
                    actor["Color"] = NbtByte(color)
                }
            }
        }

        if (definitions.isNotEmpty()) {
            val defTags = NbtList<NbtString>()
            for (def in definitions) {
                defTags.add(NbtString(def))
            }
            actor["definitions"] = defTags
        }

        return actor
    }

    private fun convertDoubleVectorToFloat(doubleList: NbtList<NbtDouble>?): NbtList<NbtFloat> {
        val list = NbtList<NbtFloat>()
        for (i in 0 until 3) {
            val d = if (doubleList != null && doubleList.size > i) doubleList[i].value else 0.0
            list.add(NbtFloat(d.toFloat()))
        }
        return list
    }

    private fun isBaby(entity: NbtCompound): Boolean {
        val isBabyByte = when (val t = entity["IsBaby"]) {
            is NbtByte -> t.signed != 0.toByte()
            else -> null
        }
        if (isBabyByte != null) return isBabyByte

        val age = when (val t = entity["Age"]) {
            is NbtInt -> t.value
            is NbtShort -> t.value.toInt()
            is NbtByte -> t.signed.toInt()
            else -> null
        }
        if (age != null) return age < 0

        return false
    }

    private fun getInt(entity: NbtCompound, key: String, default: Int): Int {
        return when (val tag = entity[key]) {
            is NbtInt -> tag.value
            is NbtByte -> tag.signed.toInt()
            is NbtShort -> tag.value.toInt()
            else -> default
        }
    }

    private fun getFloat(entity: NbtCompound, key: String, default: Float): Float {
        return when (val tag = entity[key]) {
            is NbtFloat -> tag.value
            is NbtDouble -> tag.value.toFloat()
            is NbtShort -> tag.value.toFloat()
            is NbtInt -> tag.value.toFloat()
            is NbtByte -> tag.signed.toFloat()
            else -> default
        }
    }

    private fun getShort(entity: NbtCompound, key: String, default: Short): Short {
        return when (val tag = entity[key]) {
            is NbtShort -> tag.value
            is NbtInt -> tag.value.toShort()
            is NbtByte -> tag.signed.toShort()
            else -> default
        }
    }

    private fun getByte(entity: NbtCompound, key: String, default: Byte): Byte {
        return when (val tag = entity[key]) {
            is NbtByte -> tag.signed
            is NbtInt -> tag.value.toByte()
            is NbtShort -> tag.value.toByte()
            else -> default
        }
    }

    private fun cleanJsonText(raw: String): String {
        if (!raw.startsWith("{") && !raw.startsWith("[")) return raw
        val match = Regex("\"text\"\\s*:\\s*\"([^\"]*)\"").find(raw)
        return match?.groupValues?.getOrNull(1) ?: raw
    }
}
