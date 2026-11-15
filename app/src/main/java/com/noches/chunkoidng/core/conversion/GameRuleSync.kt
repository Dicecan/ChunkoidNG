package com.noches.chunkoidng.core.conversion

import br.com.gamemods.nbtmanipulator.NbtByte
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtIO
import br.com.gamemods.nbtmanipulator.NbtInt
import br.com.gamemods.nbtmanipulator.NbtShort
import br.com.gamemods.nbtmanipulator.NbtString
import br.com.gamemods.nbtmanipulator.NbtTag
import com.noches.chunkoidng.core.entity.JavaTargetFormatHelper
import com.noches.chunkoidng.core.world.Platform
import java.io.File

object GameRuleSync {

    private const val NAMESPACE = "minecraft:"
    private const val DEFAULT_FIRE_SPREAD_RADIUS = 128

    private enum class RuleType { BOOL, INT }

    private data class RuleSpec(
        val canonical: String,
        val type: RuleType,
        val jeLegacyKey: String? = null,
        val jeLegacyInverted: Boolean = false,
        val jeModernKey: String = NAMESPACE + canonical,
        val beKeys: List<String> = emptyList()
    )

    private val RULE_SPECS: List<RuleSpec> = listOf(
        RuleSpec("mob_griefing", RuleType.BOOL, "mobGriefing", beKeys = listOf("mobgriefing")),
        RuleSpec("spawn_mobs", RuleType.BOOL, "doMobSpawning", beKeys = listOf("domobspawning", "spawnMobs")),
        RuleSpec("spawn_phantoms", RuleType.BOOL, "doInsomnia", beKeys = listOf("doinsomnia")),
        RuleSpec("advance_time", RuleType.BOOL, "doDaylightCycle", beKeys = listOf("dodaylightcycle")),
        RuleSpec("advance_weather", RuleType.BOOL, "doWeatherCycle", beKeys = listOf("doweathercycle")),
        RuleSpec("block_drops", RuleType.BOOL, "doTileDrops", beKeys = listOf("dotiledrops")),
        RuleSpec("entity_drops", RuleType.BOOL, "doEntityDrops", beKeys = listOf("doentitydrops")),
        RuleSpec("mob_drops", RuleType.BOOL, "doMobLoot", beKeys = listOf("domobloot")),
        RuleSpec("keep_inventory", RuleType.BOOL, "keepInventory", beKeys = listOf("keepinventory")),
        RuleSpec("natural_health_regeneration", RuleType.BOOL, "naturalRegeneration", beKeys = listOf("naturalregeneration")),
        RuleSpec("command_block_output", RuleType.BOOL, "commandBlockOutput", beKeys = listOf("commandblockoutput")),
        RuleSpec("command_blocks_work", RuleType.BOOL, null, beKeys = listOf("commandblocksenabled")),
        RuleSpec("send_command_feedback", RuleType.BOOL, "sendCommandFeedback", beKeys = listOf("sendcommandfeedback")),
        RuleSpec("show_death_messages", RuleType.BOOL, "showDeathMessages", beKeys = listOf("showdeathmessages")),
        RuleSpec("immediate_respawn", RuleType.BOOL, "doImmediateRespawn", beKeys = listOf("doimmediaterespawn")),
        RuleSpec("drowning_damage", RuleType.BOOL, "drowningDamage", beKeys = listOf("drowningdamage")),
        RuleSpec("fall_damage", RuleType.BOOL, "fallDamage", beKeys = listOf("falldamage")),
        RuleSpec("fire_damage", RuleType.BOOL, "fireDamage", beKeys = listOf("firedamage")),
        RuleSpec("freeze_damage", RuleType.BOOL, "freezeDamage", beKeys = listOf("freezedamage")),
        RuleSpec("pvp", RuleType.BOOL, null, beKeys = listOf("pvp")),
        RuleSpec("tnt_explodes", RuleType.BOOL, "tntExplodes", beKeys = listOf("tntexplodes")),
        RuleSpec("tnt_explosion_drop_decay", RuleType.BOOL, "tntExplosionDropDecay", beKeys = listOf("tntexplosiondropdecay")),
        RuleSpec("locator_bar", RuleType.BOOL, "locatorBar", beKeys = listOf("locatorbar")),
        RuleSpec("projectiles_can_break_blocks", RuleType.BOOL, "projectilesCanBreakBlocks", beKeys = listOf("projectilescanbreakblocks")),
        RuleSpec("players_sleeping_percentage", RuleType.INT, "playersSleepingPercentage", beKeys = listOf("playerssleepingpercentage")),
        RuleSpec("random_tick_speed", RuleType.INT, "randomTickSpeed", beKeys = listOf("randomtickspeed")),
        RuleSpec("respawn_radius", RuleType.INT, "spawnRadius", beKeys = listOf("spawnradius")),
        RuleSpec("max_command_sequence_length", RuleType.INT, "maxCommandChainLength", beKeys = listOf("maxcommandchainlength")),

        RuleSpec("raids", RuleType.BOOL, "disableRaids", jeLegacyInverted = true),
        RuleSpec("elytra_movement_check", RuleType.BOOL, "disableElytraMovementCheck", jeLegacyInverted = true),
        RuleSpec("player_movement_check", RuleType.BOOL, "disablePlayerMovementCheck", jeLegacyInverted = true),
        RuleSpec("show_advancement_messages", RuleType.BOOL, "announceAdvancements"),
        RuleSpec("limited_crafting", RuleType.BOOL, "doLimitedCrafting"),
        RuleSpec("log_admin_commands", RuleType.BOOL, "logAdminCommands"),
        RuleSpec("reduced_debug_info", RuleType.BOOL, "reducedDebugInfo"),
        RuleSpec("spectators_generate_chunks", RuleType.BOOL, "spectatorsGenerateChunks"),
        RuleSpec("global_sound_events", RuleType.BOOL, "globalSoundEvents"),
        RuleSpec("forgive_dead_players", RuleType.BOOL, "forgiveDeadPlayers"),
        RuleSpec("universal_anger", RuleType.BOOL, "universalAnger"),
        RuleSpec("spread_vines", RuleType.BOOL, "doVinesSpread"),
        RuleSpec("spawn_patrols", RuleType.BOOL, "doPatrolSpawning"),
        RuleSpec("spawn_wandering_traders", RuleType.BOOL, "doTraderSpawning"),
        RuleSpec("spawn_wardens", RuleType.BOOL, "doWardenSpawning"),
        RuleSpec("lava_source_conversion", RuleType.BOOL, "lavaSourceConversion"),
        RuleSpec("water_source_conversion", RuleType.BOOL, "waterSourceConversion"),
        RuleSpec("block_explosion_drop_decay", RuleType.BOOL, "blockExplosionDropDecay"),
        RuleSpec("mob_explosion_drop_decay", RuleType.BOOL, "mobExplosionDropDecay"),
        RuleSpec("ender_pearls_vanish_on_death", RuleType.BOOL, "enderPearlsVanishOnDeath"),
        RuleSpec("allow_entering_nether_using_portals", RuleType.BOOL, null),
        RuleSpec("spawner_blocks_work", RuleType.BOOL, null),
        RuleSpec("spawn_monsters", RuleType.BOOL, null),
        RuleSpec("max_entity_cramming", RuleType.INT, "maxEntityCramming"),
        RuleSpec("max_block_modifications", RuleType.INT, "commandModificationBlockLimit"),
        RuleSpec("max_command_forks", RuleType.INT, "maxCommandForkCount"),
        RuleSpec("max_snow_accumulation_height", RuleType.INT, "snowAccumulationHeight"),
        RuleSpec("players_nether_portal_creative_delay", RuleType.INT, "playersNetherPortalCreativeDelay"),
        RuleSpec("players_nether_portal_default_delay", RuleType.INT, "playersNetherPortalDefaultDelay")
    )

