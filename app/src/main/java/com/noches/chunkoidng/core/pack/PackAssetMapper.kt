package com.noches.chunkoidng.core.pack

object PackAssetMapper {

    private const val JAVA_TEXTURE_ROOT = "assets/minecraft/"
    private const val BEDROCK_TEXTURE_UI_ROOT = "textures/ui/"
    private val JAVA_ARMOR_TEXTURE_REGEX = Regex(
        "^assets/[^/]+/textures/models/armor/[a-zA-Z0-9_]+_layer_[12]\\.png$",
        RegexOption.IGNORE_CASE
    )

    private val JAVA_TO_BEDROCK_BLOCK_NAMES = mapOf(
        "grass_block_top" to "grass_top",
        "grass_block_side" to "grass_side",
        "grass_block_side_overlay" to "grass_side_overlay",
        "dirt_path_top" to "grass_path_top",
        "dirt_path_side" to "grass_path_side",
        "oak_planks" to "planks_oak",
        "spruce_planks" to "planks_spruce",
        "birch_planks" to "planks_birch",
        "jungle_planks" to "planks_jungle",
        "acacia_planks" to "planks_acacia",
        "dark_oak_planks" to "planks_big_oak",
        "mangrove_planks" to "planks_mangrove",
        "cherry_planks" to "planks_cherry",
        "bamboo_planks" to "planks_bamboo",
        "crimson_planks" to "planks_crimson",
        "warped_planks" to "planks_warped",
        "stone_bricks" to "stonebrick",
        "mossy_stone_bricks" to "stonebrick_mossy",
        "cracked_stone_bricks" to "stonebrick_cracked",
        "chiseled_stone_bricks" to "stonebrick_carved",
        "mossy_cobblestone" to "cobblestone_mossy",
        "redstone_dust_dot" to "redstone_dust_cross",
        "redstone_dust_line0" to "redstone_dust_line",
        "repeater" to "repeater_off",
        "repeater_on" to "repeater_on",
        "comparator" to "comparator_up",
        "comparator_on" to "comparator_on",
        "furnace_front" to "furnace_front_off",
        "blast_furnace_front" to "blast_furnace_front_off",
        "smoker_front" to "smoker_front_off",
        "note_block" to "noteblock",
        "red_sand" to "sand_red",
        "packed_ice" to "ice_packed",
        "blue_ice" to "ice_blue",
        "magma_block" to "magma",
        "smooth_stone" to "stone_slab_top",
        "podzol_top" to "dirt_podzol_top",
        "podzol_side" to "dirt_podzol_side",
        "wet_sponge" to "sponge_wet"
    )

    private val BEDROCK_TO_JAVA_BLOCK_NAMES = JAVA_TO_BEDROCK_BLOCK_NAMES.entries.associate { (k, v) -> v to k }

    private val JAVA_TO_BEDROCK_ITEM_NAMES = mapOf(
        "golden_sword" to "gold_sword",
        "golden_pickaxe" to "gold_pickaxe",
        "golden_axe" to "gold_axe",
        "golden_shovel" to "gold_shovel",
        "golden_hoe" to "gold_hoe",
        "golden_helmet" to "gold_helmet",
        "golden_chestplate" to "gold_chestplate",
        "golden_leggings" to "gold_leggings",
        "golden_boots" to "gold_boots",
        "golden_apple" to "apple_golden",
        "golden_horse_armor" to "gold_horse_armor",
        "wooden_sword" to "wood_sword",
        "wooden_pickaxe" to "wood_pickaxe",
        "wooden_axe" to "wood_axe",
        "wooden_shovel" to "wood_shovel",
        "wooden_hoe" to "wood_hoe",
        "experience_bottle" to "bottle_enchanting",
        "ender_eye" to "eye_of_ender",
        "firework_rocket" to "fireworks",
        "firework_star" to "fireworks_charge",
        "empty_map" to "map_empty",
        "filled_map" to "map_filled",
        "bow" to "bow_standby",
        "crossbow" to "crossbow_standby"
    )

    private val BEDROCK_TO_JAVA_ITEM_NAMES = JAVA_TO_BEDROCK_ITEM_NAMES.entries.associate { (k, v) -> v to k }

