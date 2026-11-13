package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.*

object BedrockActorBuilder {

    private const val ITEM = "minecraft:item"
    private const val XP_ORB = "minecraft:xp_orb"
    private const val TNT = "minecraft:tnt"
    private const val END_CRYSTAL = "minecraft:ender_crystal"
    private const val FALLING_BLOCK = "minecraft:falling_block"
    private const val COMMAND_BLOCK_MINECART = "minecraft:command_block_minecart"
    private const val EVOCATION_FANG = "minecraft:evocation_fang"
    private const val MIN_COORD = -30_000_000.0
    private const val MAX_COORD = 30_000_000.0
    private const val MAX_MOTION = 10.0
    private const val MINECART_LIFT = 0.35f

    private val NON_DESPAWNING_ENTITIES = setOf(
        "minecraft:boat",
        "minecraft:chest_boat",
        "minecraft:minecart",
        "minecraft:chest_minecart",
        "minecraft:hopper_minecart",
        "minecraft:tnt_minecart",
        "minecraft:command_block_minecart",
        "minecraft:furnace_minecart",
        "minecraft:armor_stand",
        END_CRYSTAL,
        "minecraft:painting",
        "minecraft:leash_knot",
        "minecraft:interaction",
        "minecraft:marker",
        "minecraft:block_display",
        "minecraft:item_display",
        "minecraft:text_display"
    )

    private val EQUINE_ENTITIES = setOf(
        "minecraft:horse",
        "minecraft:donkey",
        "minecraft:mule",
        "minecraft:skeleton_horse",
        "minecraft:zombie_horse",
        "minecraft:camel",
        "minecraft:camel_husk",
        "minecraft:llama",
        "minecraft:trader_llama"
    )

    fun build(
        javaEntity: NbtCompound,
        uniqueId: Long,
        hasPassengers: Boolean = false,
        isPassenger: Boolean = false
    ): NbtCompound? {
        val javaId = javaEntity.getNullableString("id") ?: return null
        var identifier = EntityIdMapper.toBedrock(javaId) ?: return null

        if (identifier == "minecraft:splash_potion") {
            val thrownId = javaEntity.getNullableCompound("Item")?.getNullableString("id")
            if (thrownId == "minecraft:lingering_potion") {
                identifier = "minecraft:lingering_potion"
            }
        }

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
            if (row.hasProperties()) {
                actor["properties"] = row.properties()
            }
        }

        val pos = convertDoubleVectorToFloat(javaEntity.getNullableDoubleList("Pos"), MIN_COORD, MAX_COORD)
        if (isMinecart(identifier) && pos.size >= 2) {
            pos[1] = NbtFloat(pos[1].value + MINECART_LIFT)
        }
        actor["Pos"] = pos
        actor["Motion"] = convertDoubleVectorToFloat(javaEntity.getNullableDoubleList("Motion"), -MAX_MOTION, MAX_MOTION)

        val rotation = javaEntity.getNullableFloatList("Rotation")
        val rotList = NbtList<NbtFloat>()
        val r0 = if (rotation != null && rotation.size > 0 && rotation[0].value.isFinite()) rotation[0].value else 0f
        val r1 = if (rotation != null && rotation.size > 1 && rotation[1].value.isFinite()) rotation[1].value else 0f
        rotList.add(NbtFloat(r0))
        rotList.add(NbtFloat(r1))
        actor["Rotation"] = rotList

        if (EntityGroupTable.hasHealth(identifier)) {
            val tableMaxHp = EntityGroupTable.healthOrNull(identifier)!!
            val maxHp = attributeBase(javaEntity, "minecraft:max_health")?.takeIf { it.isFinite() && it > 0f } ?: tableMaxHp
            val rawHp = getFloat(javaEntity, "Health", maxHp)
            val health = (if (rawHp.isFinite() && rawHp > 0f) rawHp else maxHp).coerceIn(0f, maxHp)
            actor["Health"] = NbtShort(health.toInt().toShort())

            val attrList = NbtList<NbtCompound>()
            attrList.add(buildAttribute("minecraft:health", maxHp, health, 0f, maxHp))
            if (identifier in EQUINE_ENTITIES) {
                val movementSpeed = attributeBase(
                    javaEntity,
                    "minecraft:movement_speed",
                    "minecraft:generic.movement_speed"
                )
                if (movementSpeed != null && movementSpeed > 0f) {
                    attrList.add(
                        buildAttribute(
                            "minecraft:movement",
                            movementSpeed,
                            movementSpeed,
                            0f,
                            Float.MAX_VALUE
                        )
                    )
                }
                val jumpStrength = attributeBase(
                    javaEntity,
                    "minecraft:jump_strength",
                    "minecraft:horse.jump_strength",
                    "minecraft:generic.jump_strength"
                )
                if (jumpStrength != null && jumpStrength > 0f) {
                    attrList.add(
                        buildAttribute(
                            "minecraft:horse.jump_strength",
                            jumpStrength,
                            jumpStrength,
                            0f,
                            Float.MAX_VALUE
                        )
                    )
                }
            }
            actor["Attributes"] = attrList
        }

