package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.*

object JavaEntityBuilder {

    private const val NAMESPACE = "minecraft:"

    private val CAT_VARIANTS_REVERSE = mapOf(
        0 to "white",
        1 to "black",
        2 to "red",
        3 to "siamese",
        4 to "british_shorthair",
        5 to "calico",
        6 to "persian",
        7 to "ragdoll",
        8 to "tabby",
        9 to "all_black",
        10 to "jellie"
    )

    private val FROG_VARIANTS = listOf("temperate", "warm", "cold")

    fun build(bedrockActor: NbtCompound): NbtCompound? {
        val bedrockId = bedrockActor.getNullableString("identifier") ?: return null

        val variant = getInt(bedrockActor, "Variant", 0)
        val javaId = EntityIdMapper.toJavaId(bedrockId, variant) ?: return null

        val javaEntity = NbtCompound()
        javaEntity["id"] = NbtString(javaId)

        val uniqueId = bedrockActor.getNullableLong("UniqueID") ?: 0L
        val uuidArray = EntityLinks.uuidFromUniqueId(uniqueId)
        javaEntity["UUID"] = NbtIntArray(uuidArray)

        val posList = bedrockActor.getNullableFloatList("Pos")
        val javaPos = NbtList<NbtDouble>()
        for (i in 0 until 3) {
            val f = if (posList != null && posList.size > i) posList[i].value else 0f
            javaPos.add(NbtDouble(f.toDouble()))
        }
        javaEntity["Pos"] = javaPos

        val motionList = bedrockActor.getNullableFloatList("Motion")
        val javaMotion = NbtList<NbtDouble>()
        for (i in 0 until 3) {
            val f = if (motionList != null && motionList.size > i) motionList[i].value else 0f
            javaMotion.add(NbtDouble(f.toDouble()))
        }
        javaEntity["Motion"] = javaMotion

        val rotList = bedrockActor.getNullableFloatList("Rotation")
        val javaRot = NbtList<NbtFloat>()
        val r0 = if (rotList != null && rotList.size > 0) rotList[0].value else 0f
        val r1 = if (rotList != null && rotList.size > 1) rotList[1].value else 0f
        javaRot.add(NbtFloat(r0))
        javaRot.add(NbtFloat(r1))
        javaEntity["Rotation"] = javaRot

        val health = extractHealth(bedrockActor)
        javaEntity["Health"] = NbtFloat(health)

        val maxHealth = extractMaxHealth(bedrockActor)
        if (maxHealth > 0f) {
            val attrList = NbtList<NbtCompound>()
            val maxHealthAttr = NbtCompound()
            maxHealthAttr["Name"] = NbtString("minecraft:generic.max_health")
            maxHealthAttr["Base"] = NbtDouble(maxHealth.toDouble())
            attrList.add(maxHealthAttr)
            javaEntity["Attributes"] = attrList
        }

        val air = getShort(bedrockActor, "Air", 300)
        javaEntity["Air"] = NbtShort(air)

        val fallDistance = getFloat(bedrockActor, "FallDistance", 0f)
        javaEntity["FallDistance"] = NbtFloat(fallDistance)

        val onGround = getByte(bedrockActor, "OnGround", 1)
        javaEntity["OnGround"] = NbtByte(onGround)

        val invulnerable = getByte(bedrockActor, "Invulnerable", 0)
        if (invulnerable != 0.toByte()) {
            javaEntity["Invulnerable"] = NbtByte(1)
        }

        val flags = bedrockActor.getNullableLong("Flags") ?: 0L
        unpackFlags(javaEntity, bedrockActor, flags)

        val customName = bedrockActor.getNullableString("CustomName")
        if (!customName.isNullOrEmpty()) {
            javaEntity["CustomName"] = NbtString(toJsonText(customName))
            val nameVisible = getByte(bedrockActor, "CustomNameVisible", 0)
            javaEntity["CustomNameVisible"] = NbtByte(nameVisible)
        }

        val definitions = bedrockActor.getNullableStringList("definitions")?.map { it.value } ?: emptyList()

        val isBaby = definitions.any { it.contains("baby") } || getInt(bedrockActor, "Age", 0) < 0
        if (isBaby) {
            javaEntity["IsBaby"] = NbtByte(1)
            javaEntity["Age"] = NbtInt(-24000)
        } else {
            javaEntity["Age"] = NbtInt(0)
        }

        when (bedrockId) {
            "minecraft:sheep" -> sheep(javaEntity, bedrockActor)
            "minecraft:horse" -> horse(javaEntity, bedrockActor)
            "minecraft:cat" -> cat(javaEntity, bedrockActor)
            "minecraft:wolf" -> wolf(javaEntity, bedrockActor, definitions)
            "minecraft:fox" -> fox(javaEntity, bedrockActor)
            "minecraft:rabbit" -> rabbit(javaEntity, bedrockActor)
            "minecraft:parrot" -> parrot(javaEntity, bedrockActor)
            "minecraft:llama", "minecraft:trader_llama" -> llama(javaEntity, bedrockActor)
            "minecraft:frog" -> frog(javaEntity, definitions)
            "minecraft:villager", "minecraft:villager_v2",
            "minecraft:zombie_villager", "minecraft:zombie_villager_v2" -> villager(javaEntity, bedrockActor, definitions)
            "minecraft:item" -> item(javaEntity, bedrockActor)
            "minecraft:xp_orb" -> xpOrb(javaEntity, bedrockActor)
            "minecraft:tnt" -> tnt(javaEntity, bedrockActor)
            "minecraft:ender_crystal" -> endCrystal(javaEntity, bedrockActor)
            "minecraft:creeper" -> {
                val powered = getByte(bedrockActor, "powered", 0) != 0.toByte() || definitions.any { it.contains("charged") }
                if (powered) javaEntity["powered"] = NbtByte(1)
            }
            "minecraft:slime", "minecraft:magma_cube" -> {
                val size = getInt(bedrockActor, "Size", 1)
                javaEntity["Size"] = NbtInt(size)
            }
            "minecraft:shulker" -> {
                val color = getByte(bedrockActor, "Color", 16)
                javaEntity["Color"] = NbtByte(color)
            }
        }

        restoreEquipment(javaEntity, bedrockActor)

        restoreSaddleAndChest(javaEntity, bedrockId, bedrockActor, definitions, flags)

        return javaEntity
    }