    fun mapJavaToBedrock(relativePath: String): String? {
        val normalized = relativePath.replace('\\', '/')
        val lower = normalized.lowercase()

        if (lower == "pack.png") {
            return "pack_icon.png"
        }

        val armorRegex = Regex("^assets/([^/]+)/textures/models/armor/([a-zA-Z0-9_]+)_layer_([12])\\.png$", RegexOption.IGNORE_CASE)
        val armorMatch = armorRegex.find(normalized)
        if (armorMatch != null) {
            val namespace = armorMatch.groupValues[1].lowercase()
            val mat = armorMatch.groupValues[2]
            val layer = armorMatch.groupValues[3]
            val outputName = if (namespace == "minecraft") mat else "${namespace}_$mat"
            return "textures/models/armor/${outputName}_$layer.png"
        }

        val blockRegex = Regex("^assets/([^/]+)/textures/(?:block|blocks)/([a-zA-Z0-9_]+)(\\.[a-zA-Z0-9_.]+)$", RegexOption.IGNORE_CASE)
        val blockMatch = blockRegex.find(normalized)
        if (blockMatch != null) {
            val namespace = blockMatch.groupValues[1].lowercase()
            val rawName = blockMatch.groupValues[2].lowercase()
            val ext = blockMatch.groupValues[3].lowercase()
            val mappedName = JAVA_TO_BEDROCK_BLOCK_NAMES[rawName] ?: rawName
            val outputName = if (namespace == "minecraft") mappedName else "${namespace}_$mappedName"
            return "textures/blocks/$outputName$ext"
        }

        val itemRegex = Regex("^assets/([^/]+)/textures/(?:item|items)/([a-zA-Z0-9_]+)(\\.[a-zA-Z0-9_.]+)$", RegexOption.IGNORE_CASE)
        val itemMatch = itemRegex.find(normalized)
        if (itemMatch != null) {
            val namespace = itemMatch.groupValues[1].lowercase()
            val rawName = itemMatch.groupValues[2].lowercase()
            val ext = itemMatch.groupValues[3].lowercase()
            val mappedName = JAVA_TO_BEDROCK_ITEM_NAMES[rawName] ?: rawName
            val outputName = if (namespace == "minecraft") mappedName else "${namespace}_$mappedName"
            return "textures/items/$outputName$ext"
        }

        if (lower.startsWith("assets/minecraft/textures/entity/")) {
            return normalized.substring(JAVA_TEXTURE_ROOT.length)
        }

        if (lower.startsWith("assets/minecraft/textures/environment/")) {
            return normalized.substring(JAVA_TEXTURE_ROOT.length)
        }

        if (lower.startsWith("assets/minecraft/textures/particle/")) {
            return normalized.substring(JAVA_TEXTURE_ROOT.length)
        }

        if (lower.startsWith("assets/minecraft/textures/gui/")) {
            return "textures/ui/" + normalized.substring("assets/minecraft/textures/gui/".length)
        }

        if (lower.startsWith("assets/minecraft/textures/")) {
            return normalized.substring(JAVA_TEXTURE_ROOT.length)
        }

        if (lower.startsWith("assets/minecraft/sounds/")) {
            return normalized.substring(JAVA_TEXTURE_ROOT.length)
        }

        val customTexture = Regex("^assets/([^/]+)/textures/(.+)$", RegexOption.IGNORE_CASE).find(normalized)
        if (customTexture != null && !customTexture.groupValues[1].equals("minecraft", ignoreCase = true)) {
            return "textures/${customTexture.groupValues[1].lowercase()}/${customTexture.groupValues[2]}"
        }

        if (lower.startsWith("assets/minecraft/lang/")) {
            val fileName = normalized.substringAfterLast('/')
            val localeCode = fileName.substringBeforeLast('.', fileName)
            val bedrockLocale = mapJavaLocaleToBedrock(localeCode)
            return "texts/$bedrockLocale.lang"
        }

        if (lower == "assets/minecraft/texts/splashes.txt") {
            return "splashes.json"
        }

        if (lower == "assets/minecraft/sounds.json") {
            return "sounds/sound_definitions.json"
        }

        return null
    }

