package com.noches.chunkoidng.core.nbt

import androidx.annotation.StringRes
import com.noches.chunkoidng.R
import com.noches.chunkoidng.core.leveldb.LevelDbCategory
import com.noches.chunkoidng.core.leveldb.LevelDbRecord

data class SemanticTag(
    @StringRes val titleRes: Int,
    @StringRes val guideRes: Int? = null,
    val colorRgb: Long = 0xFF4F46E5
)

object MinecraftSemanticDescriptor {

    private val NBT_TAG_MAP = mapOf(
        "LevelName" to SemanticTag(R.string.nbt_tag_level_name, colorRgb = 0xFF8B5CF6),
        "RandomSeed" to SemanticTag(R.string.nbt_tag_seed, colorRgb = 0xFF8B5CF6),
        "GameType" to SemanticTag(R.string.nbt_tag_gamemode, R.string.nbt_guide_gamemode, colorRgb = 0xFFF59E0B),
        "playerGameType" to SemanticTag(R.string.nbt_tag_gamemode, R.string.nbt_guide_gamemode, colorRgb = 0xFFF59E0B),
        "Difficulty" to SemanticTag(R.string.nbt_tag_difficulty, R.string.nbt_guide_difficulty, colorRgb = 0xFFEF4444),
        "DifficultyLocked" to SemanticTag(R.string.nbt_tag_difficulty_locked, R.string.nbt_guide_difficulty_locked, colorRgb = 0xFFEF4444),
        "hardcore" to SemanticTag(R.string.nbt_tag_hardcore, R.string.nbt_guide_hardcore, colorRgb = 0xFFDC2626),
        "allowCommands" to SemanticTag(R.string.nbt_tag_cheats, R.string.nbt_guide_cheats, colorRgb = 0xFF6366F1),
        "commandsEnabled" to SemanticTag(R.string.nbt_tag_cheats, R.string.nbt_guide_cheats, colorRgb = 0xFF6366F1),
        "cheatsEnabled" to SemanticTag(R.string.nbt_tag_cheats, R.string.nbt_guide_cheats, colorRgb = 0xFF6366F1),
        "hasBeenLoadedInCreative" to SemanticTag(R.string.nbt_tag_creative_loaded, R.string.nbt_guide_creative_loaded, colorRgb = 0xFFEA580C),
        "Time" to SemanticTag(R.string.nbt_tag_time, colorRgb = 0xFF0EA5E9),
        "DayTime" to SemanticTag(R.string.nbt_tag_day_time, R.string.nbt_guide_day_time, colorRgb = 0xFF0EA5E9),
        "SpawnX" to SemanticTag(R.string.nbt_tag_spawn_coord, colorRgb = 0xFF10B981),
        "SpawnY" to SemanticTag(R.string.nbt_tag_spawn_coord, colorRgb = 0xFF10B981),
        "SpawnZ" to SemanticTag(R.string.nbt_tag_spawn_coord, colorRgb = 0xFF10B981),
        "raining" to SemanticTag(R.string.nbt_tag_weather_rain, R.string.nbt_guide_bool_0_1, colorRgb = 0xFF0284C7),
        "rainTime" to SemanticTag(R.string.nbt_tag_weather_duration, colorRgb = 0xFF0284C7),
        "thundering" to SemanticTag(R.string.nbt_tag_weather_thunder, R.string.nbt_guide_bool_0_1, colorRgb = 0xFF0284C7),
        "thunderTime" to SemanticTag(R.string.nbt_tag_weather_duration, colorRgb = 0xFF0284C7),
        "clearWeatherTime" to SemanticTag(R.string.nbt_tag_weather_duration, colorRgb = 0xFF0284C7),
        "BorderSize" to SemanticTag(R.string.nbt_tag_border, colorRgb = 0xFF64748B),
        "BorderCenterX" to SemanticTag(R.string.nbt_tag_border, colorRgb = 0xFF64748B),
        "BorderCenterZ" to SemanticTag(R.string.nbt_tag_border, colorRgb = 0xFF64748B),
        "DataVersion" to SemanticTag(R.string.nbt_tag_data_version, colorRgb = 0xFF6B7280),
        "StorageVersion" to SemanticTag(R.string.nbt_tag_data_version, colorRgb = 0xFF6B7280),
        "NetworkVersion" to SemanticTag(R.string.nbt_tag_data_version, colorRgb = 0xFF6B7280),
        "Platform" to SemanticTag(R.string.nbt_tag_platform, R.string.nbt_guide_platform, colorRgb = 0xFF4F46E5),
        "DragonFight" to SemanticTag(R.string.nbt_tag_dragon_fight, colorRgb = 0xFF7C3AED),
        "WanderingTraderSpawnDelay" to SemanticTag(R.string.nbt_tag_wandering_trader, colorRgb = 0xFFD97706),
        "WanderingTraderSpawnChance" to SemanticTag(R.string.nbt_tag_wandering_trader, colorRgb = 0xFFD97706),

        "Player" to SemanticTag(R.string.nbt_tag_player_data, colorRgb = 0xFF10B981),
        "Pos" to SemanticTag(R.string.nbt_tag_position, colorRgb = 0xFF10B981),
        "Motion" to SemanticTag(R.string.nbt_tag_motion, colorRgb = 0xFF10B981),
        "Rotation" to SemanticTag(R.string.nbt_tag_rotation, colorRgb = 0xFF10B981),
        "Health" to SemanticTag(R.string.nbt_tag_health, R.string.nbt_guide_health, colorRgb = 0xFFF43F5E),
        "foodLevel" to SemanticTag(R.string.nbt_tag_hunger, R.string.nbt_guide_hunger, colorRgb = 0xFFF97316),
        "foodSaturationLevel" to SemanticTag(R.string.nbt_tag_saturation, colorRgb = 0xFFF97316),
        "XpLevel" to SemanticTag(R.string.nbt_tag_xp_level, colorRgb = 0xFF84CC16),
        "XpP" to SemanticTag(R.string.nbt_tag_xp_progress, colorRgb = 0xFF84CC16),
        "XpTotal" to SemanticTag(R.string.nbt_tag_xp_total, colorRgb = 0xFF84CC16),
        "Score" to SemanticTag(R.string.nbt_tag_score, colorRgb = 0xFFEAB308),
        "Dimension" to SemanticTag(R.string.nbt_tag_dimension, R.string.nbt_guide_dimension, colorRgb = 0xFF8B5CF6),
        "Inventory" to SemanticTag(R.string.nbt_tag_inventory, colorRgb = 0xFFD97706),
        "EnderItems" to SemanticTag(R.string.nbt_tag_ender_chest, colorRgb = 0xFF0D9488),
        "EnderChestInventory" to SemanticTag(R.string.nbt_tag_ender_chest, colorRgb = 0xFF0D9488),
        "abilities" to SemanticTag(R.string.nbt_tag_abilities, colorRgb = 0xFF7C3AED),
        "mayfly" to SemanticTag(R.string.nbt_tag_fly, R.string.nbt_guide_fly, colorRgb = 0xFF7C3AED),
        "flying" to SemanticTag(R.string.nbt_tag_fly, R.string.nbt_guide_fly, colorRgb = 0xFF7C3AED),
        "invulnerable" to SemanticTag(R.string.nbt_tag_invulnerable, R.string.nbt_guide_invulnerable, colorRgb = 0xFFE11D48),
        "Invulnerable" to SemanticTag(R.string.nbt_tag_invulnerable, R.string.nbt_guide_invulnerable, colorRgb = 0xFFE11D48),
        "SelectedItemSlot" to SemanticTag(R.string.nbt_tag_selected_slot, R.string.nbt_guide_slot_0_8, colorRgb = 0xFF6366F1),
        "FallDistance" to SemanticTag(R.string.nbt_tag_fall_distance, colorRgb = 0xFF64748B),
        "Fire" to SemanticTag(R.string.nbt_tag_fire_ticks, colorRgb = 0xFFEF4444),
        "Air" to SemanticTag(R.string.nbt_tag_air_ticks, R.string.nbt_guide_air, colorRgb = 0xFF06B6D4),
        "ActiveEffects" to SemanticTag(R.string.nbt_tag_active_effects, colorRgb = 0xFFEC4899),
        "Attributes" to SemanticTag(R.string.nbt_tag_attributes, colorRgb = 0xFF8B5CF6),
        "UUID" to SemanticTag(R.string.nbt_tag_uuid, colorRgb = 0xFF64748B),
        "CustomName" to SemanticTag(R.string.nbt_tag_custom_name, colorRgb = 0xFF10B981),
        "SpawnForced" to SemanticTag(R.string.nbt_tag_player_respawn, colorRgb = 0xFF10B981),

        "InhabitedTime" to SemanticTag(R.string.nbt_tag_inhabited_time, R.string.nbt_guide_inhabited_time, colorRgb = 0xFF06B6D4),
        "Status" to SemanticTag(R.string.nbt_tag_chunk_status, colorRgb = 0xFF3B82F6),
        "sections" to SemanticTag(R.string.nbt_tag_subchunks, colorRgb = 0xFF3B82F6),
        "Sections" to SemanticTag(R.string.nbt_tag_subchunks, colorRgb = 0xFF3B82F6),
        "block_states" to SemanticTag(R.string.nbt_tag_block_states, colorRgb = 0xFF3B82F6),
        "BlockStates" to SemanticTag(R.string.nbt_tag_block_states, colorRgb = 0xFF3B82F6),
        "block_entities" to SemanticTag(R.string.nbt_tag_block_entities, colorRgb = 0xFFF59E0B),
        "TileEntities" to SemanticTag(R.string.nbt_tag_block_entities, colorRgb = 0xFFF59E0B),
        "entities" to SemanticTag(R.string.nbt_tag_entities, colorRgb = 0xFFEC4899),
        "Entities" to SemanticTag(R.string.nbt_tag_entities, colorRgb = 0xFFEC4899),
        "Heightmaps" to SemanticTag(R.string.nbt_tag_heightmaps, colorRgb = 0xFF64748B),
        "structures" to SemanticTag(R.string.nbt_tag_structures, colorRgb = 0xFFD946EF),
        "starts" to SemanticTag(R.string.nbt_tag_structures, colorRgb = 0xFFD946EF),

        "id" to SemanticTag(R.string.nbt_tag_item_id, colorRgb = 0xFF6366F1),
        "Count" to SemanticTag(R.string.nbt_tag_item_count, R.string.nbt_guide_item_count, colorRgb = 0xFF10B981),
        "Damage" to SemanticTag(R.string.nbt_tag_item_damage, R.string.nbt_guide_item_damage, colorRgb = 0xFFEF4444),
        "Slot" to SemanticTag(R.string.nbt_tag_item_slot, colorRgb = 0xFF6366F1),
        "Enchantments" to SemanticTag(R.string.nbt_tag_item_enchantments, colorRgb = 0xFF8B5CF6),
        "ench" to SemanticTag(R.string.nbt_tag_item_enchantments, colorRgb = 0xFF8B5CF6),
        "lvl" to SemanticTag(R.string.nbt_tag_enchant_level, R.string.nbt_guide_enchant_level, colorRgb = 0xFF8B5CF6),
        "Unbreakable" to SemanticTag(R.string.nbt_tag_unbreakable, R.string.nbt_guide_unbreakable, colorRgb = 0xFF059669),
        "RepairCost" to SemanticTag(R.string.nbt_tag_repair_cost, R.string.nbt_guide_repair_cost, colorRgb = 0xFFD97706)
    )

