package com.noches.chunkoidng.core.entity

object EntityIdMapper {

    private val BEDROCK_RENAME_MAP = mapOf(
        "minecraft:zombified_piglin" to "minecraft:zombie_pigman",
        "minecraft:evoker" to "minecraft:evocation_illager",
        "minecraft:evoker_fangs" to "minecraft:evocation_fang",
        "minecraft:breeze_wind_charge" to "minecraft:breeze_wind_charge_projectile",
        "minecraft:wind_charge" to "minecraft:wind_charge_projectile",
        "minecraft:experience_orb" to "minecraft:xp_orb",
        "minecraft:experience_bottle" to "minecraft:xp_bottle",
        "minecraft:eye_of_ender" to "minecraft:eye_of_ender_signal",
        "minecraft:firework_rocket" to "minecraft:fireworks_rocket",
        "minecraft:fishing_bobber" to "minecraft:fishing_hook",
        "minecraft:trident" to "minecraft:thrown_trident",
        "minecraft:potion" to "minecraft:splash_potion",
        "minecraft:tropical_fish" to "minecraft:tropicalfish",
        "minecraft:end_crystal" to "minecraft:ender_crystal",
        "minecraft:villager" to "minecraft:villager_v2",
        "minecraft:zombie_villager" to "minecraft:zombie_villager_v2",
        "minecraft:snow_golem" to "minecraft:snow_golem",
        "minecraft:leash_knot" to "minecraft:leash_knot"
    )

    private val BOAT_WOODS = listOf(
        "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove",
        "bamboo", "cherry", "pale_oak", "poplar"
    )

    private val VANILLA_WHITELIST = setOf(
        "allay", "area_effect_cloud", "armadillo", "armor_stand", "arrow", "axolotl", "bamboo_chest_raft",
        "bamboo_raft", "bat", "bee", "blaze", "block_display", "boat", "bogged", "breeze", "breeze_wind_charge", "camel",
        "camel_husk", "cat", "cave_spider", "chest_boat", "chest_minecart", "chicken", "cod",
        "command_block_minecart", "copper_golem", "cow", "creaking", "creeper", "cushion", "dolphin", "donkey",
        "dragon_fireball", "drowned", "egg", "elder_guardian", "end_crystal", "ender_dragon",
        "ender_pearl", "enderman", "endermite", "evoker", "evoker_fangs", "experience_bottle",
        "experience_orb", "eye_of_ender", "falling_block", "fireball", "firework_rocket",
        "fishing_bobber", "fox", "frog", "furnace_minecart", "ghast", "giant", "glow_item_frame",
        "glow_squid", "goat", "guardian", "happy_ghast", "hoglin", "hopper_minecart", "horse", "husk",
        "illusioner", "interaction", "iron_golem", "item", "item_display", "item_frame", "leash_knot",
        "lightning_bolt", "lingering_potion", "llama", "llama_spit", "magma_cube", "mannequin", "marker",
        "minecart", "mooshroom", "mule", "nautilus", "ocelot", "ominous_item_spawner", "painting",
        "panda", "parched", "parrot", "phantom", "pig", "piglin", "piglin_brute", "pillager",
        "polar_bear", "poplar_boat", "poplar_chest_boat", "potion", "pufferfish", "rabbit", "ravager",
        "salmon", "sheep", "shulker", "shulker_bullet", "silverfish", "skeleton", "skeleton_horse",
        "slime", "small_fireball", "sniffer", "snow_golem", "snowball", "spawner_minecart",
        "spectral_arrow", "spider", "splash_potion", "squid", "stray", "strider", "sulfur_cube",
        "tadpole", "text_display", "tnt", "tnt_minecart", "trader_llama", "trident", "tropical_fish",
        "turtle", "vex", "villager", "vindicator", "wandering_trader", "warden", "wind_charge",
        "witch", "wither", "wither_skeleton", "wither_skull", "wolf", "zoglin", "zombie",
        "zombie_horse", "zombie_nautilus", "zombie_villager", "zombified_piglin"
    )

    private val JAVA_REVERSE_MAP = BEDROCK_RENAME_MAP.entries.associate { (k, v) -> v to k } + mapOf(
        "minecraft:lingering_potion" to "minecraft:potion",
        "minecraft:wither_skull_dangerous" to "minecraft:wither_skull"
    )

    fun isVanilla(javaId: String?): Boolean {
        if (javaId == null) return false
        val clean = javaId.removePrefix("minecraft:")
        if (clean in VANILLA_WHITELIST) return true
        for (wood in BOAT_WOODS) {
            if (clean == "${wood}_boat" || clean == "${wood}_chest_boat") return true
        }
        return false
    }

    fun isHandledByChunkerNative(id: String?, includeBedrockExclusiveNative: Boolean = false): Boolean {
        if (id == null) return false
        val clean = id.removePrefix("minecraft:")
        if (clean in setOf("painting", "item_frame", "glow_item_frame")) return true
        return includeBedrockExclusiveNative && clean == "cushion"
    }

    fun toJavaId(bedrockId: String?, variant: Int? = null, modernPotionIds: Boolean = true): String? {
        if (bedrockId == null) return null
        val normalized = if (bedrockId.contains(":")) bedrockId else "minecraft:$bedrockId"

        if (normalized == "minecraft:boat") {
            val v = variant ?: 0
            if (v == 7) return "minecraft:bamboo_raft"
            val wood = if (v in BOAT_WOODS.indices) BOAT_WOODS[v] else "oak"
            return "minecraft:${wood}_boat"
        }
        if (normalized == "minecraft:chest_boat") {
            val v = variant ?: 0
            if (v == 7) return "minecraft:bamboo_chest_raft"
            val wood = if (v in BOAT_WOODS.indices) BOAT_WOODS[v] else "oak"
            return "minecraft:${wood}_chest_boat"
        }
        if (normalized == "minecraft:splash_potion") {
            return if (modernPotionIds) "minecraft:splash_potion" else "minecraft:potion"
        }
        if (normalized == "minecraft:lingering_potion") {
            return if (modernPotionIds) "minecraft:lingering_potion" else "minecraft:potion"
        }

        val mapped = JAVA_REVERSE_MAP[normalized] ?: normalized
        val clean = mapped.removePrefix("minecraft:")
        if (clean in VANILLA_WHITELIST) {
            return mapped
        }
        for (wood in BOAT_WOODS) {
            if (clean == "${wood}_boat" || clean == "${wood}_chest_boat") return mapped
        }
        return null
    }

    fun toBedrock(javaId: String?): String? {
        if (javaId == null) return null
        val normalized = if (javaId.startsWith("minecraft:")) javaId else "minecraft:$javaId"
        if (!isVanilla(normalized)) {
            return null
        }

        val clean = normalized.removePrefix("minecraft:")
        if (clean == "bamboo_raft") return "minecraft:boat"
        if (clean == "bamboo_chest_raft") return "minecraft:chest_boat"
        for (wood in BOAT_WOODS) {
            if (clean == "${wood}_boat") return "minecraft:boat"
            if (clean == "${wood}_chest_boat") return "minecraft:chest_boat"
        }

        return BEDROCK_RENAME_MAP[normalized] ?: normalized
    }
}