    private val BE_EXTRA_BOOL_KEYS = listOf(
        "recipesunlock",
        "respawnblocksexplode",
        "showcoordinates",
        "showdaysplayed",
        "showrecipemessages"
    )

    private val BE_EXTRA_INT_KEYS = listOf(
        "functioncommandlimit"
    )

    private val SPEC_BY_JE_LEGACY = RULE_SPECS.mapNotNull { spec ->
        spec.jeLegacyKey?.let { it to spec }
    }.toMap()

    private val SPEC_BY_JE_MODERN = RULE_SPECS.associateBy { it.jeModernKey }

    private data class Snapshot(
        val bools: MutableMap<String, Boolean> = LinkedHashMap(),
        val ints: MutableMap<String, Int> = LinkedHashMap(),
        var doFireTick: Boolean? = null,
        var fireSpreadRadius: Int? = null,
        val extraJeLegacy: MutableMap<String, String> = LinkedHashMap(),
        val extraJeModern: MutableMap<String, NbtTag> = LinkedHashMap(),
        val extraBeTags: MutableMap<String, NbtTag> = LinkedHashMap()
    ) {
        fun isEmpty(): Boolean =
            bools.isEmpty() && ints.isEmpty() && doFireTick == null && fireSpreadRadius == null &&
                extraJeLegacy.isEmpty() && extraJeModern.isEmpty() && extraBeTags.isEmpty()
    }

