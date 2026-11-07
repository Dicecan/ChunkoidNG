package com.noches.chunkoidng.core.entity

object EntityGroupTable {

    data class Row(
        val adultGroups: List<String> = emptyList(),
        val babyGroups: List<String> = emptyList(),
        val extraGroups: List<String> = emptyList()
    ) {
        fun groups(isBaby: Boolean): List<String> {
            return if (isBaby) babyGroups else adultGroups
        }
    }

    private val ROWS = mutableMapOf<String, Row>()
    private val HEALTH = mutableMapOf<String, Float>()
    private val VARIANTS = mutableMapOf<String, Pair<String, Int>>()

    init {
        registerMobs()
    }

    private fun register(
        identifier: String,
        adult: String = "",
        baby: String = "",
        extra: String = "",
        health: Float = -1f
    ) {
        val adultList = if (adult.isEmpty()) emptyList() else adult.split(",")
        val babyList = if (baby.isEmpty()) emptyList() else baby.split(",")
        val extraList = if (extra.isEmpty()) emptyList() else extra.split(",")
        ROWS[identifier] = Row(adultList, babyList, extraList)
        if (health > 0f) {
            HEALTH[identifier] = health
        }
    }

    fun variant(identifier: String, group: String): Pair<String, Int>? {
        return VARIANTS["$identifier\u0000$group"]
    }

    fun of(identifier: String): Row? = ROWS[identifier]

    fun defaultHealth(identifier: String): Float = HEALTH[identifier] ?: 20f