        val cleanName = extractCustomName(javaEntity["CustomName"])
        if (!cleanName.isNullOrEmpty()) {
            actor["CustomName"] = NbtString(cleanName)
            val nameVisible = getByte(javaEntity, "CustomNameVisible", 0)
            actor["CustomNameVisible"] = NbtByte(nameVisible)
        }

        val carriesPassengers = hasPassengers || !javaEntity.getNullableCompoundList("Passengers").isNullOrEmpty()
        val isTamed = getByte(javaEntity, "Tame", 0) != 0.toByte() ||
                getByte(javaEntity, "Tamed", 0) != 0.toByte() ||
                javaEntity.contains("Owner")
        val persistenceRequired = getByte(javaEntity, "PersistenceRequired", 0) != 0.toByte() ||
                getByte(javaEntity, "Persistent", 0) != 0.toByte() ||
                !cleanName.isNullOrEmpty() ||
                javaEntity.contains("leash") ||
                javaEntity.contains("Leash") ||
                isTamed ||
                carriesPassengers ||
                isPassenger ||
                identifier in NON_DESPAWNING_ENTITIES
        if (persistenceRequired) {
            actor["Persistent"] = NbtByte(1)
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

        val tags = javaEntity.getNullableStringList("Tags")
        if (!tags.isNullOrEmpty()) {
            val bedrockTags = NbtList<NbtString>()
            for (tag in tags) {
                bedrockTags.add(NbtString(tag.value))
            }
            actor["Tags"] = bedrockTags
        }

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
                val value = getInt(
                    javaEntity,
                    "Value",
                    getInt(javaEntity, "experience value", 1)
                ).coerceAtLeast(1)
                actor["experience value"] = NbtInt(value)
                val age = getShort(javaEntity, "Age", 0)
                actor["Age"] = NbtShort(age)
            }
            FALLING_BLOCK -> {
                val fallingBlock = convertFallingBlockToBedrock(javaEntity)
                if (fallingBlock != null) {
                    actor["FallingBlock"] = fallingBlock
                }
            }
            TNT -> {
                val fuse = getShort(
                    javaEntity,
                    "fuse",
                    getShort(javaEntity, "Fuse", 80)
                ).coerceAtLeast(0)
                actor["Fuse"] = NbtShort(fuse)
            }
            END_CRYSTAL -> {
                val showBottom = getByte(javaEntity, "ShowBottom", 0)
                actor["ShowBottom"] = NbtByte(showBottom)
            }
            else -> {
                EntityVariants.apply(actor, identifier, javaEntity, definitions)
                EntityStates.apply(actor, identifier, javaEntity, definitions)
                EntityEquipment.apply(actor, javaEntity)
                EntityContainers.apply(actor, javaEntity, identifier)

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
                } else if (identifier == COMMAND_BLOCK_MINECART) {
                    val cmd = javaEntity.getNullableString("Command")
                    if (!cmd.isNullOrEmpty()) {
                        actor["Command"] = NbtString(cmd)
                    }
                } else if (identifier == EVOCATION_FANG) {
                    val warmup = javaEntity.getNullableInt("Warmup")
                    if (warmup != null) {
                        actor["Warmup"] = NbtInt(warmup)
                    }
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

    private fun isMinecart(identifier: String): Boolean {
        return identifier == "minecraft:minecart" || identifier.endsWith("_minecart")
    }

    private fun convertDoubleVectorToFloat(
        doubleList: NbtList<NbtDouble>?,
        minVal: Double,
        maxVal: Double
    ): NbtList<NbtFloat> {
        val list = NbtList<NbtFloat>()
        for (i in 0 until 3) {
            val raw = if (doubleList != null && doubleList.size > i) doubleList[i].value else 0.0
            val clamped = if (raw.isFinite()) raw.coerceIn(minVal, maxVal) else 0.0
            list.add(NbtFloat(clamped.toFloat()))
        }
        return list
    }

    private fun convertFallingBlockToBedrock(entity: NbtCompound): NbtCompound? {
        val rawState = entity["BlockState"]
        var blockName: String? = null
        var javaProps: NbtCompound? = null
        when (rawState) {
            is NbtString -> blockName = rawState.value
            is NbtCompound -> {
                blockName = rawState.getNullableString("Name") ?: rawState.getNullableString("name")
                javaProps = rawState.getNullableCompound("Properties") ?: rawState.getNullableCompound("states")
            }
            else -> {
                blockName = entity.getNullableString("Block")
            }
        }
        val cleanName = blockName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val normalizedName = if (cleanName.contains(":")) cleanName else "minecraft:$cleanName"
        if (normalizedName == "minecraft:air") return null

        val states = NbtCompound()
        if (javaProps != null) {
            for ((k, v) in javaProps) {
                when (v) {
                    is NbtString -> {
                        val s = v.value
                        if (s == "true" || s == "false") {
                            states[k] = NbtByte(if (s == "true") 1 else 0)
                        } else {
                            states[k] = NbtString(s)
                        }
                    }
                    is NbtByte -> states[k] = NbtByte(v.signed)
                    is NbtInt -> states[k] = NbtInt(v.value)
                    is NbtShort -> states[k] = NbtInt(v.value.toInt())
                    else -> {}
                }
            }
        }

        return NbtCompound().apply {
            this["name"] = NbtString(normalizedName)
            this["states"] = states
            this["version"] = NbtInt(18163713)
        }
    }

    private fun buildAttribute(
        name: String,
        base: Float,
        current: Float,
        defaultMin: Float,
        defaultMax: Float
    ): NbtCompound {
        return NbtCompound().apply {
            this["Name"] = NbtString(name)
            this["Base"] = NbtFloat(base)
            this["Current"] = NbtFloat(current)
            this["DefaultMax"] = NbtFloat(defaultMax)
            this["DefaultMin"] = NbtFloat(defaultMin)
            this["Max"] = NbtFloat(defaultMax)
            this["Min"] = NbtFloat(defaultMin)
        }
    }

    private fun attributeBase(entity: NbtCompound, vararg ids: String): Float? {
        val attributes = entity.getNullableCompoundList("attributes")
            ?: entity.getNullableCompoundList("Attributes")
            ?: return null
        for (attr in attributes) {
            val attrId = attr.getNullableString("id") ?: attr.getNullableString("Name")
            if (attrId != null && attrId in ids) {
                val baseTag = attr["base"] ?: attr["Base"]
                val v = when (baseTag) {
                    is NbtDouble -> baseTag.value.toFloat()
                    is NbtFloat -> baseTag.value
                    is NbtInt -> baseTag.value.toFloat()
                    else -> null
                }
                if (v != null && v.isFinite()) return v
            }
        }
        return null
    }

    private fun extractCustomName(tag: NbtTag?): String? {
        return when (tag) {
            is NbtString -> cleanJsonText(tag.value).ifEmpty { null }
            is NbtCompound -> extractCompoundText(tag).ifEmpty { null }
            is NbtList<*> -> tag.joinToString("") { extractCustomName(it) ?: "" }.ifEmpty { null }
            else -> null
        }
    }

    private fun extractCompoundText(tag: NbtCompound): String {
        val sb = StringBuilder()
        appendLegacyFormatting(
            sb = sb,
            color = tag.getNullableString("color"),
            bold = getNullableBool(tag, "bold"),
            italic = getNullableBool(tag, "italic"),
            underlined = getNullableBool(tag, "underlined"),
            strikethrough = getNullableBool(tag, "strikethrough"),
            obfuscated = getNullableBool(tag, "obfuscated")
        )
        val text = tag.getNullableString("text")
        if (text != null) {
            sb.append(text)
        } else {
            val translate = tag.getNullableString("translate")
            if (!translate.isNullOrEmpty()) {
                sb.append(translate)
            }
        }
        val extraList = tag.getNullableList("extra")
        if (extraList != null) {
            for (item in extraList) {
                sb.append(extractCustomName(item) ?: "")
            }
        }
        return sb.toString()
    }

    private fun getNullableBool(compound: NbtCompound, key: String): Boolean? {
        return when (val tag = compound[key]) {
            is NbtByte -> tag.signed != 0.toByte()
            is NbtInt -> tag.value != 0
            is NbtString -> tag.value.equals("true", ignoreCase = true)
            else -> null
        }
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
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[") && !trimmed.startsWith("\"")) {
            return raw
        }
        val parsed = SimpleJsonTextParser(trimmed).parseComponentText()
        return parsed ?: raw
    }

