package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.NbtByte
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtFloat
import br.com.gamemods.nbtmanipulator.NbtInt
import br.com.gamemods.nbtmanipulator.NbtString

object EntityGroupTable {

    data class Row(
        val adultGroups: List<String> = emptyList(),
        val babyGroups: List<String> = emptyList(),
        val extraGroups: List<String> = emptyList(),
        val propertyEntries: List<String> = emptyList()
    ) {
        fun groups(isBaby: Boolean): List<String> {
            return if (isBaby) babyGroups else adultGroups
        }

        fun hasGroup(group: String): Boolean {
            return group in extraGroups || group in adultGroups || group in babyGroups
        }

        fun hasProperties(): Boolean {
            return propertyEntries.isNotEmpty()
        }

        fun properties(): NbtCompound {
            val tag = NbtCompound()
            for (entry in propertyEntries) {
                val parts = entry.split("|", limit = 3)
                if (parts.size < 3) continue
                val key = parts[0]
                val type = parts[1]
                val raw = parts[2]
                when (type) {
                    "bool" -> tag[key] = NbtByte(if (raw.equals("true", ignoreCase = true)) 1 else 0)
                    "int" -> tag[key] = NbtInt(raw.toIntOrNull() ?: 0)
                    "float" -> tag[key] = NbtFloat(raw.toFloatOrNull() ?: 0f)
                    else -> tag[key] = NbtString(raw)
                }
            }
            return tag
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
        properties: String = "",
        health: Float = -1f
    ) {
        val adultList = if (adult.isEmpty()) emptyList() else adult.split(",")
        val babyList = if (baby.isEmpty()) emptyList() else baby.split(",")
        val extraList = if (extra.isEmpty()) emptyList() else extra.split(",")
        val propList = if (properties.isEmpty()) emptyList() else properties.split(";")
        ROWS[identifier] = Row(adultList, babyList, extraList, propList)
        if (health > 0f) {
            HEALTH[identifier] = health
        }
    }

    private fun variant(identifier: String, group: String, component: String, value: Int) {
        val actorKey = when (component) {
            "color" -> "Color"
            "mark_variant" -> "MarkVariant"
            else -> "Variant"
        }
        VARIANTS["$identifier\u0000$group"] = Pair(actorKey, value)
    }

    fun variant(identifier: String, group: String): Pair<String, Int>? {
        return VARIANTS["$identifier\u0000$group"]
    }

    fun of(identifier: String): Row? = ROWS[identifier]

    fun hasHealth(identifier: String): Boolean = HEALTH.containsKey(identifier)

    fun healthOrNull(identifier: String): Float? = HEALTH[identifier]

    fun defaultHealth(identifier: String): Float = HEALTH[identifier] ?: 20f

    private fun registerMobs() {
        register("minecraft:agent")
        register("minecraft:allay", extra = "pickup_item,pickup_item_delay", health = 20f)
        register("minecraft:area_effect_cloud")
        register(
            "minecraft:armadillo",
            adult = "adult,adult_unrolled,scanning,unrolled",
            baby = "baby,baby_unrolled,scanning,unrolled",
            extra = "rolled_up,rolled_up_with_threats,rolled_up_without_threats",
            properties = "minecraft:armadillo_state|enum|unrolled",
            health = 12f
        )
        register("minecraft:armor_stand", health = 6f)
        register("minecraft:arrow", extra = "mob_arrow,mob_crossbow_arrow,player_arrow,player_crossbow_arrow")
        register(
            "minecraft:axolotl",
            extra = "attack_cooldown,axolotl_adult,axolotl_baby,axolotl_blue,axolotl_cyan,axolotl_dried,axolotl_gold,axolotl_in_water,axolotl_lucy,axolotl_on_land,axolotl_on_land_in_rain,axolotl_wild",
            health = 14f
        )
        register("minecraft:bat", health = 6f)
        register(
            "minecraft:bee",
            adult = "bee_adult,look_for_food,shelter_detection,track_attacker",
            extra = "abort_shelter_detection,add_poison_effect,add_wither_effect,angry_bee,bee_baby,countdown_to_perish,default_sound,easy_attack,escape_fire,find_hive,hard_attack,has_nectar,hive_full,normal_attack,perish,return_to_home,take_nearest_target",
            health = 10f
        )
        register("minecraft:blaze", extra = "melee_mode,mode_switcher,ranged_mode", health = 20f)
        register("minecraft:boat", extra = "above_bubble_column_down,above_bubble_column_up,can_ride_bamboo,can_ride_default,floating")
        register("minecraft:bogged", extra = "bogged_sheared,melee_attack,ranged_attack,ranged_attack_hard", health = 16f)
        register("minecraft:breeze", properties = "minecraft:is_playing_idle_ground_sound|bool|False", health = 30f)
        register("minecraft:breeze_wind_charge_projectile")
        register(
            "minecraft:camel",
            adult = "camel_adult,camel_standing",
            extra = "camel_baby,camel_baby_sitting,camel_baby_standing,camel_saddled,camel_sitting",
            health = 32f
        )
        register(
            "minecraft:camel_husk",
            extra = "camel_husk_saddled,camel_husk_sitting,camel_husk_standing,camel_husk_with_hostile_rider,camel_husk_with_no_hostile_rider",
            properties = "minecraft:has_rider_mounted|bool|False",
            health = 32f
        )
        register(
            "minecraft:cat",
            extra = "cat_adult,cat_baby,cat_black,cat_british,cat_calico,cat_gift_for_owner,cat_jellie,cat_persian,cat_ragdoll,cat_red,cat_siamese,cat_tabby,cat_tame,cat_tuxedo,cat_white,cat_wild",
            properties = "minecraft:sound_variant|enum|default",
            health = 10f
        )
        register(
            "minecraft:cave_spider",
            extra = "spider_angry,spider_bogged_jockey,spider_hostile,spider_jockey,spider_neutral,spider_parched_jockey,spider_poison_easy,spider_poison_hard,spider_poison_normal,spider_stray_jockey,spider_wither_jockey",
            health = 12f
        )
        register("minecraft:chest_boat", extra = "above_bubble_column_down,above_bubble_column_up,can_ride_bamboo,can_ride_default,floating")
        register("minecraft:chest_minecart")
        register(
            "minecraft:chicken",
            adult = "chicken_adult",
            extra = "chicken_baby",
            properties = "minecraft:climate_variant|enum|temperate;minecraft:sound_variant|enum|default",
            health = 4f
        )
        register("minecraft:cod", health = 3f)
        register("minecraft:command_block_minecart", extra = "command_block_active,command_block_inactive")
        register(
            "minecraft:copper_golem",
            extra = "became_statue,becoming_statue,copper_oxidizing",
            properties = "minecraft:oxidation_level|enum|unoxidized;minecraft:is_waxed|bool|False;minecraft:chest_interaction|enum|none;minecraft:has_flower|bool|False;minecraft:is_becoming_statue|bool|False",
            health = 12f
        )
        register(
            "minecraft:cow",
            adult = "cow_adult",
            extra = "cow_baby",
            properties = "minecraft:climate_variant|enum|temperate;minecraft:sound_variant|enum|default",
            health = 10f
        )
        register(
            "minecraft:creaking",
            extra = "crumbling,hostile,hostile_unobserved,immobile,mobile,neutral,spawned_by_creaking_heart,spawned_by_player,twitching",
            properties = "minecraft:creaking_swaying_ticks|int|0;minecraft:creaking_state|enum|neutral",
            health = 1f
        )
        register("minecraft:creeper", extra = "charged_creeper,charged_exploding,exploding,forced_charged_exploding,forced_exploding", health = 20f)
        register("minecraft:cushion")
        register(
            "minecraft:dolphin",
            extra = "dolphin_adult,dolphin_angry,dolphin_baby,dolphin_dried,dolphin_on_land,dolphin_on_land_in_rain,dolphin_swimming_navigation",
            health = 10f
        )
        register(
            "minecraft:donkey",
            adult = "donkey_adult,donkey_wild",
            extra = "donkey_baby,donkey_chested,donkey_saddled,donkey_tamed,donkey_unchested",
            health = 20f
        )
        register("minecraft:dragon_fireball")
        register(
            "minecraft:drowned",
            extra = "adult_drowned,baby_drowned,can_break_doors,drowned_jockey,drowned_rider,hunter_mode,melee_equipment,melee_mode,mode_switcher,ranged_equipment,ranged_mode,wander_mode",
            health = 20f
        )
        register("minecraft:egg", properties = "minecraft:climate_variant|enum|temperate")
        register("minecraft:elder_guardian", health = 80f)
        register("minecraft:ender_crystal", extra = "crystal_exploding", health = 1f)
        register("minecraft:ender_dragon", extra = "dragon_death,dragon_flying,dragon_sitting", health = 200f)
        register("minecraft:ender_pearl", extra = "no_spawn")
        register("minecraft:enderman", extra = "enderman_angry,enderman_calm,not_riding,riding", health = 40f)
        register("minecraft:endermite", health = 8f)
        register("minecraft:evocation_illager", extra = "celebrate,raid_configuration,raid_despawn,raid_persistence", health = 24f)
        register("minecraft:eye_of_ender_signal")
        register("minecraft:fireball", extra = "exploding")
        register("minecraft:fireworks_rocket")
        register("minecraft:fishing_hook", extra = "loot_jungle")
        register(
            "minecraft:fox",
            extra = "defending_fox,docile_fox,fox_adult,fox_ambient_defending_target,fox_ambient_night,fox_ambient_normal,fox_ambient_sleep,fox_arctic,fox_baby,fox_day,fox_night,fox_red,fox_thunderstorm,fox_with_item,trusting_fox",
            health = 10f
        )
        register("minecraft:frog", extra = "cold_frog,pregnant,temperate_frog,warm_frog", health = 10f)
        register("minecraft:ghast", health = 10f)
        register("minecraft:glow_squid", extra = "squid_adult,squid_baby", health = 10f)
        register(
            "minecraft:goat",
            extra = "attack_cooldown,goat_adult,goat_baby,goat_default,goat_screamer,interact_default,interact_screamer,ram_default,ram_screamer",
            health = 10f
        )
        register("minecraft:guardian", extra = "guardian_aggressive,guardian_passive", health = 30f)
        register(
            "minecraft:happy_ghast",
            adult = "adult,adult_mobile,adult_unharnessed,adult_without_passengers",
            baby = "baby",
            extra = "adult_harnessed,adult_immobile,adult_with_passengers",
            properties = "minecraft:can_move|bool|True",
            health = 20f
        )
        register(
            "minecraft:hoglin",
            adult = "hoglin_adult,huntable_adult,zombification_sensor",
            baby = "hoglin_baby,zombification_sensor",
            extra = "angry_hoglin,attack_cooldown,become_zombie,start_zombification,unhuntable_adult",
            health = 40f
        )
        register("minecraft:hopper_minecart", extra = "hopper_active,hopper_inactive")
        register(
            "minecraft:horse",
            adult = "horse_adult,horse_wild",
            extra = "base_black,base_brown,base_chestnut,base_creamy,base_darkbrown,base_gray,base_white,horse_baby,horse_saddled,horse_tamed,markings_black_dots,markings_none,markings_white_details,markings_white_dots,markings_white_fields",
            health = 20f
        )
        register(
            "minecraft:husk",
            extra = "can_break_doors,convert_to_baby_zombie,convert_to_zombie,look_to_start_transformations,not_on_camel_husk,on_camel_husk,start_zombie_transformation,zombie_husk_adult,zombie_husk_baby,zombie_husk_jockey,zombie_husk_rider",
            properties = "minecraft:is_riding_camel_husk|bool|False",
            health = 20f
        )
        register("minecraft:iron_golem", extra = "player_created,village_created", health = 100f)
        register("minecraft:lightning_bolt")
        register("minecraft:lingering_potion")
        register(
            "minecraft:llama",
            adult = "llama_adult,llama_brown,llama_creamy,llama_gray,llama_unleashed,llama_white,llama_wild,strength_1,strength_2,strength_3,strength_4,strength_5",
            baby = "llama_baby,llama_brown,llama_creamy,llama_gray,llama_unleashed,llama_white,strength_1,strength_2,strength_3,strength_4,strength_5",
            extra = "in_caravan,llama_angry,llama_angry_wolf,llama_chested,llama_tamed,llama_unchested",
            health = 20f
        )
        register("minecraft:llama_spit")
        register("minecraft:magma_cube", extra = "slime_aggressive,slime_calm,slime_large,slime_medium,slime_small", health = 16f)
        register("minecraft:minecart")
        register(
            "minecraft:mooshroom",
            extra = "cow_adult,cow_baby,mooshroom_become_cow,mooshroom_brown,mooshroom_brown_fed_allium,mooshroom_brown_fed_azure_bluet,mooshroom_brown_fed_blue_orchid,mooshroom_brown_fed_closed_eyeblossom,mooshroom_brown_fed_cornflower,mooshroom_brown_fed_dandelion,mooshroom_brown_fed_lily_of_the_valley,mooshroom_brown_fed_open_eyeblossom,mooshroom_brown_fed_oxeye_daisy,mooshroom_brown_fed_poppy,mooshroom_brown_fed_torchflower,mooshroom_brown_fed_tulips,mooshroom_brown_fed_wither_rose,mooshroom_fed_nothing,mooshroom_red",
            health = 10f
        )
        register(
            "minecraft:mule",
            adult = "mule_adult,mule_wild",
            extra = "mule_baby,mule_chested,mule_saddled,mule_tamed,mule_unchested",
            health = 20f
        )
        register(
            "minecraft:nautilus",
            extra = "nautilus_adult,nautilus_ai_controlled,nautilus_baby,nautilus_leashable,nautilus_player_controlled,nautilus_tame,nautilus_tame_adult,nautilus_tame_saddled,nautilus_tame_saddled_in_water,nautilus_tame_saddled_on_ground,nautilus_tame_unsaddled,nautilus_wild_adult_angry,nautilus_wild_adult_calm",
            health = 15f
        )
        register("minecraft:npc")
        register(
            "minecraft:ocelot",
            extra = "ocelot_adult,ocelot_baby,ocelot_tame,ocelot_trusting,ocelot_wild,wild_child_ocelot_spawn",
            health = 10f
        )
        register("minecraft:ominous_item_spawner")
        register(
            "minecraft:panda",
            extra = "baby_scared,panda_adult,panda_aggressive,panda_angry,panda_baby,panda_brown,panda_lazy,panda_playful,panda_sneezing,panda_weak,panda_worried",
            health = 20f
        )
        register("minecraft:parched", extra = "melee_attack,ranged_attack,ranged_attack_hard", health = 16f)
        register(
            "minecraft:parrot",
            extra = "parrot_adult,parrot_blue,parrot_cyan,parrot_green,parrot_not_riding_player,parrot_red,parrot_riding_player,parrot_silver,parrot_tame,parrot_wild",
            health = 6f
        )
        register("minecraft:phantom", health = 20f)
        register(
            "minecraft:pig",
            adult = "pig_adult,pig_unsaddled",
            extra = "pig_baby,pig_saddled,pig_transform",
            properties = "minecraft:climate_variant|enum|temperate;minecraft:sound_variant|enum|default",
            health = 10f
        )
        register(
            "minecraft:piglin",
            adult = "alert_for_attack_targets,attack_cooldown,hunter,interactable_piglin,melee_unit,piglin_adult,ranged_unit,zombification_sensor",
            baby = "piglin_baby,piglin_jockey,zombification_sensor",
            extra = "angry,become_zombie,not_hunter,start_zombification,take_target_as_response_to_block_break",
            health = 16f
        )
        register(
            "minecraft:piglin_brute",
            extra = "alert_for_attack_targets,angry,become_zombie,go_back_to_spawn,melee_unit,start_zombification,take_target_as_response_to_block_break,zombification_sensor",
            health = 50f
        )
        register(
            "minecraft:pillager",
            extra = "celebrate,illager_squad_captain,melee_attack,patrol_captain,patrol_follower,raid_configuration,raid_persistence,ranged_attack",
            health = 24f
        )
        register("minecraft:player", extra = "add_raid_omen,clear_raid_omen_spell_effect,raid_trigger")
        register("minecraft:polar_bear", extra = "adult,adult_hostile,adult_wild,baby,baby_scared,baby_wild", health = 30f)
        register("minecraft:pufferfish", extra = "deflate_sensor,deflate_sensor_buffer,full_puff,half_puff_primary,half_puff_secondary,normal_puff,start_deflate", health = 3f)
        register("minecraft:rabbit", extra = "adult,baby,coat_black,coat_brown,coat_desert,coat_salt,coat_splotched,coat_white", health = 3f)
        register(
            "minecraft:ravager",
            extra = "celebrate,evoker_rider_for_raid,hostile,pillager_captain_rider,pillager_rider,pillager_rider_for_raid,raid_configuration,raid_persistence,vindicator_captain_rider,vindicator_rider,roaring,stunned",
            health = 100f
        )
        register("minecraft:salmon", extra = "scale_large,scale_normal,scale_small", health = 3f)
        register(
            "minecraft:sheep",
            adult = "loot_wooly,rideable_wooly,sheep_adult,sheep_dyeable",
            baby = "sheep_baby,sheep_dyeable",
            extra = "loot_sheared,rideable_sheared,sheep_black,sheep_blue,sheep_brown,sheep_cyan,sheep_gray,sheep_light_blue,sheep_light_gray,sheep_orange,sheep_pink,sheep_red,sheep_sheared,sheep_white,sheep_yellow",
            health = 8f
        )
        register(
            "minecraft:shulker",
            extra = "shulker_black,shulker_blue,shulker_brown,shulker_cyan,shulker_gray,shulker_green,shulker_light_blue,shulker_lime,shulker_magenta,shulker_orange,shulker_pink,shulker_purple,shulker_red,shulker_silver,shulker_undyed,shulker_white,shulker_yellow",
            health = 30f
        )
        register("minecraft:shulker_bullet")
        register("minecraft:silverfish", extra = "silverfish_angry,silverfish_calm", health = 8f)
        register("minecraft:skeleton", extra = "become_stray,got_out_of_powder_snow_environment_sensor,in_powder_snow,lightning_immune,melee_attack,ranged_attack,ranged_attack_hard", health = 20f)
        register("minecraft:skeleton_horse", extra = "lightning_immune,skeleton_horse_adult,skeleton_horse_baby,skeleton_horse_r5_upgrade,skeleton_trap", health = 15f)
        register("minecraft:slime", extra = "slime_aggressive,slime_calm,slime_large,slime_medium,slime_small", health = 16f)
        register("minecraft:small_fireball")
        register("minecraft:sniffer", adult = "pushable,sniffer_adult", extra = "feeling_happy,sniffer_baby,sniffer_pregnant,sniffer_search_and_dig,stand_up", health = 14f)
        register("minecraft:snow_golem", extra = "snowman_sheared", health = 4f)
        register("minecraft:snowball")
        register("minecraft:spider", extra = "spider_angry,spider_bogged_jockey,spider_hostile,spider_jockey,spider_neutral,spider_parched_jockey,spider_stray_jockey,spider_wither_jockey", health = 16f)
        register("minecraft:splash_potion")
        register("minecraft:squid", extra = "squid_adult,squid_baby", health = 10f)
        register("minecraft:stray", extra = "melee_attack,ranged_attack,ranged_attack_hard,revert_to_skeleton", health = 20f)
        register(
            "minecraft:strider",
            adult = "detect_suffocating,strider_adult,strider_pathing_behaviors,strider_unsaddled",
            baby = "detect_suffocating,strider_baby,strider_pathing_behaviors",
            extra = "start_suffocating,strider_parent_jockey,strider_piglin_jockey,strider_saddled",
            health = 20f
        )
        register(
            "minecraft:sulfur_cube",
            extra = "sulfur_cube_ai,sulfur_cube_bouncy,sulfur_cube_explosive,sulfur_cube_fast_flat,sulfur_cube_fast_sliding,sulfur_cube_high_resistance,sulfur_cube_hot,sulfur_cube_light,sulfur_cube_medium,sulfur_cube_medium_primed,sulfur_cube_medium_primed_by_explosion,sulfur_cube_medium_with_block,sulfur_cube_medium_with_block_interactable,sulfur_cube_medium_without_block,sulfur_cube_medium_without_block_can_pickup,sulfur_cube_medium_without_block_pickup_timeout,sulfur_cube_regular,sulfur_cube_slow_bouncy,sulfur_cube_slow_flat,sulfur_cube_slow_sliding,sulfur_cube_small,sulfur_cube_sticky,sulfur_cube_with_target,sulfur_cube_without_target",
            properties = "minecraft:sulfur_cube_archetype|enum|none",
            health = 16f
        )
        register("minecraft:tadpole", extra = "grow_up", health = 6f)
        register("minecraft:thrown_trident")
        register("minecraft:tnt", extra = "from_explosion")
        register("minecraft:tnt_minecart", extra = "inactive,instant_explode_tnt,primed_tnt")
        register(
            "minecraft:trader_llama",
            adult = "llama_adult,llama_brown,llama_creamy,llama_gray,llama_unleashed,llama_white,llama_wild,strength_1,strength_2,strength_3,strength_4,strength_5",
            baby = "llama_baby,llama_brown,llama_creamy,llama_gray,llama_unleashed,llama_white,strength_1,strength_2,strength_3,strength_4,strength_5",
            extra = "in_caravan,llama_angry,llama_angry_wolf,llama_chested,llama_defend_trader,llama_persistence,llama_tamed,llama_unchested,llama_wandering_trader",
            health = 20f
        )
        register("minecraft:tripod_camera", health = 4f)
        register(
            "minecraft:tropicalfish",
            extra = "anenonme,black_tang,blue_dory,butterfly_fish,cc_betta,cichlid,clownfish,dog_fish,e_red_snapper,goat_fish,moorish_idol,ornate_butterfly,parrot_fish,queen_angel_fish,red_cichlid,red_lipped_benny,red_snapper,threadfin,tomato_clown,triggerfish,tropicalfish_base_blue,tropicalfish_base_brown,tropicalfish_base_cyan,tropicalfish_base_gray,tropicalfish_base_green,tropicalfish_base_lightblue,tropicalfish_base_lightgreen,tropicalfish_base_magenta,tropicalfish_base_orange,tropicalfish_base_pink,tropicalfish_base_purple,tropicalfish_base_red,tropicalfish_base_silver,tropicalfish_base_white,tropicalfish_base_yellow,tropicalfish_pattern_blue,tropicalfish_pattern_brown,tropicalfish_pattern_cyan,tropicalfish_pattern_gray,tropicalfish_pattern_green,tropicalfish_pattern_lightblue,tropicalfish_pattern_lightgreen,tropicalfish_pattern_magenta,tropicalfish_pattern_orange,tropicalfish_pattern_pink,tropicalfish_pattern_purple,tropicalfish_pattern_red,tropicalfish_pattern_silver,tropicalfish_pattern_white,tropicalfish_pattern_yellow,tropicalfish_variant_a,tropicalfish_variant_b,tropicalfish_variant_pattern_1,tropicalfish_variant_pattern_2,tropicalfish_variant_pattern_3,tropicalfish_variant_pattern_4,tropicalfish_variant_pattern_5,tropicalfish_variant_pattern_6,yellow_tail_parrot,yellow_tang",
            health = 3f
        )
        register("minecraft:turtle", extra = "adult,baby,pregnant,wants_to_lay_egg", health = 30f)
        register("minecraft:vex", extra = "periodic_damage,start_damage_timer", health = 14f)
        register(
            "minecraft:villager",
            extra = "adult,armorer,baby,become_villager_v2,become_witch,become_zombie,behavior_non_peasant,behavior_peasant,butcher,cartographer,cleric,farmer,fisherman,fletcher,leatherworker,librarian,celebrate,shepherd,toolsmith,weaponsmith",
            health = 20f
        )
        register(
            "minecraft:villager_v2",
            extra = "adult,armorer,baby,basic_schedule,become_witch,become_zombie,bed_schedule_villager,behavior_non_peasant,behavior_peasant,butcher,cartographer,child_schedule,cleric,desert_villager,farmer,farmer_schedule,fisher_schedule,fisherman,fletcher,gather_schedule_villager,home_schedule_villager,job_specific_goals,jobless_schedule,jungle_villager,leatherworker,librarian,librarian_schedule,make_and_receive_love,mason,celebrate,nitwit,play_schedule_villager,savanna_villager,shepherd,snow_villager,swamp_villager,taiga_villager,toolsmith,trade_components,trade_resupply_component_group,unskilled,villager_skin_0,villager_skin_1,villager_skin_2,villager_skin_3,villager_skin_4,villager_skin_5,wander_schedule_villager,weaponsmith,work_schedule,work_schedule_farmer,work_schedule_fisher,work_schedule_librarian,work_schedule_villager",
            health = 20f
        )
        register(
            "minecraft:vindicator",
            extra = "celebrate,default_targeting,illager_squad_captain,patrol_captain,patrol_follower,raid_configuration,raid_despawn,raid_persistence,vindicator_aggro,vindicator_johnny",
            health = 24f
        )
        register("minecraft:wandering_trader", extra = "despawning,managed,scared", health = 20f)
        register("minecraft:warden", extra = "emerging,pushable", health = 500f)
        register("minecraft:wind_charge_projectile")
        register("minecraft:witch", extra = "celebrate,raid_configuration,raid_persistence", health = 26f)
        register("minecraft:wither", health = 600f)
        register("minecraft:wither_skeleton", health = 20f)
        register("minecraft:wither_skull", extra = "exploding")
        register("minecraft:wither_skull_dangerous", extra = "exploding")
        register(
            "minecraft:wolf",
            extra = "on_tame_collar_color,wolf_adult,wolf_angry,wolf_armorable,wolf_ashen,wolf_baby,wolf_black,wolf_chestnut,wolf_increased_max_health,wolf_leashable,wolf_pale,wolf_rusty,wolf_snowy,wolf_spotted,wolf_striped,wolf_tame,wolf_wild,wolf_woods",
            properties = "minecraft:is_armorable|bool|False;minecraft:has_increased_max_health|bool|False;minecraft:sound_variant|enum|default;minecraft:was_upgraded_to_1_21_100|bool|False",
            health = 20f
        )
        register("minecraft:xp_bottle")
        register("minecraft:zoglin", extra = "angry_zoglin,zoglin_adult,zoglin_baby", health = 40f)
        register(
            "minecraft:zombie",
            extra = "can_break_doors,can_have_equipment,convert_to_baby_drowned,convert_to_drowned,look_to_start_transformations,not_on_zombie_horse,on_zombie_horse,start_drowned_transformation,zombie_adult,zombie_baby,zombie_default,zombie_jockey,zombie_rider",
            properties = "minecraft:is_riding_zombie_horse|bool|False",
            health = 20f
        )
        register(
            "minecraft:zombie_horse",
            adult = "horse_adult,horse_can_be_leashed,horse_wild",
            extra = "horse_baby,horse_saddled,horse_tamed,horse_wild_with_rider",
            properties = "minecraft:was_upgraded_to_1_21_130|bool|False",
            health = 25f
        )
        register(
            "minecraft:zombie_nautilus",
            extra = "zombie_nautilus_ai_controlled,zombie_nautilus_leashable,zombie_nautilus_player_controlled,zombie_nautilus_tame,zombie_nautilus_tame_saddled,zombie_nautilus_tame_saddled_in_water,zombie_nautilus_tame_saddled_on_ground,zombie_nautilus_tame_unsaddled,zombie_nautilus_tameable,zombie_nautilus_wild,zombie_nautilus_wild_angry,zombie_nautilus_wild_calm,zombie_nautilus_wild_mounted,zombie_nautilus_wild_unmounted",
            properties = "minecraft:variant|enum|default",
            health = 15f
        )
        register(
            "minecraft:zombie_pigman",
            adult = "pig_zombie_adult,pig_zombie_calm",
            baby = "pig_zombie_baby,pig_zombie_calm",
            extra = "pig_zombie_angry,pig_zombie_jockey,strider_jockey",
            health = 20f
        )
        register(
            "minecraft:zombie_villager",
            extra = "adult,armorer,baby,become_zombie_villager_v2,butcher,can_break_doors,cartographer,cleric,farmer,fisherman,fletcher,from_abandoned_village,jockey,leatherworker,librarian,shepherd,to_villager,toolsmith,weaponsmith",
            health = 20f
        )
        register(
            "minecraft:zombie_villager_v2",
            extra = "adult,armorer,baby,butcher,can_break_doors,cartographer,cleric,desert_villager,farmer,fisherman,fletcher,from_abandoned_village,jockey,jungle_villager,leatherworker,librarian,mason,nitwit,savanna_villager,shepherd,snow_villager,swamp_villager,taiga_villager,to_villager,toolsmith,unskilled,villager_skin_0,villager_skin_1,villager_skin_2,villager_skin_3,villager_skin_4,villager_skin_5,weaponsmith",
            health = 20f
        )

        variant("minecraft:axolotl", "axolotl_blue", "variant", 4)
        variant("minecraft:axolotl", "axolotl_cyan", "variant", 1)
        variant("minecraft:axolotl", "axolotl_gold", "variant", 2)
        variant("minecraft:axolotl", "axolotl_lucy", "variant", 0)
        variant("minecraft:axolotl", "axolotl_wild", "variant", 3)
        variant("minecraft:cat", "cat_black", "variant", 9)
        variant("minecraft:cat", "cat_british", "variant", 4)
        variant("minecraft:cat", "cat_calico", "variant", 5)
        variant("minecraft:cat", "cat_jellie", "variant", 10)
        variant("minecraft:cat", "cat_persian", "variant", 6)
        variant("minecraft:cat", "cat_ragdoll", "variant", 7)
        variant("minecraft:cat", "cat_red", "variant", 2)
        variant("minecraft:cat", "cat_siamese", "variant", 3)
        variant("minecraft:cat", "cat_tabby", "variant", 8)
        variant("minecraft:cat", "cat_tuxedo", "variant", 1)
        variant("minecraft:cat", "cat_white", "variant", 0)
        variant("minecraft:fox", "fox_arctic", "variant", 1)
        variant("minecraft:fox", "fox_red", "variant", 0)
        variant("minecraft:frog", "cold_frog", "variant", 1)
        variant("minecraft:frog", "temperate_frog", "variant", 0)
        variant("minecraft:frog", "warm_frog", "variant", 2)
        variant("minecraft:goat", "goat_default", "variant", 0)
        variant("minecraft:goat", "goat_screamer", "variant", 1)
        variant("minecraft:horse", "base_black", "variant", 4)
        variant("minecraft:horse", "base_brown", "variant", 3)
        variant("minecraft:horse", "base_chestnut", "variant", 2)
        variant("minecraft:horse", "base_creamy", "variant", 1)
        variant("minecraft:horse", "base_darkbrown", "variant", 6)
        variant("minecraft:horse", "base_gray", "variant", 5)
        variant("minecraft:horse", "base_white", "variant", 0)
        variant("minecraft:horse", "markings_black_dots", "mark_variant", 4)
        variant("minecraft:horse", "markings_none", "mark_variant", 0)
        variant("minecraft:horse", "markings_white_details", "mark_variant", 1)
        variant("minecraft:horse", "markings_white_dots", "mark_variant", 3)
        variant("minecraft:horse", "markings_white_fields", "mark_variant", 2)
        variant("minecraft:llama", "llama_brown", "variant", 2)
        variant("minecraft:llama", "llama_creamy", "variant", 0)
        variant("minecraft:llama", "llama_gray", "variant", 3)
        variant("minecraft:llama", "llama_white", "variant", 1)
        variant("minecraft:magma_cube", "slime_large", "variant", 4)
        variant("minecraft:magma_cube", "slime_medium", "variant", 2)
        variant("minecraft:magma_cube", "slime_small", "variant", 1)
        variant("minecraft:mooshroom", "mooshroom_brown", "variant", 1)
        variant("minecraft:mooshroom", "mooshroom_red", "variant", 0)
        variant("minecraft:parrot", "parrot_blue", "variant", 1)
        variant("minecraft:parrot", "parrot_cyan", "variant", 3)
        variant("minecraft:parrot", "parrot_green", "variant", 2)
        variant("minecraft:parrot", "parrot_red", "variant", 0)
        variant("minecraft:parrot", "parrot_silver", "variant", 4)
        variant("minecraft:rabbit", "coat_black", "variant", 2)
        variant("minecraft:rabbit", "coat_brown", "variant", 0)
        variant("minecraft:rabbit", "coat_desert", "variant", 4)
        variant("minecraft:rabbit", "coat_salt", "variant", 5)
        variant("minecraft:rabbit", "coat_splotched", "variant", 3)
        variant("minecraft:rabbit", "coat_white", "variant", 1)
        variant("minecraft:sheep", "sheep_black", "color", 15)
        variant("minecraft:sheep", "sheep_blue", "color", 11)
        variant("minecraft:sheep", "sheep_brown", "color", 12)
        variant("minecraft:sheep", "sheep_cyan", "color", 9)
        variant("minecraft:sheep", "sheep_gray", "color", 7)
        variant("minecraft:sheep", "sheep_light_blue", "color", 3)
        variant("minecraft:sheep", "sheep_light_gray", "color", 8)
        variant("minecraft:sheep", "sheep_orange", "color", 1)
        variant("minecraft:sheep", "sheep_pink", "color", 6)
        variant("minecraft:sheep", "sheep_red", "color", 14)
        variant("minecraft:sheep", "sheep_white", "color", 0)
        variant("minecraft:sheep", "sheep_yellow", "color", 4)
        variant("minecraft:slime", "slime_large", "variant", 4)
        variant("minecraft:slime", "slime_medium", "variant", 2)
        variant("minecraft:slime", "slime_small", "variant", 1)
        variant("minecraft:trader_llama", "llama_brown", "variant", 2)
        variant("minecraft:trader_llama", "llama_creamy", "variant", 0)
        variant("minecraft:trader_llama", "llama_gray", "variant", 3)
        variant("minecraft:trader_llama", "llama_white", "variant", 1)
        variant("minecraft:wolf", "wolf_ashen", "variant", 1)
        variant("minecraft:wolf", "wolf_black", "variant", 2)
        variant("minecraft:wolf", "wolf_chestnut", "variant", 3)
        variant("minecraft:wolf", "wolf_pale", "variant", 0)
        variant("minecraft:wolf", "wolf_rusty", "variant", 4)
        variant("minecraft:wolf", "wolf_snowy", "variant", 5)
        variant("minecraft:wolf", "wolf_spotted", "variant", 6)
        variant("minecraft:wolf", "wolf_striped", "variant", 7)
        variant("minecraft:wolf", "wolf_woods", "variant", 8)
    }
}