    fun mapBedrockToJava(relativePath: String, targetJavaFormat: Int = 34): String? {
        val normalized = relativePath.replace('\\', '/')
        val lower = normalized.lowercase()

        if (lower == "pack_icon.png") {
            return "pack.png"
        }

        val armorRegex = Regex("^textures/models/armor/([a-zA-Z0-9_]+)_([12])\\.png$", RegexOption.IGNORE_CASE)
        val armorMatch = armorRegex.find(normalized)
        if (armorMatch != null) {
            val mat = armorMatch.groupValues[1]
            val layer = armorMatch.groupValues[2]
            return "assets/minecraft/textures/models/armor/${mat}_layer_$layer.png"
        }

        val blockDir = if (targetJavaFormat <= 3) "blocks" else "block"
        val itemDir = if (targetJavaFormat <= 3) "items" else "item"

        val blockRegex = Regex("^textures/blocks/([a-zA-Z0-9_]+)(\\.[a-zA-Z0-9_.]+)$", RegexOption.IGNORE_CASE)
        val blockMatch = blockRegex.find(normalized)
        if (blockMatch != null) {
            val rawName = blockMatch.groupValues[1].lowercase()
            val ext = blockMatch.groupValues[2].lowercase()
            val mappedName = BEDROCK_TO_JAVA_BLOCK_NAMES[rawName] ?: rawName
            return "assets/minecraft/textures/$blockDir/$mappedName$ext"
        }

        val itemRegex = Regex("^textures/items/([a-zA-Z0-9_]+)(\\.[a-zA-Z0-9_.]+)$", RegexOption.IGNORE_CASE)
        val itemMatch = itemRegex.find(normalized)
        if (itemMatch != null) {
            val rawName = itemMatch.groupValues[1].lowercase()
            val ext = itemMatch.groupValues[2].lowercase()
            val mappedName = BEDROCK_TO_JAVA_ITEM_NAMES[rawName] ?: rawName
            return "assets/minecraft/textures/$itemDir/$mappedName$ext"
        }

        if (lower.startsWith("textures/entity/")) {
            return "assets/minecraft/$normalized"
        }

        if (lower.startsWith("textures/environment/")) {
            return "assets/minecraft/$normalized"
        }

        if (lower.startsWith("textures/particle/")) {
            return "assets/minecraft/$normalized"
        }

        if (lower.startsWith("textures/ui/")) {
            return "assets/minecraft/textures/gui/" + normalized.substring(BEDROCK_TEXTURE_UI_ROOT.length)
        }

        val customTexture = Regex("^textures/([^/]+)/(.+)$", RegexOption.IGNORE_CASE).find(normalized)
        if (customTexture != null && !customTexture.groupValues[1].equals("ui", ignoreCase = true)) {
            return "assets/${customTexture.groupValues[1].lowercase()}/textures/${customTexture.groupValues[2]}"
        }

        if (lower.startsWith("textures/")) {
            return "assets/minecraft/$normalized"
        }

        if (lower.startsWith("sounds/") && lower != "sounds/sound_definitions.json") {
            return "assets/minecraft/$normalized"
        }

        if (lower.startsWith("texts/") && lower.endsWith(".lang")) {
            val fileName = normalized.substringAfterLast('/')
            val localeCode = fileName.substringBeforeLast('.', fileName)
            val javaLocale = mapBedrockLocaleToJava(localeCode)
            return "assets/minecraft/lang/$javaLocale.json"
        }

        if (lower == "splashes.json") {
            return "assets/minecraft/texts/splashes.txt"
        }

        if (lower == "sounds/sound_definitions.json") {
            return "assets/minecraft/sounds.json"
        }

        return null
    }

    fun isJavaArmorTexture(relativePath: String): Boolean =
        JAVA_ARMOR_TEXTURE_REGEX.matches(relativePath.replace('\\', '/'))

    fun isBedrockArmorTexture(relativePath: String): Boolean =
        Regex("^textures/models/armor/[a-zA-Z0-9_]+_[12]\\.png$", RegexOption.IGNORE_CASE)
            .matches(relativePath.replace('\\', '/'))

    fun mapJavaLocaleToBedrock(locale: String): String {
        val parts = locale.lowercase().split("_")
        return if (parts.size == 2) {
            "${parts[0]}_${parts[1].uppercase()}"
        } else {
            locale
        }
    }

    fun mapBedrockLocaleToJava(locale: String): String {
        return locale.lowercase()
    }
}