    private fun appendLegacyFormatting(
        sb: StringBuilder,
        color: String?,
        bold: Boolean?,
        italic: Boolean?,
        underlined: Boolean?,
        strikethrough: Boolean?,
        obfuscated: Boolean?
    ) {
        if (color != null) {
            LEGACY_COLOR_CODES[color.lowercase()]?.let { sb.append('§').append(it) }
        }
        if (obfuscated == true) sb.append("§k")
        if (bold == true) sb.append("§l")
        if (strikethrough == true) sb.append("§m")
        if (underlined == true) sb.append("§n")
        if (italic == true) sb.append("§o")
    }

    private val LEGACY_COLOR_CODES = mapOf(
        "black" to '0',
        "dark_blue" to '1',
        "dark_green" to '2',
        "dark_aqua" to '3',
        "dark_red" to '4',
        "dark_purple" to '5',
        "gold" to '6',
        "gray" to '7',
        "dark_gray" to '8',
        "blue" to '9',
        "green" to 'a',
        "aqua" to 'b',
        "red" to 'c',
        "light_purple" to 'd',
        "yellow" to 'e',
        "white" to 'f',
        "reset" to 'r'
    )

    private class SimpleJsonTextParser(private val src: String) {
        private var pos = 0

        fun parseComponentText(): String? {
            return try {
                val value = readValue() ?: return null
                skipWs()
                if (pos != src.length) return null
                extractFromAny(value)
            } catch (_: Exception) {
                null
            }
        }