    private fun unpackFlags(javaEntity: NbtCompound, bedrockActor: NbtCompound, flags: Long) {

        val onFire = (flags and (1L shl 0)) != 0L
        val fire = getShort(bedrockActor, "Fire", 0)
        if (onFire || fire > 0) {
            javaEntity["Fire"] = NbtShort(if (fire > 0) fire else 200)
        }

        val isSitting = (flags and (1L shl 16)) != 0L ||
                (flags and (1L shl 23)) != 0L ||
                getByte(bedrockActor, "Sitting", 0) != 0.toByte()
        if (isSitting) {
            javaEntity["Sitting"] = NbtByte(1)
        }

        val isAngry = (flags and (1L shl 17)) != 0L
        if (isAngry) {
            javaEntity["AngerTime"] = NbtInt(400)
        }

        val isTamed = (flags and (1L shl 28)) != 0L || getByte(bedrockActor, "Tamed", 0) != 0.toByte()
        if (isTamed) {
            javaEntity["Tamed"] = NbtByte(1)
        }
    }

    private fun sheep(javaEntity: NbtCompound, bedrockActor: NbtCompound) {
        val color = getByte(bedrockActor, "Color", 0).coerceIn(0, 15)
        javaEntity["Color"] = NbtByte(color)
        val sheared = getByte(bedrockActor, "Sheared", 0)
        if (sheared != 0.toByte()) {
            javaEntity["Sheared"] = NbtByte(1)
        }
    }

    private fun horse(javaEntity: NbtCompound, bedrockActor: NbtCompound) {
        val coat = getInt(bedrockActor, "Variant", 0).coerceIn(0, 6)
        val markings = getInt(bedrockActor, "MarkVariant", 0).coerceIn(0, 4)
        val javaVariant = (markings shl 8) or coat
        javaEntity["Variant"] = NbtInt(javaVariant)
    }

    private fun cat(javaEntity: NbtCompound, bedrockActor: NbtCompound) {
        val variantIndex = getInt(bedrockActor, "Variant", 0)
        val breed = CAT_VARIANTS_REVERSE[variantIndex] ?: "black"
        javaEntity["variant"] = NbtString(NAMESPACE + breed)

        val collar = getByte(bedrockActor, "CollarColor", -1)
        val color = if (collar >= 0) collar else getByte(bedrockActor, "Color", 14)
        javaEntity["CollarColor"] = NbtByte(color.coerceIn(0, 15))
    }

    private val WOLF_VARIANTS = setOf(
        "pale", "ashen", "black", "chestnut", "rusty",
        "spotted", "striped", "woods", "snowy"
    )

    private fun wolf(javaEntity: NbtCompound, bedrockActor: NbtCompound, definitions: List<String>) {
        var breed = "pale"
        for (def in definitions) {
            val candidate = def.substringAfter("wolf_")
            if (candidate in WOLF_VARIANTS) {
                breed = candidate
                break
            }
        }
        javaEntity["variant"] = NbtString(NAMESPACE + breed)

        val collar = getByte(bedrockActor, "CollarColor", -1)
        val color = if (collar >= 0) collar else getByte(bedrockActor, "Color", 14)
        javaEntity["CollarColor"] = NbtByte(color.coerceIn(0, 15))
    }

