package com.noches.chunkoidng.core.midi

enum class NoteBlockInstrument(
    val id: Int,
    val displayName: String,
    val soundId: String,
    val bedrockSoundId: String,
    val baseBlockId: String
) {
    HARP(0, "Harp / Piano", "block.note_block.harp", "note.harp", "minecraft:dirt"),
    BASS(1, "Bass / Double Bass", "block.note_block.bass", "note.bass", "minecraft:oak_planks"),
    BASEDRUM(2, "Bass Drum / Kick", "block.note_block.basedrum", "note.bd", "minecraft:stone"),
    SNARE(3, "Snare Drum", "block.note_block.snare", "note.snare", "minecraft:sand"),
    HAT(4, "Click / Stick", "block.note_block.hat", "note.hat", "minecraft:glass"),
    GUITAR(5, "Guitar", "block.note_block.guitar", "note.guitar", "minecraft:white_wool"),
    FLUTE(6, "Flute", "block.note_block.flute", "note.flute", "minecraft:clay"),
    BELL(7, "Bell / Glockenspiel", "block.note_block.bell", "note.bell", "minecraft:gold_block"),
    CHIME(8, "Chime", "block.note_block.chime", "note.chime", "minecraft:packed_ice"),
    XYLOPHONE(9, "Xylophone", "block.note_block.xylophone", "note.xylophone", "minecraft:bone_block"),
    IRON_XYLOPHONE(10, "Vibraphone", "block.note_block.iron_xylophone", "note.iron_xylophone", "minecraft:iron_block"),
    COW_BELL(11, "Cowbell", "block.note_block.cow_bell", "note.cow_bell", "minecraft:soul_sand"),
    DIDGERIDOO(12, "Didgeridoo", "block.note_block.didgeridoo", "note.didgeridoo", "minecraft:carved_pumpkin"),
    BIT(13, "8-bit / Square Wave", "block.note_block.bit", "note.bit", "minecraft:emerald_block"),
    BANJO(14, "Banjo", "block.note_block.banjo", "note.banjo", "minecraft:hay_block"),
    PLING(15, "Pling / Electric Piano", "block.note_block.pling", "note.pling", "minecraft:glowstone");

    companion object {
        fun fromId(id: Int): NoteBlockInstrument {
            return entries.find { it.id == id } ?: HARP
        }

        fun mapGeneralMidiProgram(program: Int): NoteBlockInstrument {
            return when (program) {
                in 0..7 -> HARP
                in 8..10 -> BELL
                11 -> IRON_XYLOPHONE
                12, 13 -> XYLOPHONE
                14, 15 -> BELL
                in 16..23 -> PLING
                in 24..31 -> GUITAR
                in 32..39 -> BASS
                in 40..47 -> FLUTE
                in 56..71 -> DIDGERIDOO
                in 72..79 -> FLUTE
                in 80..87 -> BIT
                in 104..111 -> BANJO
                else -> HARP
            }
        }

        fun mapPercussionNote(percussionPitch: Int): NoteBlockInstrument {
            return when (percussionPitch) {
                35, 36 -> BASEDRUM
                38, 40 -> SNARE
                42, 44, 46 -> HAT
                49, 51, 52, 53, 55, 57 -> HAT
                56 -> COW_BELL
                41, 43, 45, 47, 48, 50 -> BASEDRUM
                76, 77 -> BASS
                else -> SNARE
            }
        }
    }
}

data class NoteBlockNote(
    val tick: Int,
    val instrument: NoteBlockInstrument,
    val key: Int,
    val velocity: Int = 100
) {
    fun calculatePlaysoundPitch(): Double {
        return Math.pow(2.0, (key - 12).toDouble() / 12.0)
    }
}

data class NoteBlockSong(
    val title: String,
    val author: String = "ChunkoidNG",
    val description: String = "",
    val speedTicksPerSec: Double = 10.0,
    val notes: List<NoteBlockNote>,
    val lengthTicks: Int
)