        private fun extractFromAny(value: Any?): String {
            return when (value) {
                is String -> value
                is List<*> -> value.joinToString("") { extractFromAny(it) }
                is Map<*, *> -> {
                    val sb = StringBuilder()
                    appendLegacyFormatting(
                        sb = sb,
                        color = value["color"] as? String,
                        bold = value["bold"] as? Boolean,
                        italic = value["italic"] as? Boolean,
                        underlined = value["underlined"] as? Boolean,
                        strikethrough = value["strikethrough"] as? Boolean,
                        obfuscated = value["obfuscated"] as? Boolean
                    )
                    val text = value["text"]?.toString()
                    if (text != null) {
                        sb.append(text)
                    } else {
                        val translate = value["translate"]?.toString()
                        if (translate != null) sb.append(translate)
                    }
                    val extra = value["extra"]
                    if (extra != null) {
                        sb.append(extractFromAny(extra))
                    }
                    sb.toString()
                }
                else -> ""
            }
        }

        private fun readValue(): Any? {
            skipWs()
            if (pos >= src.length) return null
            return when (src[pos]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't', 'f' -> readBoolean()
                'n' -> { readNull(); null }
                '-', in '0'..'9' -> readNumber()
                else -> throw IllegalArgumentException("Unexpected char at $pos")
            }
        }

        private fun readObject(): Map<String, Any?> {
            pos++ // '{'
            val map = linkedMapOf<String, Any?>()
            skipWs()
            if (pos < src.length && src[pos] == '}') {
                pos++
                return map
            }
            while (pos < src.length) {
                skipWs()
                if (src[pos] != '"') throw IllegalArgumentException("Expected string key")
                val key = readString()
                skipWs()
                if (pos >= src.length || src[pos] != ':') throw IllegalArgumentException("Expected ':'")
                pos++
                val v = readValue()
                map[key] = v
                skipWs()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                    continue
                }
                if (pos < src.length && src[pos] == '}') {
                    pos++
                    return map
                }
                throw IllegalArgumentException("Unterminated object")
            }
            throw IllegalArgumentException("Unterminated object")
        }

        private fun readArray(): List<Any?> {
            pos++ // '['
            val list = mutableListOf<Any?>()
            skipWs()
            if (pos < src.length && src[pos] == ']') {
                pos++
                return list
            }
            while (pos < src.length) {
                list.add(readValue())
                skipWs()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                    continue
                }
                if (pos < src.length && src[pos] == ']') {
                    pos++
                    return list
                }
                throw IllegalArgumentException("Unterminated array")
            }
            throw IllegalArgumentException("Unterminated array")
        }

        private fun readString(): String {
            pos++ // '"'
            val sb = StringBuilder()
            while (pos < src.length) {
                val c = src[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= src.length) throw IllegalArgumentException("Unterminated escape")
                        val esc = src[pos++]
                        when (esc) {
                            '"', '\\', '/' -> sb.append(esc)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > src.length) throw IllegalArgumentException("Bad unicode")
                                sb.append(src.substring(pos, pos + 4).toInt(16).toChar())
                                pos += 4
                            }
                            else -> sb.append(esc)
                        }
                    }
                    else -> sb.append(c)
                }
            }
            throw IllegalArgumentException("Unterminated string")
        }

        private fun readBoolean(): Boolean {
            return if (src.startsWith("true", pos)) {
                pos += 4; true
            } else if (src.startsWith("false", pos)) {
                pos += 5; false
            } else {
                throw IllegalArgumentException("Bad bool")
            }
        }

        private fun readNull() {
            if (src.startsWith("null", pos)) pos += 4
            else throw IllegalArgumentException("Bad null")
        }

        private fun readNumber(): Number {
            val start = pos
            while (pos < src.length && src[pos] in "0123456789+-.eE") pos++
            val token = src.substring(start, pos)
            return token.toLongOrNull() ?: token.toDouble()
        }

        private fun skipWs() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }
    }
}