    private fun fox(javaEntity: NbtCompound, bedrockActor: NbtCompound) {
        val variant = getInt(bedrockActor, "Variant", 0)
        javaEntity["Type"] = NbtString(if (variant == 1) "snow" else "red")
    }

    private fun rabbit(javaEntity: NbtCompound, bedrockActor: NbtCompound) {
        val variant = getInt(bedrockActor, "Variant", 0)
        javaEntity["RabbitType"] = NbtInt(variant)
    }

    private fun parrot(javaEntity: NbtCompound, bedrockActor: NbtCompound) {
        val variant = getInt(bedrockActor, "Variant", 0)
        javaEntity["Variant"] = NbtInt(variant)
    }

    private fun llama(javaEntity: NbtCompound, bedrockActor: NbtCompound) {
        val variant = getInt(bedrockActor, "Variant", 0)
        javaEntity["Variant"] = NbtInt(variant)
    }

    private fun frog(javaEntity: NbtCompound, definitions: List<String>) {
        var variant = "temperate"
        for (def in definitions) {
            for (f in FROG_VARIANTS) {
                if (def.contains(f)) {
                    variant = f
                    break
                }
            }
        }
        javaEntity["variant"] = NbtString(NAMESPACE + variant)
    }

    private fun villager(javaEntity: NbtCompound, bedrockActor: NbtCompound, definitions: List<String>) {
        var profession = "none"
        var type = "plains"

        for (def in definitions) {
            val clean = def.removePrefix("+").removePrefix(NAMESPACE)
            if (clean.endsWith("_villager")) {
                type = clean.removeSuffix("_villager")
            } else if (clean in listOf(
                    "armorer", "butcher", "cartographer", "cleric", "farmer",
                    "fisherman", "fletcher", "leatherworker", "librarian",
                    "mason", "nitwit", "shepherd", "toolsmith", "weaponsmith"
                )) {
                profession = clean
            }
        }

        val tier = getInt(bedrockActor, "TradeTier", 0)
        val level = (tier + 1).coerceIn(1, 5)

        val vData = NbtCompound()
        vData["profession"] = NbtString(NAMESPACE + profession)
        vData["type"] = NbtString(NAMESPACE + type)
        vData["level"] = NbtInt(level)
        javaEntity["VillagerData"] = vData

        val xp = getInt(bedrockActor, "TradeExperience", 0)
        if (xp > 0) {
            javaEntity["Xp"] = NbtInt(xp)
        }

        val bedrockOffers = bedrockActor.getNullableCompound("Offers")
        val javaOffers = EntityStates.mapOffersToJava(bedrockOffers)
        if (javaOffers != null) {
            javaEntity["Offers"] = javaOffers
        }
    }

    private fun restoreEquipment(javaEntity: NbtCompound, bedrockActor: NbtCompound) {

        val handItems = NbtList<NbtCompound>()
        val mainhand = bedrockActor.getNullableCompoundList("Mainhand")?.firstOrNull()
        val offhand = bedrockActor.getNullableCompoundList("Offhand")?.firstOrNull()

        handItems.add(convertBedrockItemToJava(mainhand))
        handItems.add(convertBedrockItemToJava(offhand))
        javaEntity["HandItems"] = handItems

        val armorList = bedrockActor.getNullableCompoundList("Armor")
        val armorItems = NbtList<NbtCompound>()

        val head = armorList?.getOrNull(0)
        val chest = armorList?.getOrNull(1)
        val legs = armorList?.getOrNull(2)
        val feet = armorList?.getOrNull(3)

        armorItems.add(convertBedrockItemToJava(feet))
        armorItems.add(convertBedrockItemToJava(legs))
        armorItems.add(convertBedrockItemToJava(chest))
        armorItems.add(convertBedrockItemToJava(head))
        javaEntity["ArmorItems"] = armorItems
    }

    private fun convertBedrockItemToJava(bItem: NbtCompound?): NbtCompound {
        val result = NbtCompound()
        if (bItem == null) return result

        val name = bItem.getNullableString("Name")
        if (name.isNullOrEmpty() || name == "minecraft:air") return result

        result["id"] = NbtString(name)
        val count = getByte(bItem, "Count", 1).coerceAtLeast(1)
        result["Count"] = NbtByte(count)

        val damage = getShort(bItem, "Damage", 0)
        if (damage > 0) {
            val tag = NbtCompound()
            tag["Damage"] = NbtInt(damage.toInt())
            result["tag"] = tag
        }

        return result
    }