    fun sync(inputDir: File, outputDir: File, targetFormat: ChunkerFormat): Int {
        val isInputBedrock = isBedrockWorld(inputDir)
        val isInputJava = isJavaWorld(inputDir)

        val snapshot = when {
            isInputBedrock && !isInputJava -> readBedrockSnapshot(inputDir)
            isInputJava -> readJavaSnapshot(inputDir)
            else -> Snapshot()
        }
        if (snapshot.isEmpty()) return 0

        val targetIsBedrock = when {
            targetFormat.id == "INPUT" -> isBedrockWorld(outputDir) || (isInputBedrock && !isInputJava)
            else -> targetFormat.platform == Platform.BEDROCK
        }

        return if (targetIsBedrock) {
            writeBedrockSnapshot(outputDir, snapshot, preserveBeExtras = isInputBedrock)
        } else {
            writeJavaSnapshot(outputDir, targetFormat.id, snapshot, preserveJeExtras = isInputJava)
        }
    }

    private fun isBedrockWorld(dir: File): Boolean =
        File(dir, "db").isDirectory || isBedrockLevelDat(File(dir, "level.dat"))

    private fun isJavaWorld(dir: File): Boolean =
        File(dir, "data/minecraft/game_rules.dat").isFile ||
            File(dir, "region").isDirectory ||
            File(dir, "entities").isDirectory ||
            File(dir, "dimensions").isDirectory ||
            isGzipFile(File(dir, "level.dat"))

    private fun isBedrockLevelDat(file: File): Boolean {
        if (!file.isFile || file.length() < 12L) return false
        return !isGzipFile(file)
    }

