package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.*

object JavaEntityBuilder {

    private const val NAMESPACE = "minecraft:"
    private const val MINECART_LIFT = 0.35

    data class JavaTargetOptions(
        val modernPotionIds: Boolean = true,
        val modernComponents: Boolean = true,
        val flatComponents: Boolean = true,
        val modernEquipment: Boolean = true,
        val shortFuse: Boolean = true
    ) {
        companion object {
            fun fromProfile(profile: JavaTargetProfile): JavaTargetOptions {
                return JavaTargetOptions(
                    modernPotionIds = profile.usesModernPotionIds,
                    modernComponents = profile.usesModernComponents,
                    flatComponents = profile.usesFlatComponents,
                    modernEquipment = profile.usesModernEquipment,
                    shortFuse = profile.usesShortFuse
                )
            }
        }
    }

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

    fun build(bedrockActor: NbtCompound, options: JavaTargetOptions = JavaTargetOptions()): NbtCompound? {
        val bedrockId = bedrockActor.getNullableString("identifier") ?: return null

        val variant = getInt(bedrockActor, "Variant", 0)
        val javaId = EntityIdMapper.toJavaId(bedrockId, variant, options.modernPotionIds) ?: return null

        val javaEntity = NbtCompound()
        javaEntity["id"] = NbtString(javaId)

        val uniqueId = bedrockActor.getNullableLong("UniqueID") ?: 0L
        val uuidArray = EntityLinks.uuidFromUniqueId(uniqueId)
        javaEntity["UUID"] = NbtIntArray(uuidArray)

        val isMinecart = bedrockId == "minecraft:minecart" || bedrockId.endsWith("_minecart")
        val posList = bedrockActor.getNullableFloatList("Pos")
        val javaPos = NbtList<NbtDouble>()
        for (i in 0 until 3) {
            val f = if (posList != null && posList.size > i && posList[i].value.isFinite()) posList[i].value else 0f
            val yAdjust = if (isMinecart && i == 1) -MINECART_LIFT else 0.0
            javaPos.add(NbtDouble(f.toDouble() + yAdjust))
        }
        javaEntity["Pos"] = javaPos

        val motionList = bedrockActor.getNullableFloatList("Motion")
        val javaMotion = NbtList<NbtDouble>()
        for (i in 0 until 3) {
            val f = if (motionList != null && motionList.size > i && motionList[i].value.isFinite()) motionList[i].value else 0f
            javaMotion.add(NbtDouble(f.toDouble()))
        }
        javaEntity["Motion"] = javaMotion

        val rotList = bedrockActor.getNullableFloatList("Rotation")
        val javaRot = NbtList<NbtFloat>()
        val r0 = if (rotList != null && rotList.size > 0 && rotList[0].value.isFinite()) rotList[0].value else 0f
        val r1 = if (rotList != null && rotList.size > 1 && rotList[1].value.isFinite()) rotList[1].value else 0f
        javaRot.add(NbtFloat(r0))
        javaRot.add(NbtFloat(r1))
        javaEntity["Rotation"] = javaRot

        val tableMaxHp = EntityGroupTable.healthOrNull(bedrockId)
        if (tableMaxHp != null) {
            val health = extractHealth(bedrockActor, tableMaxHp)
            javaEntity["Health"] = NbtFloat(health)

            val attrMax = extractMaxHealth(bedrockActor)
            val maxHealth = if (attrMax > 0f) attrMax else tableMaxHp
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

        if (getByte(bedrockActor, "Persistent", 0) != 0.toByte()) {
            javaEntity["PersistenceRequired"] = NbtByte(1)
        }

        val flags = bedrockActor.getNullableLong("Flags") ?: 0L
        unpackFlags(javaEntity, bedrockActor, flags)

        val customName = bedrockActor.getNullableString("CustomName")
        if (!customName.isNullOrEmpty()) {
            javaEntity["CustomName"] = if (options.flatComponents) {
                NbtCompound().apply { this["text"] = NbtString(customName) }
            } else {
                NbtString(toJsonText(customName))
            }
            val nameVisible = getByte(bedrockActor, "CustomNameVisible", 0)
            javaEntity["CustomNameVisible"] = NbtByte(nameVisible)
        }

        val tags = bedrockActor.getNullableStringList("Tags")
        if (!tags.isNullOrEmpty()) {
            val javaTags = NbtList<NbtString>()
            for (tag in tags) {
                javaTags.add(NbtString(tag.value))
            }
            javaEntity["Tags"] = javaTags
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
            "minecraft:mooshroom" -> mooshroom(javaEntity, bedrockActor, definitions)
            "minecraft:horse" -> horse(javaEntity, bedrockActor)
            "minecraft:cat" -> cat(javaEntity, bedrockActor)
            "minecraft:wolf" -> wolf(javaEntity, bedrockActor, definitions)
            "minecraft:fox" -> fox(javaEntity, bedrockActor)
            "minecraft:rabbit" -> rabbit(javaEntity, bedrockActor)
            "minecraft:parrot" -> parrot(javaEntity, bedrockActor)
            "minecraft:llama", "minecraft:trader_llama" -> llama(javaEntity, bedrockActor)
            "minecraft:frog" -> frog(javaEntity, definitions)
            "minecraft:villager", "minecraft:villager_v2",
            "minecraft:zombie_villager", "minecraft:zombie_villager_v2" -> villager(javaEntity, bedrockActor, definitions, options)
            "minecraft:item" -> item(javaEntity, bedrockActor, options)
            "minecraft:xp_orb" -> xpOrb(javaEntity, bedrockActor)
            "minecraft:falling_block" -> fallingBlock(javaEntity, bedrockActor)
            "minecraft:tnt" -> tnt(javaEntity, bedrockActor, options)
            "minecraft:ender_crystal" -> endCrystal(javaEntity, bedrockActor)
            "minecraft:splash_potion", "minecraft:lingering_potion" -> potion(javaEntity, bedrockId, bedrockActor, options)
            "minecraft:command_block_minecart" -> {
                val cmd = bedrockActor.getNullableString("Command")
                if (!cmd.isNullOrEmpty()) {
                    javaEntity["Command"] = NbtString(cmd)
                }
            }
            "minecraft:evocation_fang" -> {
                val warmup = bedrockActor.getNullableInt("Warmup")
                if (warmup != null) {
                    javaEntity["Warmup"] = NbtInt(warmup)
                }
            }
            "minecraft:creeper" -> {
                val powered = getByte(bedrockActor, "powered", 0) != 0.toByte() || definitions.any { it.contains("charged") }
                if (powered) javaEntity["powered"] = NbtByte(1)
            }
            "minecraft:slime", "minecraft:magma_cube" -> {
                val size = getInt(bedrockActor, "Size", 1)
                javaEntity["Size"] = NbtInt(size)
            }
            "minecraft:shulker" -> {
                val jeColor = if (bedrockActor.contains("Color")) {
                    getByte(bedrockActor, "Color", 16).toInt() and 0xFF
                } else if (bedrockActor.contains("Variant")) {
                    val v = getInt(bedrockActor, "Variant", 16)
                    if (v in 0..15) 15 - v else 16
                } else {
                    16
                }
                javaEntity["Color"] = NbtByte(jeColor.toByte())
            }
            "minecraft:goat" -> {
                val variant = getInt(bedrockActor, "Variant", 0)
                val isScreamer = variant == 1 || definitions.any { it.contains("screamer") }
                javaEntity["IsScreamingGoat"] = NbtByte(if (isScreamer) 1 else 0)
            }
            "minecraft:leash_knot" -> {
                val px = if (posList != null && posList.size > 0) posList[0].value else 0f
                val py = if (posList != null && posList.size > 1) posList[1].value else 0f
                val pz = if (posList != null && posList.size > 2) posList[2].value else 0f
                val bx = Math.floor(px.toDouble()).toInt()
                val by = Math.floor(py.toDouble()).toInt()
                val bz = Math.floor(pz.toDouble()).toInt()
                javaEntity["TileX"] = NbtInt(bx)
                javaEntity["TileY"] = NbtInt(by)
                javaEntity["TileZ"] = NbtInt(bz)
            }
        }

        if (EntityGroupTable.hasHealth(bedrockId) || bedrockId == "minecraft:armor_stand") {
            restoreEquipment(javaEntity, bedrockActor, options)
        }

        restoreSaddleAndChest(javaEntity, bedrockId, bedrockActor, definitions, flags, options)
        EntityContainers.applyToJava(
            javaEntity = javaEntity,
            actor = bedrockActor,
            bedrockId = bedrockId,
            modernComponents = options.modernComponents,
            flatComponents = options.flatComponents
        )

        val leasherId = bedrockActor.getNullableLong("LeasherID") ?: 0L
        if (leasherId != 0L && leasherId != -1L) {
            val holderUuid = EntityLinks.uuidFromUniqueId(leasherId)
            val leashComp = NbtCompound()
            leashComp["UUID"] = NbtIntArray(holderUuid)
            javaEntity["leash"] = leashComp
            javaEntity["Leash"] = leashComp
        }

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

    private fun mooshroom(javaEntity: NbtCompound, bedrockActor: NbtCompound, definitions: List<String>) {
        val isBrown = getInt(bedrockActor, "Variant", 0) == 1 ||
                definitions.any { it.contains("mooshroom_brown") }
        javaEntity["Type"] = NbtString(if (isBrown) "brown" else "red")
    }

    private fun fallingBlock(javaEntity: NbtCompound, bedrockActor: NbtCompound) {
        val falling = bedrockActor.getNullableCompound("FallingBlock") ?: return
        val name = falling.getNullableString("name") ?: falling.getNullableString("Name") ?: return
        val blockState = NbtCompound()
        blockState["Name"] = NbtString(if (name.contains(":")) name else "minecraft:$name")
        val states = falling.getNullableCompound("states") ?: falling.getNullableCompound("States")
        if (states != null && states.isNotEmpty()) {
            val props = NbtCompound()
            for ((k, v) in states) {
                when (v) {
                    is NbtString -> props[k] = NbtString(v.value)
                    is NbtByte -> props[k] = NbtString(if (v.signed != 0.toByte()) "true" else "false")
                    is NbtInt -> props[k] = NbtString(v.value.toString())
                    is NbtShort -> props[k] = NbtString(v.value.toString())
                    else -> {}
                }
            }
            if (props.isNotEmpty()) {
                blockState["Properties"] = props
            }
        }
        javaEntity["BlockState"] = blockState
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

    private fun readCollarColor(bedrockActor: NbtCompound): NbtByte {
        val collar = getByte(bedrockActor, "CollarColor", -1)
        val color = if (collar >= 0) collar else getByte(bedrockActor, "Color", 14)
        return NbtByte(color.coerceIn(0, 15))
    }

    private fun cat(javaEntity: NbtCompound, bedrockActor: NbtCompound) {
        val variantIndex = getInt(bedrockActor, "Variant", 0)
        val breed = CAT_VARIANTS_REVERSE[variantIndex] ?: "black"
        javaEntity["variant"] = NbtString(NAMESPACE + breed)
        javaEntity["CollarColor"] = readCollarColor(bedrockActor)
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
        javaEntity["CollarColor"] = readCollarColor(bedrockActor)
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

    private fun villager(
        javaEntity: NbtCompound,
        bedrockActor: NbtCompound,
        definitions: List<String>,
        options: JavaTargetOptions
    ) {
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
        val javaOffers = EntityStates.mapOffersToJava(
            bedrockOffers,
            modernComponents = options.modernComponents,
            flatComponents = options.flatComponents
        )
        if (javaOffers != null) {
            javaEntity["Offers"] = javaOffers
        }
    }

    private fun restoreEquipment(
        javaEntity: NbtCompound,
        bedrockActor: NbtCompound,
        options: JavaTargetOptions
    ) {
        val mainhand = EntityEquipment.convertBedrockItemToJava(
            bedrockActor.getNullableCompoundList("Mainhand")?.firstOrNull(),
            options.modernComponents,
            options.flatComponents
        )
        val offhand = EntityEquipment.convertBedrockItemToJava(
            bedrockActor.getNullableCompoundList("Offhand")?.firstOrNull(),
            options.modernComponents,
            options.flatComponents
        )

        val armorList = bedrockActor.getNullableCompoundList("Armor")
        val head = EntityEquipment.convertBedrockItemToJava(armorList?.getOrNull(0), options.modernComponents, options.flatComponents)
        val chest = EntityEquipment.convertBedrockItemToJava(armorList?.getOrNull(1), options.modernComponents, options.flatComponents)
        val legs = EntityEquipment.convertBedrockItemToJava(armorList?.getOrNull(2), options.modernComponents, options.flatComponents)
        val feet = EntityEquipment.convertBedrockItemToJava(armorList?.getOrNull(3), options.modernComponents, options.flatComponents)
        val body = EntityEquipment.convertBedrockItemToJava(armorList?.getOrNull(4), options.modernComponents, options.flatComponents)

        if (options.modernEquipment) {
            val equip = NbtCompound()
            if (mainhand != null) equip["mainhand"] = mainhand
            if (offhand != null) equip["offhand"] = offhand
            if (head != null) equip["head"] = head
            if (chest != null) equip["chest"] = chest
            if (legs != null) equip["legs"] = legs
            if (feet != null) equip["feet"] = feet
            if (body != null) equip["body"] = body
            if (equip.isNotEmpty()) {
                javaEntity["equipment"] = equip
            }
        } else {
            val handItems = NbtList<NbtCompound>()
            handItems.add(mainhand ?: NbtCompound())
            handItems.add(offhand ?: NbtCompound())
            javaEntity["HandItems"] = handItems

            val armorItems = NbtList<NbtCompound>()
            armorItems.add(feet ?: NbtCompound())
            armorItems.add(legs ?: NbtCompound())
            armorItems.add(chest ?: NbtCompound())
            armorItems.add(head ?: NbtCompound())
            javaEntity["ArmorItems"] = armorItems

            if (body != null) {
                javaEntity["ArmorItem"] = body
            }
        }
    }

    private fun restoreSaddleAndChest(
        javaEntity: NbtCompound,
        bedrockId: String,
        bedrockActor: NbtCompound,
        definitions: List<String>,
        flags: Long,
        options: JavaTargetOptions
    ) {
        val isSaddled = (flags and (1L shl 48)) != 0L ||
                getByte(bedrockActor, "Saddled", 0) != 0.toByte() ||
                definitions.any { it.contains("saddled") }

        if (isSaddled) {
            if (options.modernEquipment) {
                val eq = javaEntity.getNullableCompound("equipment") ?: NbtCompound().also {
                    javaEntity["equipment"] = it
                }
                val saddle = NbtCompound().apply {
                    this["id"] = NbtString("minecraft:saddle")
                    this["count"] = NbtInt(1)
                }
                eq["saddle"] = saddle
            } else {
                val saddleTag = NbtCompound().apply {
                    this["id"] = NbtString("minecraft:saddle")
                    if (options.modernComponents) {
                        this["count"] = NbtInt(1)
                    } else {
                        this["Count"] = NbtByte(1)
                    }
                }
                javaEntity["SaddleItem"] = saddleTag
            }
        }

        val isChested = getByte(bedrockActor, "Chested", 0) != 0.toByte() ||
                getByte(bedrockActor, "ChestedHorse", 0) != 0.toByte()
        if (isChested) {
            javaEntity["ChestedHorse"] = NbtByte(1)
        }
    }

    private fun item(javaEntity: NbtCompound, bedrockActor: NbtCompound, options: JavaTargetOptions) {
        val bItem = bedrockActor.getNullableCompound("Item")
        val jItem = EntityEquipment.convertBedrockItemToJava(
            bItem,
            options.modernComponents,
            options.flatComponents
        )
        if (jItem != null && jItem.isNotEmpty()) {
            javaEntity["Item"] = jItem
        }
        val age = getShort(bedrockActor, "Age", 0)
        javaEntity["Age"] = NbtShort(age)
        javaEntity["Health"] = NbtShort(5)
    }

    private fun xpOrb(javaEntity: NbtCompound, bedrockActor: NbtCompound) {
        val value = getInt(
            bedrockActor,
            "experience value",
            getInt(bedrockActor, "Value", 1)
        ).coerceIn(1, Short.MAX_VALUE.toInt()).toShort()
        javaEntity["Value"] = NbtShort(value)
        val age = getShort(bedrockActor, "Age", 0)
        javaEntity["Age"] = NbtShort(age)
    }

    private fun tnt(javaEntity: NbtCompound, bedrockActor: NbtCompound, options: JavaTargetOptions) {
        val fuse = getShort(bedrockActor, "Fuse", getShort(bedrockActor, "fuse", 80)).coerceAtLeast(0)
        if (options.shortFuse) {
            javaEntity["fuse"] = NbtShort(fuse)
        } else {
            javaEntity["Fuse"] = NbtByte(fuse.coerceIn(0, 127).toByte())
        }
    }

    private fun potion(
        javaEntity: NbtCompound,
        bedrockId: String,
        bedrockActor: NbtCompound,
        options: JavaTargetOptions
    ) {
        val bItem = bedrockActor.getNullableCompound("Item")
        val converted = EntityEquipment.convertBedrockItemToJava(
            bItem,
            options.modernComponents,
            options.flatComponents
        )
        if (converted != null) {
            javaEntity["Item"] = converted
        } else if (!options.modernPotionIds || bedrockId == "minecraft:lingering_potion") {
            val fallbackItem = NbtCompound().apply {
                this["id"] = NbtString(bedrockId)
                if (options.modernComponents) {
                    this["count"] = NbtInt(1)
                } else {
                    this["Count"] = NbtByte(1)
                }
            }
            javaEntity["Item"] = fallbackItem
        }
    }

    private fun endCrystal(javaEntity: NbtCompound, bedrockActor: NbtCompound) {
        val showBottom = getByte(bedrockActor, "ShowBottom", 0)
        javaEntity["ShowBottom"] = NbtByte(showBottom)
    }

    private fun extractHealth(bedrockActor: NbtCompound, defaultHp: Float = 20f): Float {
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
        return defaultHp
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