    private fun restoreSaddleAndChest(
        javaEntity: NbtCompound,
        bedrockId: String,
        bedrockActor: NbtCompound,
        definitions: List<String>,
        flags: Long
    ) {

        val isSaddled = (flags and (1L shl 48)) != 0L ||
                getByte(bedrockActor, "Saddled", 0) != 0.toByte() ||
                definitions.any { it.contains("saddled") }

        if (isSaddled) {
            val saddleTag = NbtCompound()
            saddleTag["id"] = NbtString("minecraft:saddle")
            saddleTag["Count"] = NbtByte(1)
            javaEntity["SaddleItem"] = saddleTag

            val eq = NbtCompound()
            val body = NbtCompound()
            body["id"] = NbtString("minecraft:saddle")
            body["count"] = NbtInt(1)
            eq["body"] = body
            javaEntity["equipment"] = eq
        }

        val isChested = getByte(bedrockActor, "Chested", 0) != 0.toByte() ||
                getByte(bedrockActor, "ChestedHorse", 0) != 0.toByte()
        if (isChested) {
            javaEntity["ChestedHorse"] = NbtByte(1)
        }
    }

    private fun item(javaEntity: NbtCompound, bedrockActor: NbtCompound) {
        val bItem = bedrockActor.getNullableCompound("Item")
        val jItem = convertBedrockItemToJava(bItem)
        if (jItem.isNotEmpty()) {
            javaEntity["Item"] = jItem
        }
        val age = getShort(bedrockActor, "Age", 0)
        javaEntity["Age"] = NbtShort(age)
        javaEntity["Health"] = NbtShort(5)
    }

    private fun xpOrb(javaEntity: NbtCompound, bedrockActor: NbtCompound) {
        val value = getShort(bedrockActor, "Value", 1)
        javaEntity["Value"] = NbtShort(value)
        val age = getShort(bedrockActor, "Age", 0)
        javaEntity["Age"] = NbtShort(age)
    }

    private fun tnt(javaEntity: NbtCompound, bedrockActor: NbtCompound) {
        val fuse = getByte(bedrockActor, "Fuse", 80)
        javaEntity["Fuse"] = NbtShort(fuse.toShort())
    }

    private fun endCrystal(javaEntity: NbtCompound, bedrockActor: NbtCompound) {
        val showBottom = getByte(bedrockActor, "ShowBottom", 0)
        javaEntity["ShowBottom"] = NbtByte(showBottom)
    }

    private fun extractHealth(bedrockActor: NbtCompound): Float {
        val direct = getFloat(bedrockActor, "Health", -1f)
        if (direct >= 0f) return direct

        val attrs = bedrockActor.getNullableCompoundList("Attributes")
        if (attrs != null) {
            for (attr in attrs) {
                if (attr.getNullableString("Name") == "minecraft:health") {
                    val current = getFloat(attr, "Current", -1f)
                    if (current >= 0f) return current
                }
            }
        }
        return 20f
    }

    private fun extractMaxHealth(bedrockActor: NbtCompound): Float {
        val attrs = bedrockActor.getNullableCompoundList("Attributes")
        if (attrs != null) {
            for (attr in attrs) {
                if (attr.getNullableString("Name") == "minecraft:health") {
                    val max = getFloat(attr, "Max", -1f)
                    if (max > 0f) return max
                }
            }
        }
        return -1f
    }

    private fun toJsonText(raw: String): String {
        if (raw.startsWith("{") && raw.endsWith("}")) return raw
        val escaped = raw.replace("\\", "\\\\").replace("\"", "\\\"")
        return "{\"text\":\"$escaped\"}"
    }

    private fun getInt(compound: NbtCompound, key: String, default: Int): Int {
        return when (val tag = compound[key]) {
            is NbtInt -> tag.value
            is NbtByte -> tag.signed.toInt()
            is NbtShort -> tag.value.toInt()
            is NbtLong -> tag.value.toInt()
            else -> default
        }
    }

    private fun getShort(compound: NbtCompound, key: String, default: Short): Short {
        return when (val tag = compound[key]) {
            is NbtShort -> tag.value
            is NbtInt -> tag.value.toShort()
            is NbtByte -> tag.signed.toShort()
            else -> default
        }
    }

    private fun getByte(compound: NbtCompound, key: String, default: Byte): Byte {
        return when (val tag = compound[key]) {
            is NbtByte -> tag.signed
            is NbtInt -> tag.value.toByte()
            is NbtShort -> tag.value.toByte()
            else -> default
        }
    }

    private fun getFloat(compound: NbtCompound, key: String, default: Float): Float {
        return when (val tag = compound[key]) {
            is NbtFloat -> tag.value
            is NbtDouble -> tag.value.toFloat()
            is NbtShort -> tag.value.toFloat()
            is NbtInt -> tag.value.toFloat()
            is NbtByte -> tag.signed.toFloat()
            else -> default
        }
    }
}
