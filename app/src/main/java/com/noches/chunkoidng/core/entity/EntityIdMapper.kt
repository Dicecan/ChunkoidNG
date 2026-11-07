package com.noches.chunkoidng.core.entity

object EntityIdMapper {

    private val BEDROCK_RENAME_MAP = mapOf(
        "minecraft:zombified_piglin" to "minecraft:zombie_pigman",
        "minecraft:evoker" to "minecraft:evocation_illager",
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
        "cherry", "bamboo", "pale_oak"
    )

    private val VANILLA_WHITELIST = setOf(
        "allay", "armadillo", "armor_stand", "arrow", "axolotl", "bat", "bee", "blaze", "boat",
        "bogged", "breeze", "breeze_wind_charge", "camel", "cat", "cave_spider", "chest_boat",
        "chest_minecart", "chicken", "cod", "command_block_minecart", "cow", "creeper",
        "dolphin", "donkey", "dragon_fireball", "drowned", "egg", "elder_guardian",
        "end_crystal", "ender_dragon", "ender_pearl", "enderman", "endermite", "evoker",
        "evoker_fangs", "experience_bottle", "experience_orb", "eye_of_ender", "falling_block",
        "fireball", "firework_rocket", "fishing_bobber", "fox", "frog", "furnace_minecart",
        "ghast", "giant", "glow_item_frame", "glow_squid", "goat", "guardian", "hoglin",
        "hopper_minecart", "horse", "husk", "illusioner", "interaction", "iron_golem", "item",
        "item_display", "item_frame", "leash_knot", "lightning_bolt", "llama", "llama_spit",
        "magma_cube", "marker", "minecart", "mooshroom", "mule", "ocelot", "ominous_item_spawner",
        "painting", "panda", "parrot", "phantom", "pig", "piglin", "piglin_brute", "pillager",
        "polar_bear", "potion", "pufferfish", "rabbit", "ravager", "salmon", "sheep",
        "shulker", "shulker_bullet", "silverfish", "skeleton", "skeleton_horse", "slime",
        "small_fireball", "sniffer", "snow_golem", "snowball", "spawner_minecart", "spectral_arrow",
        "spider", "squid", "stray", "strider", "tadpole", "text_display", "tnt", "tnt_minecart",
        "trader_llama", "trident", "tropical_fish", "turtle", "vex", "villager", "vindicator",
        "wandering_trader", "warden", "wind_charge", "witch", "wither", "wither_skeleton",
        "wither_skull", "wolf", "zoglin", "zombie", "zombie_horse", "zombie_villager",
        "zombified_piglin"
    )

    private val JAVA_REVERSE_MAP = BEDROCK_RENAME_MAP.entries.associate { (k, v) -> v to k } + mapOf(
        "minecraft:lingering_potion" to "minecraft:potion"
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

    fun isHandledByChunkerNative(id: String?): Boolean {
        if (id == null) return false
        val clean = id.removePrefix("minecraft:")
        return clean in setOf("painting", "item_frame", "glow_item_frame")
    }

    fun toJavaId(bedrockId: String?, variant: Int? = null): String? {
        if (bedrockId == null) return null
        val normalized = if (bedrockId.contains(":")) bedrockId else "minecraft:$bedrockId"

        if (normalized == "minecraft:boat") {
            val v = variant ?: 0
            val wood = if (v in BOAT_WOODS.indices) BOAT_WOODS[v] else "oak"
            return "minecraft:${wood}_boat"
        }
        if (normalized == "minecraft:chest_boat") {
            val v = variant ?: 0
            val wood = if (v in BOAT_WOODS.indices) BOAT_WOODS[v] else "oak"
            return "minecraft:${wood}_chest_boat"
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
        for (wood in BOAT_WOODS) {
            if (clean == "${wood}_boat") return "minecraft:boat"
            if (clean == "${wood}_chest_boat") return "minecraft:chest_boat"
        }

        return BEDROCK_RENAME_MAP[normalized] ?: normalized
    }
}