    private fun isGzipFile(file: File): Boolean {
        if (!file.isFile || file.length() < 2L) return false
        return try {
            file.inputStream().use { input ->
                val b0 = input.read()
                val b1 = input.read()
                b0 == 0x1F && b1 == 0x8B
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun readJavaSnapshot(inputDir: File): Snapshot {
        val snapshot = Snapshot()
        var foundRules = false

        val gameRulesDat = File(inputDir, "data/minecraft/game_rules.dat")
        if (gameRulesDat.isFile) {
            val rootFile = runCatching { NbtIO.readNbtFile(gameRulesDat, compressed = true, littleEndian = false) }.getOrNull()
            val root = rootFile?.tag as? NbtCompound
            if (root != null) {
                val rulesCompound = (root["data"] as? NbtCompound) ?: root
                readJeModernCompound(rulesCompound, snapshot)
                foundRules = true
            }
        }

        val levelDat = File(inputDir, "level.dat")
        if (!foundRules && levelDat.isFile) {
            val rootFile = runCatching { NbtIO.readNbtFile(levelDat, compressed = true, littleEndian = false) }.getOrNull()
            val root = rootFile?.tag as? NbtCompound
            val data = (root?.get("Data") as? NbtCompound) ?: root
            if (data != null) {
                val modernRules = data["game_rules"] as? NbtCompound
                if (modernRules != null) {
                    readJeModernCompound(modernRules, snapshot)
                    foundRules = true
                }
                val legacyRules = data["GameRules"] as? NbtCompound
                if (legacyRules != null && !foundRules) {
                    readJeLegacyCompound(legacyRules, snapshot)
                }
            }
        }
        return snapshot
    }

    private fun readJeModernCompound(compound: NbtCompound, snapshot: Snapshot) {
        for ((key, tag) in compound) {
            if (key == "${NAMESPACE}fire_spread_radius_around_player") {
                val radius = readIntValue(tag)
                if (radius != null) {
                    snapshot.fireSpreadRadius = radius
                    snapshot.doFireTick = radius != 0
                }
                continue
            }
            val spec = SPEC_BY_JE_MODERN[key]
            if (spec != null) {
                when (spec.type) {
                    RuleType.BOOL -> readBoolValue(tag)?.let { snapshot.bools[spec.canonical] = it }
                    RuleType.INT -> readIntValue(tag)?.let { snapshot.ints[spec.canonical] = it }
                }
            } else {
                snapshot.extraJeModern[key] = tag
            }
        }
    }

    private fun readJeLegacyCompound(compound: NbtCompound, snapshot: Snapshot) {
        for ((key, tag) in compound) {
            val rawStr = (tag as? NbtString)?.value
            if (key == "doFireTick") {
                val value = parseBoolTag(tag)
                if (value != null) {
                    snapshot.doFireTick = value
                    if (!value) snapshot.fireSpreadRadius = 0
                }
                continue
            }
            val spec = SPEC_BY_JE_LEGACY[key]
            if (spec != null) {
                when (spec.type) {
                    RuleType.BOOL -> {
                        val b = parseBoolTag(tag)
                        if (b != null) {
                            snapshot.bools[spec.canonical] = if (spec.jeLegacyInverted) !b else b
                        }
                    }
                    RuleType.INT -> {
                        val i = parseIntTag(tag)
                        if (i != null) {
                            snapshot.ints[spec.canonical] = i
                        }
                    }
                }
            } else if (rawStr != null) {
                snapshot.extraJeLegacy[key] = rawStr
            }
        }
    }

    private fun readBedrockSnapshot(inputDir: File): Snapshot {
        val snapshot = Snapshot()
        val levelDat = File(inputDir, "level.dat")
        if (!levelDat.isFile) return snapshot

        val nbtFile = runCatching {
            NbtIO.readNbtFile(levelDat, compressed = false, littleEndian = true, readHeaders = true)
        }.getOrNull() ?: return snapshot

        val root = nbtFile.tag as? NbtCompound ?: return snapshot

        if (root.contains("dofiretick")) {
            readBoolValue(root["dofiretick"])?.let { snapshot.doFireTick = it }
        }

        for (spec in RULE_SPECS) {
            if (spec.beKeys.isEmpty()) continue
            var resolved = false
            for (beKey in spec.beKeys) {
                val tag = root[beKey] ?: continue
                when (spec.type) {
                    RuleType.BOOL -> {
                        val b = readBoolValue(tag)
                        if (b != null) {
                            snapshot.bools[spec.canonical] = b
                            resolved = true
                            break
                        }
                    }
                    RuleType.INT -> {
                        val i = readIntValue(tag)
                        if (i != null) {
                            snapshot.ints[spec.canonical] = i
                            resolved = true
                            break
                        }
                    }
                }
            }
            if (!resolved && spec.canonical == "spawn_mobs" && root.contains("spawnMobs")) {
                readBoolValue(root["spawnMobs"])?.let { snapshot.bools[spec.canonical] = it }
            }
        }

        for (key in BE_EXTRA_BOOL_KEYS) {
            root[key]?.let { snapshot.extraBeTags[key] = it }
        }
        for (key in BE_EXTRA_INT_KEYS) {
            root[key]?.let { snapshot.extraBeTags[key] = it }
        }

        return snapshot
    }

    private fun writeBedrockSnapshot(outputDir: File, snapshot: Snapshot, preserveBeExtras: Boolean): Int {
        val levelDat = File(outputDir, "level.dat")
        if (!levelDat.isFile) return 0

        val nbtFile = runCatching {
            NbtIO.readNbtFile(levelDat, compressed = false, littleEndian = true, readHeaders = true)
        }.getOrNull() ?: return 0

        val root = nbtFile.tag as? NbtCompound ?: return 0
        var updated = 0

        for (spec in RULE_SPECS) {
            if (spec.beKeys.isEmpty()) continue
            when (spec.type) {
                RuleType.BOOL -> {
                    val value = snapshot.bools[spec.canonical] ?: continue
                    val b: Byte = if (value) 1 else 0
                    for (beKey in spec.beKeys) {
                        root[beKey] = NbtByte(b)
                    }
                    updated++
                }
                RuleType.INT -> {
                    val value = snapshot.ints[spec.canonical] ?: continue
                    for (beKey in spec.beKeys) {
                        root[beKey] = NbtInt(value)
                    }
                    updated++
                }
            }
        }

        snapshot.doFireTick?.let { enabled ->
            root["dofiretick"] = NbtByte(if (enabled) 1 else 0)
            updated++
        }

        if (preserveBeExtras) {
            for ((key, tag) in snapshot.extraBeTags) {
                root[key] = tag
                updated++
            }
        }

        if (updated > 0) {
            NbtIO.writeNbtFile(levelDat, nbtFile, compressed = false, littleEndian = true, writeHeaders = true)
        }
        return updated
    }

    private fun writeJavaSnapshot(
        outputDir: File,
        targetFormatId: String,
        snapshot: Snapshot,
        preserveJeExtras: Boolean
    ): Int {
        val levelDat = File(outputDir, "level.dat")
        val gameRulesDat = File(outputDir, "data/minecraft/game_rules.dat")

        val levelFile = if (levelDat.isFile) {
            runCatching { NbtIO.readNbtFile(levelDat, compressed = true, littleEndian = false) }.getOrNull()
        } else null
        val levelRoot = levelFile?.tag as? NbtCompound
        val levelData = (levelRoot?.get("Data") as? NbtCompound) ?: levelRoot
        val outputDataVersion = (levelData?.get("DataVersion") as? NbtInt)?.value ?: 0

        val targetIs26Plus = isJava26Plus(targetFormatId) || gameRulesDat.isFile || outputDataVersion >= 4700
        val targetIs12111 = !targetIs26Plus && (isJava12111(targetFormatId) || levelData?.contains("game_rules") == true || outputDataVersion >= 4671)

        if (targetIs26Plus) {
            return writeJava26GameRulesDat(gameRulesDat, targetFormatId, outputDataVersion, snapshot, preserveJeExtras)
        }
        if (levelFile == null || levelData == null) return 0

        var updated = 0
        if (targetIs12111) {
            val rules = (levelData["game_rules"] as? NbtCompound) ?: NbtCompound().also { levelData["game_rules"] = it }
            updated = applyJeModernRules(rules, snapshot, preserveJeExtras)
            levelData.remove("GameRules")
        } else {
            val rules = (levelData["GameRules"] as? NbtCompound) ?: NbtCompound().also { levelData["GameRules"] = it }
            updated = applyJeLegacyRules(rules, snapshot, preserveJeExtras)
        }

        if (updated > 0) {
            NbtIO.writeNbtFile(levelDat, levelFile, compressed = true, littleEndian = false)
        }
        return updated
    }

    private fun writeJava26GameRulesDat(
        gameRulesDat: File,
        targetFormatId: String,
        fallbackDataVersion: Int,
        snapshot: Snapshot,
        preserveJeExtras: Boolean
    ): Int {
        gameRulesDat.parentFile?.mkdirs()

        val existingFile = if (gameRulesDat.isFile) {
            runCatching { NbtIO.readNbtFile(gameRulesDat, compressed = true, littleEndian = false) }.getOrNull()
        } else null

        val root: NbtCompound
        val dataCompound: NbtCompound
        val fileToWrite: NbtFile

        if (existingFile != null && existingFile.tag is NbtCompound) {
            root = existingFile.tag as NbtCompound
            val existingData = root["data"] as? NbtCompound
            if (existingData != null) {
                dataCompound = existingData
            } else {
                dataCompound = root
            }
            fileToWrite = existingFile
        } else {
            root = NbtCompound()
            dataCompound = NbtCompound()
            root["data"] = dataCompound
            val dv = when {
                fallbackDataVersion > 0 -> fallbackDataVersion
                else -> defaultDataVersion(targetFormatId)
            }
            root["DataVersion"] = NbtInt(dv)
            fileToWrite = NbtFile("", root)
        }

        val updated = applyJeModernRules(dataCompound, snapshot, preserveJeExtras)
        if (updated > 0) {
            NbtIO.writeNbtFile(gameRulesDat, fileToWrite, compressed = true, littleEndian = false)
        }
        return updated
    }

    private fun applyJeModernRules(
        target: NbtCompound,
        snapshot: Snapshot,
        preserveJeExtras: Boolean
    ): Int {
        var updated = 0
        for (spec in RULE_SPECS) {
            when (spec.type) {
                RuleType.BOOL -> {
                    val value = snapshot.bools[spec.canonical] ?: continue
                    target[spec.jeModernKey] = NbtByte(if (value) 1 else 0)
                    updated++
                }
                RuleType.INT -> {
                    val value = snapshot.ints[spec.canonical] ?: continue
                    target[spec.jeModernKey] = NbtInt(value)
                    updated++
                }
            }
        }

        val radiusKey = "${NAMESPACE}fire_spread_radius_around_player"
        val radius = snapshot.fireSpreadRadius ?: snapshot.doFireTick?.let { enabled ->
            if (!enabled) 0 else {
                val existing = readIntValue(target[radiusKey])
                if (existing != null && existing != 0) existing else DEFAULT_FIRE_SPREAD_RADIUS
            }
        }
        if (radius != null) {
            target[radiusKey] = NbtInt(radius)
            updated++
        }

        if (preserveJeExtras) {
            for ((key, tag) in snapshot.extraJeModern) {
                target[key] = tag
                updated++
            }
        }
        return updated
    }

    private fun applyJeLegacyRules(
        target: NbtCompound,
        snapshot: Snapshot,
        preserveJeExtras: Boolean
    ): Int {
        var updated = 0
        for (spec in RULE_SPECS) {
            val legacyKey = spec.jeLegacyKey ?: continue
            when (spec.type) {
                RuleType.BOOL -> {
                    val value = snapshot.bools[spec.canonical] ?: continue
                    val legacyBool = if (spec.jeLegacyInverted) !value else value
                    target[legacyKey] = NbtString(legacyBool.toString())
                    updated++
                }
                RuleType.INT -> {
                    val value = snapshot.ints[spec.canonical] ?: continue
                    target[legacyKey] = NbtString(value.toString())
                    updated++
                }
            }
        }

        val fireTick = snapshot.doFireTick ?: snapshot.fireSpreadRadius?.let { it != 0 }
        if (fireTick != null) {
            target["doFireTick"] = NbtString(fireTick.toString())
            updated++
        }

        if (preserveJeExtras) {
            for ((key, value) in snapshot.extraJeLegacy) {
                target[key] = NbtString(value)
                updated++
            }
        }
        return updated
    }

    internal fun isJava26Plus(formatId: String): Boolean {
        val parts = parseJavaVersionParts(formatId) ?: return false
        return parts[0] >= 26
    }

    internal fun isJava12111(formatId: String): Boolean {
        val parts = parseJavaVersionParts(formatId) ?: return false
        return parts[0] == 1 && parts[1] == 21 && parts[2] >= 11
    }

    private fun parseJavaVersionParts(formatId: String): IntArray? {
        if (!formatId.startsWith("JAVA_")) return null
        val nums = formatId.removePrefix("JAVA_").split("_").mapNotNull { it.toIntOrNull() }
        if (nums.isEmpty()) return null
        return intArrayOf(
            nums.getOrElse(0) { 0 },
            nums.getOrElse(1) { 0 },
            nums.getOrElse(2) { 0 }
        )
    }

    private fun defaultDataVersion(formatId: String): Int =
        JavaTargetFormatHelper.resolveDataVersion(formatId)

    private fun readBoolValue(tag: NbtTag?): Boolean? = when (tag) {
        is NbtByte -> tag.signed != 0.toByte()
        is NbtShort -> tag.value != 0.toShort()
        is NbtInt -> tag.value != 0
        is NbtString -> tag.value.trim().lowercase().let {
            when (it) {
                "true", "1" -> true
                "false", "0" -> false
                else -> null
            }
        }
        else -> null
    }

    private fun readIntValue(tag: NbtTag?): Int? = when (tag) {
        is NbtInt -> tag.value
        is NbtShort -> tag.value.toInt()
        is NbtByte -> tag.signed.toInt()
        is NbtString -> tag.value.trim().toIntOrNull()
        else -> null
    }

    private fun parseBoolTag(tag: NbtTag?): Boolean? = when (tag) {
        is NbtString -> when (tag.value.trim().lowercase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> null
        }
        else -> readBoolValue(tag)
    }

    private fun parseIntTag(tag: NbtTag?): Int? = when (tag) {
        is NbtString -> tag.value.trim().toIntOrNull()
        else -> readIntValue(tag)
    }
}