    private fun registerMobs() {

        register("minecraft:sheep", adult = "sheep_adult", baby = "sheep_baby,baby", health = 8f)
        register("minecraft:cow", adult = "cow_adult", baby = "cow_baby,baby", health = 10f)
        register("minecraft:pig", adult = "pig_adult", baby = "pig_baby,baby", health = 10f)
        register("minecraft:chicken", adult = "chicken_adult", baby = "chicken_baby,baby", health = 4f)
        register("minecraft:horse", adult = "horse_adult", baby = "horse_baby,baby", health = 20f)
        register("minecraft:donkey", adult = "donkey_adult", baby = "donkey_baby,baby", health = 20f)
        register("minecraft:mule", adult = "mule_adult", baby = "mule_baby,baby", health = 20f)
        register("minecraft:cat", adult = "cat_adult", baby = "cat_baby,baby", health = 10f)
        register("minecraft:wolf", adult = "wolf_adult", baby = "wolf_baby,baby", health = 20f)
        register("minecraft:fox", adult = "fox_adult", baby = "fox_baby,baby", health = 10f)
        register("minecraft:rabbit", adult = "rabbit_adult", baby = "rabbit_baby,baby", health = 3f)
        register("minecraft:parrot", health = 6f)
        register("minecraft:llama", adult = "llama_adult", baby = "llama_baby,baby", health = 20f)
        register("minecraft:trader_llama", adult = "llama_adult", baby = "llama_baby,baby", health = 20f)
        register("minecraft:camel", adult = "camel_adult", baby = "camel_baby,baby", health = 32f)
        register("minecraft:goat", adult = "goat_adult", baby = "goat_baby,baby", health = 10f)
        register("minecraft:axolotl", adult = "axolotl_adult", baby = "axolotl_baby,baby", health = 14f)
        register("minecraft:bee", adult = "bee_adult", baby = "bee_baby,baby", health = 10f)
        register("minecraft:panda", adult = "panda_adult", baby = "panda_baby,baby", health = 20f)
        register("minecraft:polar_bear", adult = "polar_bear_adult", baby = "polar_bear_baby,baby", health = 30f)
        register("minecraft:turtle", adult = "turtle_adult", baby = "turtle_baby,baby", health = 30f)
        register("minecraft:frog", health = 10f)
        register("minecraft:tadpole", health = 6f)
        register("minecraft:armadillo", adult = "armadillo_adult", baby = "armadillo_baby,baby", health = 12f)
        register("minecraft:sniffer", adult = "sniffer_adult", baby = "sniffer_baby,baby", health = 14f)
        register("minecraft:allay", health = 20f)
        register("minecraft:bat", health = 6f)
        register("minecraft:squid", health = 10f)
        register("minecraft:glow_squid", health = 10f)
        register("minecraft:dolphin", health = 10f)
        register("minecraft:cod", health = 3f)
        register("minecraft:salmon", health = 3f)
        register("minecraft:pufferfish", health = 3f)
        register("minecraft:tropicalfish", health = 3f)
        register("minecraft:strider", adult = "strider_adult", baby = "strider_baby,baby", health = 20f)

        register("minecraft:zombie", adult = "zombie_adult", baby = "zombie_baby,baby", health = 20f)
        register("minecraft:husk", adult = "husk_adult", baby = "husk_baby,baby", health = 20f)
        register("minecraft:drowned", adult = "drowned_adult", baby = "drowned_baby,baby", health = 20f)
        register("minecraft:zombie_pigman", adult = "zombie_pigman_adult", baby = "zombie_pigman_baby,baby", health = 20f)
        register("minecraft:skeleton", health = 20f)
        register("minecraft:stray", health = 20f)
        register("minecraft:wither_skeleton", health = 20f)
        register("minecraft:bogged", health = 16f)
        register("minecraft:creeper", health = 20f)
        register("minecraft:spider", health = 16f)
        register("minecraft:cave_spider", health = 12f)
        register("minecraft:enderman", health = 40f)
        register("minecraft:slime", health = 16f)
        register("minecraft:magma_cube", health = 16f)
        register("minecraft:witch", health = 26f)
        register("minecraft:blaze", health = 20f)
        register("minecraft:ghast", health = 10f)
        register("minecraft:piglin", adult = "piglin_adult", baby = "piglin_baby,baby", health = 16f)
        register("minecraft:piglin_brute", health = 50f)
        register("minecraft:zoglin", adult = "zoglin_adult", baby = "zoglin_baby,baby", health = 40f)
        register("minecraft:hoglin", adult = "hoglin_adult", baby = "hoglin_baby,baby", health = 40f)
        register("minecraft:shulker", health = 30f)
        register("minecraft:silverfish", health = 8f)
        register("minecraft:endermite", health = 8f)
        register("minecraft:phantom", health = 20f)
        register("minecraft:ravager", health = 100f)
        register("minecraft:pillager", health = 24f)
        register("minecraft:vindicator", health = 24f)
        register("minecraft:evocation_illager", health = 24f)
        register("minecraft:vex", health = 14f)
        register("minecraft:guardian", health = 30f)
        register("minecraft:elder_guardian", health = 80f)
        register("minecraft:warden", health = 500f)
        register("minecraft:wither", health = 300f)
        register("minecraft:ender_dragon", health = 200f)
        register("minecraft:breeze", health = 30f)

        register("minecraft:villager_v2", adult = "villager_adult", baby = "villager_baby,baby", health = 20f)
        register("minecraft:zombie_villager_v2", adult = "zombie_villager_adult", baby = "zombie_villager_baby,baby", health = 20f)
        register("minecraft:wandering_trader", health = 20f)
        register("minecraft:iron_golem", health = 100f)
        register("minecraft:snow_golem", health = 4f)

        register("minecraft:armor_stand", health = 6f)
        register("minecraft:boat")
        register("minecraft:chest_boat")
        register("minecraft:minecart")
        register("minecraft:chest_minecart")
        register("minecraft:hopper_minecart")
        register("minecraft:furnace_minecart")
        register("minecraft:tnt_minecart")
        register("minecraft:command_block_minecart")

        val dyeColours = listOf(
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
        )
        for (i in dyeColours.indices) {
            VARIANTS["minecraft:sheep\u0000sheep_${dyeColours[i]}"] = Pair("Color", i)
        }
    }
}