    fun describeNbt(key: String, path: String? = null): SemanticTag? {
        val exact = NBT_TAG_MAP[key]
        if (exact != null) return exact
        if (path != null) {
            val lastPart = path.substringAfterLast('/')
            if (lastPart != key) {
                val pathMatch = NBT_TAG_MAP[lastPart]
                if (pathMatch != null) return pathMatch
            }
        }
        return null
    }

    fun describeLevelDbRecord(record: LevelDbRecord): SemanticTag? {
        val keyStr = record.keyString
        when {
            keyStr == "~local_player" -> return SemanticTag(R.string.leveldb_record_local_player, colorRgb = 0xFF10B981)
            keyStr.startsWith("player_server_") -> return SemanticTag(R.string.leveldb_record_server_player, colorRgb = 0xFF059669)
            keyStr.startsWith("player_") -> return SemanticTag(R.string.leveldb_record_server_player, colorRgb = 0xFF059669)
            keyStr == "portals" -> return SemanticTag(R.string.leveldb_record_portals, colorRgb = 0xFF9333EA)
            keyStr == "scoreboard" -> return SemanticTag(R.string.leveldb_record_scoreboard, colorRgb = 0xFF2563EB)
            keyStr == "autonomousentities" -> return SemanticTag(R.string.leveldb_record_autonomous_entities, colorRgb = 0xFF475569)
            keyStr.startsWith("village_") -> return SemanticTag(R.string.leveldb_record_village, colorRgb = 0xFFD97706)
            keyStr.startsWith("tickingarea_") -> return SemanticTag(R.string.leveldb_record_ticking_area, colorRgb = 0xFF0D9488)
            keyStr.startsWith("map_") -> return SemanticTag(R.string.leveldb_record_map, colorRgb = 0xFFEAB308)
            keyStr == "schedulerWT" -> return SemanticTag(R.string.leveldb_record_scheduler, colorRgb = 0xFF64748B)
            keyStr.startsWith("digp") -> return SemanticTag(R.string.leveldb_record_digp, colorRgb = 0xFF0284C7)
            record.category == LevelDbCategory.PLAYER -> return SemanticTag(R.string.leveldb_record_server_player, colorRgb = 0xFF059669)
            record.category == LevelDbCategory.BLOCK_ENTITY -> return SemanticTag(R.string.nbt_tag_block_entities, colorRgb = 0xFFF59E0B)
            record.category == LevelDbCategory.ENTITY -> return SemanticTag(R.string.nbt_tag_entities, colorRgb = 0xFFEC4899)
            record.category == LevelDbCategory.CHUNK -> return SemanticTag(R.string.nbt_tag_subchunks, colorRgb = 0xFF3B82F6)
            else -> return null
        }
    }
}
